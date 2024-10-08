package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.FhirPathExecutionException
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ConditionStamper
import gov.cdc.prime.router.azure.LookupTableConditionMapper
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.cli.helpers.HL7DiffHelper
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.config.validation.OrganizationValidation
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter.ReceiverFilterEvaluationResult
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.fhirpath.FHIRLexer.FHIRLexerException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Reference
import java.io.File
import java.util.UUID

/**
 * Process data into/from FHIR.
 */
class ProcessFhirCommands : CliktCommand(
    name = "fhirdata",
    help = "Process data into/from FHIR"
) {

    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Optional output file.  if no output file is specified then the output is printed to the screen.
     */
    private val outputFile by option("-o", "--output-file", help = "output file")
        .file()

    /**
     * The format to output the data.
     */
    private val outputFormat by option("--output-format", help = "output format")
        .choice(MimeFormat.HL7.toString(), MimeFormat.FHIR.toString())

    /**
     * String of file names
     */
    private val enrichmentSchemaNames by option(
        "--enrichment-schemas",
        help = "comma separated enrichment schema name(s) from current directory"
    )

    private val diffHl7Output by option(
        "--diff-hl7-output",
        help = "when true, diff the the input HL7 with the output, can only be used going HL7 -> FHIR -> HL7"
    )

    /**
     * Receiver schema location for the FHIR to HL7 conversion
     */
    private val receiverSchema by option(
        "-r", "--receiver-schema", help = "Receiver schema location. Required for HL7 output."
    )

    /**
     * Name of the receiver settings to use
     */
    private val receiverNameParam by option(
        "--receiver-name", help = "Name of the receiver settings to use"
    )

    /**
     * Name of the org settings to use
     */
    private val orgNameParam by option(
        "--org", help = "Name of the org settings to use"
    )

    /**
     * Environment that specifies where to get the receiver settings
     */
    private val environmentParam by option(
        "--receiver-setting-env", help = "Environment that specifies where to get the receiver settings"
    )

    /**
     * Sender schema location
     */
    private val senderSchemaParam by option("-s", "--sender-schema", help = "Sender schema location")

    private val inputSchema by option(
        "--input-schema", help = "Mapping schema for input file"
    ).default("./metadata/HL7/catchall")

    private val hl7DiffHelper = HL7DiffHelper()

    override fun run() {
        val messageOrBundle =
            processFhirDataRequest(
                inputFile,
                environmentParam,
                receiverNameParam,
                orgNameParam,
                senderSchemaParam,
                true
            )
        if (messageOrBundle.message != null) {
            outputResult(messageOrBundle.message!!)
        } else if (messageOrBundle.bundle != null) {
            outputResult(fhirResult = messageOrBundle.bundle!!, ActionLogger())
        } else {
            throw CliktError("No result returned.")
        }
    }

    fun processFhirDataRequest(
        inputFile: File,
        environment: String?,
        receiverName: String?,
        orgName: String?,
        senderSchema: String?,
        isCli: Boolean,
    ): MessageOrBundle {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        // Check on the extension of the file for supported operations
        val inputFileType = inputFile.extension.uppercase()
        val receiver = if (!isCli) {
            getReceiver(environment, receiverName, orgName, GetMultipleSettings(), isCli)
        } else {
            null
        }

        // If there is a receiver, check the filters
        var bundle = FhirTranscoder.decode(contents)
        if (receiver != null) {
            val reportStreamFilters = mutableListOf<Pair<String, ReportStreamFilter>>()
            reportStreamFilters.add(Pair("Jurisdictional Filter", receiver.jurisdictionalFilter))
            reportStreamFilters.add(Pair("Quality Filter", receiver.qualityFilter))
            reportStreamFilters.add(Pair("Routing Filter", receiver.routingFilter))
            reportStreamFilters.add(Pair("Processing Mode Filter", receiver.processingModeFilter))

            val validationErrors = mutableListOf<String>()
            reportStreamFilters.forEach { reportStreamFilter ->
                reportStreamFilter.second.forEach { filter ->
                    val validation = OrganizationValidation.validateFilter(filter)
                    if (!validation) {
                        validationErrors.add(
                            "Filter of type ${reportStreamFilter.first} is not valid. " +
                            "Value: '$filter'"
                        )
                    } else {
                        val result = FhirPathUtils.evaluate(
                            CustomContext(
                                bundle,
                                bundle,
                                mutableMapOf(),
                                CustomFhirPathFunctions()
                            ),
                            bundle,
                            bundle,
                            filter
                        )
                        if (result.isEmpty() ||
                            (result[0].isBooleanPrimitive && result[0].primitiveValue() == "false")
                        ) {
                            return MessageOrBundle(
                                filterErrors =
                                mutableListOf("Filter '$filter' filtered out everything, nothing to return."),
                                filtersPassed = false
                            )
                        }
                    }
                }
            }

            if (validationErrors.isNotEmpty()) {
                return MessageOrBundle(
                    filterErrors = mutableListOf(validationErrors.joinToString("\n")),
                    filtersPassed = false
                )
            }

            receiver.conditionFilter.forEach { conditionFilter ->
                val validation = OrganizationValidation.validateFilter(conditionFilter)
                if (!validation) {
                    return MessageOrBundle(
                        filterErrors =
                        mutableListOf("Condition filter '$conditionFilter' is not valid."),
                            filtersPassed = false
                    )
                }
            }
        }

        var messageOrBundle = MessageOrBundle()
        when {
            // HL7 to FHIR conversion
            inputFileType == "HL7" && (
                (isCli && outputFormat == MimeFormat.FHIR.toString()) ||
                    (receiver != null && receiver.format == MimeFormat.FHIR)
                ) -> {
                val fhirMessage = convertHl7ToFhir(contents, receiver).first
                val enrichmentSchemaInfo = applyEnrichmentSchemas(fhirMessage, isCli)
                setEnrichmentSchemaFields(messageOrBundle, enrichmentSchemaInfo)

                if (receiver != null && receiver.enrichmentSchemaNames.isNotEmpty()) {
                    receiver.enrichmentSchemaNames.forEach { currentSchema ->
                        val transfromer = FhirTransformer(currentSchema)
                        val returnedBundle =
                            transfromer.process(messageOrBundle.bundle!!)
                        setEnrichmentSchemaFields(
                            messageOrBundle,
                            transfromer.warnings,
                            transfromer.errors,
                            returnedBundle
                        )
                    }
                }
                return handleSenderAndReceiverTransforms(messageOrBundle, senderSchema, isCli)
            }

            // FHIR to HL7 conversion
            (inputFileType == "FHIR" || inputFileType == "JSON") && (
                (isCli && outputFormat == MimeFormat.HL7.toString()) ||
                    (receiver != null && (receiver.format == MimeFormat.HL7 || receiver.format == MimeFormat.HL7_BATCH))
                ) -> {
                if (receiver == null) {
                    return convertFhirToHl7(
                        jsonString = contents,
                        senderSchema = senderSchema,
                        isCli = isCli
                    )
                }

                bundle = FhirTranscoder.decode(contents)
                messageOrBundle.bundle = bundle
                if (receiver.enrichmentSchemaNames.isNotEmpty()) {
                    receiver.enrichmentSchemaNames.forEach { currentSchema ->
                        val transformer = FhirTransformer(currentSchema)
                        val returnedBundle =
                            transformer.process(bundle)
                        setEnrichmentSchemaFields(
                            messageOrBundle,
                            transformer.warnings,
                            transformer.errors,
                            returnedBundle
                        )
                    }
                }

                messageOrBundle = convertFhirToHl7(
                        FhirTranscoder.encode(messageOrBundle.bundle!!),
                        receiver.translation as Hl7Configuration,
                        receiver,
                        senderSchema,
                        isCli
                    )
            }

            // FHIR to FHIR conversion
            (inputFileType == "FHIR" || inputFileType == "JSON") && (
                (isCli && outputFormat == MimeFormat.FHIR.toString()) ||
                    (receiver != null && receiver.format == MimeFormat.FHIR)
                ) -> {
                return convertFhirToFhir(FhirTranscoder.encode(bundle), receiver, senderSchema, isCli)
            }

            // HL7 to FHIR to HL7 conversion
            inputFileType == "HL7" && (
                (isCli && outputFormat == MimeFormat.HL7.toString()) ||
                    (
                        receiver != null &&
                        (receiver.format == MimeFormat.HL7 || receiver.format == MimeFormat.HL7_BATCH)
                    )
                ) -> {
                val (bundle2, inputMessage) = convertHl7ToFhir(contents, receiver)
                val output = convertFhirToHl7(
                    jsonString = FhirTranscoder.encode(bundle2),
                    senderSchema = senderSchema,
                    isCli = isCli
                )
                if (diffHl7Output != null && isCli) {
                    val differences = hl7DiffHelper.diffHl7(output.message!!, inputMessage)
                    echo("-------diff output")
                    echo("There were ${differences.size} differences between the input and output")
                    differences.forEach { echo(it.toString()) }
                }
                return output
            }

            else -> throw CliktError("File extension ${inputFile.extension} is not supported.")
        }
        return messageOrBundle
    }

    private fun setEnrichmentSchemaFields(
        messageOrBundle: MessageOrBundle,
        enrichmentSchemaFields: FhirTransformer.BundleWithMessages,
    ): MessageOrBundle {
        messageOrBundle.enrichmentSchemaWarnings.addAll(enrichmentSchemaFields.warnings)
        messageOrBundle.enrichmentSchemaErrors.addAll(enrichmentSchemaFields.errors)
        messageOrBundle.enrichmentSchemaPassed = enrichmentSchemaFields.errors.isEmpty()
        messageOrBundle.bundle = enrichmentSchemaFields.bundle
        return messageOrBundle
    }

    private fun setEnrichmentSchemaFields(
        messageOrBundle: MessageOrBundle,
        warnings: MutableList<String>,
        errors: MutableList<String>,
        bundle: Bundle,
    ): MessageOrBundle {
        messageOrBundle.enrichmentSchemaWarnings.addAll(warnings)
        messageOrBundle.enrichmentSchemaErrors.addAll(errors)
        messageOrBundle.enrichmentSchemaPassed = errors.isEmpty()
        messageOrBundle.bundle = bundle
        return messageOrBundle
    }

    class MessageOrBundle(
        var message: Message? = null,
        var bundle: Bundle? = null,
        var senderTransformPassed: Boolean = true,
        var senderTransformErrors: MutableList<String> = mutableListOf(),
        var senderTransformWarnings: MutableList<String> = mutableListOf(),
        var enrichmentSchemaPassed: Boolean = true,
        var enrichmentSchemaErrors: MutableList<String> = mutableListOf(),
        var enrichmentSchemaWarnings: MutableList<String> = mutableListOf(),
        var receiverTransformPassed: Boolean = true,
        var receiverTransformErrors: MutableList<String> = mutableListOf(),
        var receiverTransformWarnings: MutableList<String> = mutableListOf(),
        var filterErrors: MutableList<String> = mutableListOf(),
        var filtersPassed: Boolean = true,
    )

    private fun applyConditionFilter(receiver: Receiver, bundle: Bundle): Bundle {
        val trackingId = if (bundle.id != null) {
            bundle.id
        } else {
            // this is just for logging so it is fine to just make it up
            UUID.randomUUID().toString()
        }
        val result = FHIRReceiverFilter().evaluateObservationConditionFilters(
            receiver,
            bundle,
            ActionLogger(),
            trackingId
        )
        if (result is ReceiverFilterEvaluationResult.Success) {
            return result.bundle
        } else {
            throw CliktError("Condition filter failed.")
        }
    }

    fun getReceiver(
        environment: String?,
        receiverName: String?,
        orgName: String?,
        getMultipleSettings: GetMultipleSettings = GetMultipleSettings(),
        isCli: Boolean,
    ): Receiver? {
        if (!environment.isNullOrBlank() && !receiverName.isNullOrBlank() && !orgName.isNullOrBlank()) {
            if (isCli && !outputFormat.isNullOrBlank()) {
                throw CliktError(
                    "Please specify either a receiver OR an output format. Not both."
                )
            }
            val foundEnvironment = Environment.get(environment)
            val accessToken = OktaCommand.fetchAccessToken(foundEnvironment.oktaApp)
                ?: abort(
                    "Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access " +
                        "token for the $foundEnvironment environment."
                )
            val organizations = getMultipleSettings.getAll(
                environment = foundEnvironment,
                accessToken = accessToken,
                specificOrg = orgName,
                exactMatch = true
            )

            if (organizations.isEmpty()) {
                return null
            }
            val receivers = organizations[0].receivers.filter { receiver -> receiver.name == receiverName }
            if (receivers.isNotEmpty()) {
                return receivers[0]
            }
        } else if (isCli && outputFormat.isNullOrBlank()) {
            throw CliktError(
                "Output format is required if the environment, receiver, and org " +
                    "are not specified. "
            )
        }
        return null
    }

    private val defaultHL7Configuration = Hl7Configuration(
        receivingApplicationOID = null,
        receivingFacilityOID = null,
        messageProfileId = null,
        receivingApplicationName = null,
        receivingFacilityName = null,
        receivingOrganization = null,
    )

    /**
     * Convert a FHIR bundle as a [jsonString] to an HL7 message.
     * @return an HL7 message
     */
    private fun convertFhirToHl7(
        jsonString: String,
        hl7Configuration: Hl7Configuration = defaultHL7Configuration,
        receiver: Receiver? = null,
        senderSchema: String?,
        isCli: Boolean,
    ): MessageOrBundle {
        val fhirMessage = FhirTranscoder.decode(jsonString)
        val enrichmentSchemaMessages = applyEnrichmentSchemas(fhirMessage, isCli)
        val errors: MutableList<String> = mutableListOf()
        val warnings: MutableList<String> = mutableListOf()
        return when {
            (isCli && receiverSchema == null) && (receiver == null || (isCli && receiver.schemaName.isBlank())) ->
                // Receiver schema required because if it's coming out as HL7, it would be getting any transform info
                // for that from a receiver schema.
                throw CliktError("You must specify a receiver schema using --receiver-schema.")

            isCli && receiverSchema != null -> {
                val senderTransformMessages = applySenderTransforms(enrichmentSchemaMessages.bundle, senderSchema)
                val stamper = ConditionStamper(LookupTableConditionMapper(Metadata.getInstance()))
                senderTransformMessages.bundle.getObservations().forEach { observation ->
                    stamper.stampObservation(observation)
                }
                if (receiver != null) {
                    senderTransformMessages.bundle = applyConditionFilter(receiver, senderTransformMessages.bundle)
                }

                val message = FhirToHl7Converter(
                    receiverSchema!!,
                    BlobAccess.BlobContainerMetadata.build("metadata", Environment.get().storageEnvVar),
                    context = FhirToHl7Context(
                        CustomFhirPathFunctions(),
                        config = HL7TranslationConfig(
                            hl7Configuration,
                            receiver
                        ),
                        translationFunctions = CustomTranslationFunctions(),
                    ),
                    warnings = warnings,
                    errors = errors
                ).process(senderTransformMessages.bundle)
                val messageOrBundle = MessageOrBundle()
                messageOrBundle.senderTransformPassed = senderTransformMessages.errors.isEmpty()
                messageOrBundle.senderTransformWarnings = senderTransformMessages.warnings
                messageOrBundle.senderTransformErrors = senderTransformMessages.errors
                messageOrBundle.receiverTransformPassed = errors.isEmpty()
                messageOrBundle.receiverTransformErrors = errors
                messageOrBundle.receiverTransformWarnings = warnings
                messageOrBundle.message = message
                messageOrBundle
            }
            receiver != null && receiver.schemaName.isNotBlank() -> {
                val senderTransformMessages = applySenderTransforms(fhirMessage, senderSchema)
                val bundle = applyConditionFilter(receiver, senderTransformMessages.bundle)
                val message = FhirToHl7Converter(
                    receiver.schemaName,
                    BlobAccess.BlobContainerMetadata.build("metadata", Environment.get().storageEnvVar),
                    context = FhirToHl7Context(
                        CustomFhirPathFunctions(),
                        config = HL7TranslationConfig(
                            hl7Configuration,
                            receiver
                        ),
                        translationFunctions = CustomTranslationFunctions(),
                    ),
                    warnings = warnings,
                    errors = errors
                ).process(bundle)
                val messageOrBundle = MessageOrBundle()
                messageOrBundle.senderTransformPassed = senderTransformMessages.errors.isEmpty()
                messageOrBundle.senderTransformWarnings = senderTransformMessages.warnings
                messageOrBundle.senderTransformErrors = senderTransformMessages.errors
                messageOrBundle.receiverTransformPassed = errors.isEmpty()
                messageOrBundle.receiverTransformErrors = errors
                messageOrBundle.receiverTransformWarnings = warnings
                messageOrBundle.message = message
                messageOrBundle
            }
            else -> {
                if (isCli) {
                    throw CliktError("Error state reached when trying to apply the transforms.")
                } else {
                    MessageOrBundle(
                        senderTransformErrors =
                            mutableListOf("Error state reached when trying to apply the transforms."),
                        receiverTransformErrors = mutableListOf(
                            "Error state reached when trying to apply the transforms."
                        )
                    )
                }
            }
        }
    }

    /**
     * convert an FHIR message to FHIR message
     */
    private fun convertFhirToFhir(
        jsonString: String,
        receiver: Receiver?,
        senderSchema: String?,
        isCli: Boolean,
    ): MessageOrBundle {
        var fhirMessage = FhirTranscoder.decode(jsonString)
        val stamper = ConditionStamper(LookupTableConditionMapper(Metadata.getInstance()))
        fhirMessage.getObservations().forEach { observation ->
            stamper.stampObservation(observation)
        }

        val messageOrBundle = MessageOrBundle()
        if (receiver != null) {
            fhirMessage = applyConditionFilter(receiver, fhirMessage)
            if (receiver.enrichmentSchemaNames.isNotEmpty()) {
                receiver.enrichmentSchemaNames.forEach { currentSchema ->
                    val transformer = FhirTransformer(currentSchema)
                    val bundle = transformer.process(fhirMessage)
                    setEnrichmentSchemaFields(
                        messageOrBundle,
                        transformer.warnings,
                        transformer.errors,
                        bundle
                    )
                }
            }
        }
        setEnrichmentSchemaFields(messageOrBundle, applyEnrichmentSchemas(fhirMessage, isCli))
        if ((
            (isCli && receiverSchema == null) ||
                (!isCli && (receiver == null || receiver.schemaName.isBlank()))
        ) && senderSchema == null
        ) {
            // Must have at least one schema or else why are you doing this
            throw CliktError("You must specify a schema.")
        } else {
            handleSenderAndReceiverTransforms(
                messageOrBundle = messageOrBundle,
                senderSchema = senderSchema,
                isCli = isCli
            )
        }

        return messageOrBundle
    }

    /**
     * Convert an HL7 message or batch as a [hl7String] to a FHIR bundle. [actionLogger] will contain any
     * warnings or errors from the reading of the HL7 data to HL7 objects.  Note that the --hl7-msg-index
     * is required for HL7 batch messages as this function only returns one FHIR bundle.
     * Note: This does not require a schema in case it is being used to see what our internal format message
     * look like.
     * @return a FHIR bundle and the parsed HL7 input that represents the data in the one HL7 message
     */
    private fun convertHl7ToFhir(hl7String: String, receiver: Receiver?): Pair<Bundle, Message> {
        val hasFiveEncodingChars = hl7MessageHasFiveEncodingChars(hl7String)
        // Some HL7 2.5.1 implementations have adopted the truncation character # that was added in 2.7
        // However, the library used to encode the HL7 message throws an error it there are more than 4 encoding
        // characters, so this work around exists for that scenario
        val stringToEncode = hl7String.replace("MSH|^~\\&#|", "MSH|^~\\&|")
        val hl7message = HL7Reader.parseHL7Message(
            stringToEncode,
            null
        )
        // if a hl7 parsing failure happens, throw error and show the message
        if (hl7message.toString().lowercase().contains("failed")) {
            throw CliktError("HL7 parser failure. $hl7message")
        }
        if (hasFiveEncodingChars) {
            val msh = hl7message.get("MSH") as Segment
            Terser.set(msh, 2, 0, 1, 1, "^~\\&#")
        }
        val hl7profile = HL7Reader.getMessageProfile(hl7message.toString())
        // search hl7 profile map and create translator with config path if found
        var fhirMessage = when (val configPath = HL7Reader.profileDirectoryMap[hl7profile]) {
            null -> HL7toFhirTranslator(inputSchema).translate(hl7message)
            else -> HL7toFhirTranslator(configPath).translate(hl7message)
        }

        val stamper = ConditionStamper(LookupTableConditionMapper(Metadata.getInstance()))
        fhirMessage.getObservations().forEach { observation ->
            stamper.stampObservation(observation)
        }

        if (receiver != null) {
            fhirMessage = applyConditionFilter(receiver, fhirMessage)
        }

        return Pair(fhirMessage, hl7message)
    }

    /**
     * @throws CliktError if senderSchema is present, but unable to be read.
     * @return If senderSchema is present, apply it, otherwise just return the input bundle.
     */
    private fun applySenderTransforms(bundle: Bundle, senderSchema: String?): FhirTransformer.BundleWithMessages {
        return when {
            senderSchema != null -> {
                val transformer = FhirTransformer(senderSchema)
                val returnedBundle = transformer.process(bundle)
                FhirTransformer.BundleWithMessages(returnedBundle, transformer.warnings, transformer.errors)
            }

            else -> FhirTransformer.BundleWithMessages(bundle = bundle, mutableListOf(), mutableListOf())
        }
    }

    /**
     * @throws CliktError if receiverSchema is present, but unable to be read.
     * @throws CliktError if enrichmentSchemaName is present, but unable to be read.
     * @return If receiverSchema is present, apply it, otherwise just return the input bundle.
     */
    private fun applyReceiverEnrichmentAndTransforms(bundle: Bundle, isCli: Boolean): MessageOrBundle {
        val messageOrBundle = MessageOrBundle()
        setEnrichmentSchemaFields(messageOrBundle, applyEnrichmentSchemas(bundle, isCli))

        if (isCli && receiverSchema != null) {
            val transformer = FhirTransformer(receiverSchema!!)
            val returnedBundle = transformer.process(messageOrBundle.bundle!!)
            messageOrBundle.receiverTransformWarnings.addAll(transformer.warnings)
            messageOrBundle.receiverTransformErrors.addAll(transformer.errors)
            messageOrBundle.receiverTransformPassed = transformer.errors.isEmpty()
            messageOrBundle.bundle = returnedBundle
        }

        return messageOrBundle
    }

    /**
     * Applies the enrichment schema to the bundle.
     */
    private fun applyEnrichmentSchemas(bundle: Bundle, isCli: Boolean): FhirTransformer.BundleWithMessages {
        var enrichedbundle = bundle
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        if (isCli && !enrichmentSchemaNames.isNullOrEmpty()) {
            enrichmentSchemaNames!!.split(",").forEach { currentEnrichmentSchemaName ->
                val transformer = FhirTransformer(currentEnrichmentSchemaName)
                val returnedBundle = transformer.process(
                    enrichedbundle
                )
                errors.addAll(transformer.errors)
                warnings.addAll(transformer.warnings)
                enrichedbundle = returnedBundle
            }
        }
        return FhirTransformer.BundleWithMessages(enrichedbundle, warnings, errors)
    }

    /**
     * Apply both sender and receiver schemas if present.
     * @return the FHIR bundle after having sender and/or receiver schemas applied to it.
     */
    private fun handleSenderAndReceiverTransforms(
        messageOrBundle: MessageOrBundle,
        senderSchema: String?,
        isCli: Boolean,
    ): MessageOrBundle {
        val senderTransformInfo = applySenderTransforms(messageOrBundle.bundle!!, senderSchema)
        val receiverTransformInfo = applyReceiverEnrichmentAndTransforms(senderTransformInfo.bundle, isCli)
        messageOrBundle.bundle = receiverTransformInfo.bundle
        messageOrBundle.senderTransformWarnings.addAll(senderTransformInfo.warnings)
        messageOrBundle.senderTransformErrors.addAll(senderTransformInfo.errors)
        messageOrBundle.senderTransformPassed = senderTransformInfo.errors.isEmpty()
        messageOrBundle.receiverTransformErrors.addAll(receiverTransformInfo.receiverTransformErrors)
        messageOrBundle.receiverTransformWarnings.addAll(receiverTransformInfo.receiverTransformWarnings)
        messageOrBundle.receiverTransformPassed = receiverTransformInfo.receiverTransformPassed &&
            messageOrBundle.receiverTransformPassed
        messageOrBundle.enrichmentSchemaPassed

        return messageOrBundle
    }

    /**
     * @return true if a message header (either the one at hl7ItemIndex or the first one if hl7ItemIndex is null) in the
     * given string contains MSH-2 of `^~\&#`, false otherwise
     */
    private fun hl7MessageHasFiveEncodingChars(hl7String: String): Boolean {
        // This regex should match `MSH|^~\&|` or `MSH|^~\&#`
        val mshStarts = "MSH\\|\\^~\\\\\\&[#|]".toRegex().findAll(hl7String)
        val index = 0
        mshStarts.forEachIndexed { i, matchResult ->
            if (i == index) {
                return matchResult.value == "MSH|^~\\&#"
            }
        }
        return false
    }

    /**
     * Output a [fhirResult] fire bundle data and [actionLogger] logs to the screen or a file.
     */
    private fun outputResult(fhirResult: Bundle, actionLogger: ActionLogger) {
        // Pretty print the JSON output
        val jsonObject = JacksonMapperUtilities.defaultMapper
            .readValue(FhirTranscoder.encode(fhirResult), Any::class.java)
        val prettyText = JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)

        // Write the output to the screen or a file.
        if (outputFile != null) {
            outputFile!!.writeText(prettyText, Charsets.UTF_8)
            echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            echo("-- FHIR OUTPUT ------------------------------------------")
            echo(prettyText)
            echo("-- END FHIR OUTPUT --------------------------------------")
        }

        actionLogger.errors.forEach { echo("ERROR: ${it.detail.message}") }
        actionLogger.warnings.forEach { echo("ERROR: ${it.detail.message}") }
    }

    /**
     * Output an HL7 [message] to the screen or a file.
     */
    private fun outputResult(message: Message) {
        val text = message.encodePreserveEncodingChars()
        if (outputFile != null) {
            outputFile!!.writeText(text, Charsets.UTF_8)
            echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            echo("-- HL7 OUTPUT ------------------------------------------")
            text.split("\r").forEach { echo(it) }
            echo("-- END HL7 OUTPUT --------------------------------------")
        }
    }
}

/**
 * Process a FHIR path using a FHIR bundle as input. This command is useful to parse sample FHIR data to make
 * sure your FHIR path is correct in your schemas.
 */
class FhirPathCommand : CliktCommand(
    name = "fhirpath",
    help = "Input FHIR paths to be resolved using the input FHIR bundle"
) {

    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Constants for the FHIR Path context.
     */
    private val constants by option(
        "-c", "--constants",
        help = "a constant in the form of key=value to be used in FHIR Path. Option can be repeated."
    ).associate()

    /**
     * A parser to print out the contents of a resource.
     */
    private val fhirResourceParser = FhirContext.forR4().newJsonParser()

    private var focusPath = "Bundle"
    private var focusResource: Base? = null
    private var fhirPathContext: CustomContext? = null

    init {
        fhirResourceParser.setPrettyPrint(true)
        fhirResourceParser.isOmitResourceId = true
        fhirResourceParser.isSummaryMode = true
    }

    override fun run() {
        fun printHelp() {
            echo("", true)
            echo("Using the FHIR bundle in ${inputFile.absolutePath}...", true)
            echo("Special commands:", true)
            echo(
                "\t!![FHIR path]                     - appends specified FHIR path to the end of the last path",
                true
            )
            echo("\tquit, exit                       - exit the tool", true)
            echo("\treset                            - Sets %resource to Bundle", true)
            echo("\tresource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path", true)
        }
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        val bundle = FhirTranscoder.decode(contents)
        focusResource = bundle
        val constantList = mutableMapOf("rsext" to "'https://reportstream.cdc.gov/fhir/StructureDefinition/'")
        constants.entries.forEach {
            constantList[it.key] = it.value
        }
        echo("Using constants:")
        constantList.forEach { (name, value) ->
            echo("\t$name=$value")
        }
        fhirPathContext = CustomContext(bundle, bundle, constantList, CustomFhirPathFunctions())
        printHelp()

        var lastPath = ""

        // Loop until you press CTRL-C or ENTER at the prompt.
        while (true) {
            echo("", true)
            echo("%resource = $focusPath")
            echo("Last path = $lastPath")
            print("FHIR path> ") // This needs to be a print as an echo does not show on the same line

            val input = readln()

            // Process the input checking for special/custom commands
            when {
                input.isBlank() -> printHelp()

                input == "quit" || input == "exit" -> throw ProgramResult(0)

                input.startsWith("resource") -> setFocusResource(input, bundle)

                input == "reset" -> setFocusResource("Bundle", bundle)

                else -> {
                    val path = if (input.startsWith("!!")) {
                        input.replace("!!", lastPath)
                    } else {
                        input
                    }
                    if (path.isBlank()) {
                        printHelp()
                    } else {
                        evaluatePath(path, bundle)
                        lastPath = path
                    }
                }
            }
        }
    }

    /**
     * Set the focus resource only is the path specified in the [input] string points to a resource or
     * reference in the [bundle].
     */
    private fun setFocusResource(input: String, bundle: Bundle) {
        fun setFocusPath(newPath: String) {
            focusPath = if (newPath.startsWith("%resource")) {
                newPath.replace("%resource", focusPath)
            } else {
                newPath
            }
        }

        val inputParts = input.split("=", ":", limit = 2)
        if (inputParts.size != 2 || inputParts[1].isBlank()) {
            echo("Setting %resource must be in the form of 'resource[= | :]<FHIR path>'")
        } else {
            try {
                val path = inputParts[1].trim().trimStart('\'').trimEnd('\'')
                val pathExpression = FhirPathUtils.parsePath(path)
                    ?: throw FhirPathExecutionException("Invalid FHIR Path: null or blank")
                val resourceList = FhirPathUtils.pathEngine.evaluate(
                    fhirPathContext, focusResource!!, bundle, bundle, pathExpression
                )
                if (resourceList.size == 1) {
                    setFocusPath(path)
                    focusResource = resourceList[0] as Base
                    fhirPathContext?.let { it.focusResource = focusResource as Base }
                } else {
                    echo(
                        "Resource path must evaluate to 1 resource, but got a collection of " +
                            "${resourceList.size} resources"
                    )
                }
            } catch (e: Exception) {
                echo("Error evaluating resource path: ${e.message}")
            }
        }
    }

    /**
     * Evaluate a FHIR path from the given [input] string in the [bundle].
     */
    private fun evaluatePath(input: String, bundle: Bundle) {
        // Check the syntax for the FHIR path
        try {
            val values = FhirPathUtils.evaluate(fhirPathContext, focusResource!!, bundle, input)

            values.forEach {
                // Print out the value, but add a dash to each collection entry if more than one
                echo("${if (values.size > 1) "- " else ""}${fhirBaseAsString(it)}", true)
            }
            echo("Number of results = ${values.size} ----------------------------", true)
        } catch (e: NotImplementedError) {
            echo("One or more FHIR path functions specified are not implemented in the library")
        } catch (e: FHIRLexerException) {
            echo("Invalid FHIR path specified")
        }
    }

    /**
     * Convert a [value] that is a FHIR base to a string.
     * @return a string representing the contents of the FHIR base
     */
    private fun fhirBaseAsString(value: Base): String {
        return when {
            value.isPrimitive -> "Primitive: $value"

            // References
            value is Reference ->
                "Reference to ${value.reference} - use resolve() to navigate into it"

            // An extension
            value is Extension -> {
                "extension('${value.url}')"
            }

            // This base is a resource
            else ->
                fhirPropertiesAsString(value)
        }
    }

    /**
     * Generate a string representation of all the properties in a resource
     */
    private fun fhirPropertiesAsString(value: Base): String {
        val stringValue = StringBuilder()
        stringValue.append("{  ")
        value.children().forEach { property ->
            when {
                // Empty values
                property.values.isEmpty() ->
                    stringValue.append("")

                // An array
                property.isList -> {
                    stringValue.append("\n\t\"${property.name}\": [ \n")
                    property.values.forEach { value ->
                        stringValue.append("\t\t")
                        if (value is Extension) {
                            stringValue.append("extension('${value.url}'),")
                        } else {
                            stringValue.append("$value,")
                        }
                        stringValue.append("\n")
                    }
                    stringValue.append("  ]")
                }

                // A reference
                property.values[0] is Reference -> {
                    stringValue.append("\n\t\"${property.name}\": ")
                    stringValue.append("Reference to ${(property.values[0] as Reference).reference}")
                }

                // An extension
                property.values[0] is Extension -> {
                    stringValue.append("\n\t\"${property.name}\": ")
                    stringValue.append("extension('${(property.values[0] as Extension).url}')")
                }

                // A primitive
                property.values[0].isPrimitive -> {
                    stringValue.append("\n\t\"${property.name}\": \"${property.values[0]}\"")
                }

                else -> {
                    stringValue.append("\n\t\"${property.name}\": ${property.values[0]}")
                }
            }
        }
        stringValue.append("\n}\n")
        return stringValue.toString()
    }
}