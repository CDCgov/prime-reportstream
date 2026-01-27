package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.FhirPathExecutionException
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.fhirconverter.translation.hl7.FhirToHl7Context
import gov.cdc.prime.fhirconverter.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.fhirconverter.translation.hl7.SchemaException
import gov.cdc.prime.fhirconverter.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ConditionStamper
import gov.cdc.prime.router.azure.LookupTableConditionMapper
import gov.cdc.prime.router.azure.ReportFunction.MessageOrBundleStringified
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ItemEventData
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEventBuilder
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemProcessingErrorEventBuilder
import gov.cdc.prime.router.azure.observability.event.ReportStreamReportEventBuilder
import gov.cdc.prime.router.azure.observability.event.ReportStreamReportProcessingErrorEventBuilder
import gov.cdc.prime.router.azure.observability.event.SubmissionEventData
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.cli.helpers.HL7DiffHelper
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter.FhirExpressionEvaluationResult
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter.ReceiverFilterEvaluationResult
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers.RouterSchemaReferenceResolverHelper
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
class ProcessFhirCommands :
    CliktCommand(
        name = "fhirdata",
    ) {
    override fun help(context: Context): String = "Process data into/from FHIR"

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
        .choice("local", "test", "staging", "prod", "demo1")
        .default("local", "local environment")

    /**
     * Sender schema location
     */
    private val senderSchemaParam by option("-s", "--sender-schema", help = "Sender schema location")

    private val defaultInputSchema = "./metadata/HL7/catchall"

    private val inputSchema by option(
        "--input-schema", help = "Mapping schema for input file"
    ).default(defaultInputSchema)

    private val hl7DiffHelper = HL7DiffHelper()

    /**
     * The environment specified by the command line parameters
     */
    val environment: Environment by lazy {
        Environment.get(environmentParam)
    }

    /**
     * The access token left by a previous login command as specified by the command line parameters
     */
    private val oktaAccessToken: String by lazy {

        if (environment.oktaApp == null) {
            "placeholder_token"
        } else {
            OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: abort(
                    "Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access token for the $environmentParam environment."
                )
        }
    }

    override fun run() {
        val messageOrBundle =
            processFhirDataRequest(
                inputFile,
                environment,
                receiverNameParam,
                orgNameParam,
                senderSchemaParam,
                true,
                oktaAccessToken,
                inputSchema
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
        environment: Environment,
        receiverName: String?,
        orgName: String?,
        senderSchema: String?,
        isCli: Boolean,
        accessToken: String,
        inputSchema: String = defaultInputSchema,
    ): MessageOrBundle {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        // Check on the extension of the file for supported operations
        val inputFileType = inputFile.extension.uppercase()
        val receiver = getReceiver(environment, receiverName, orgName, isCli, accessToken)

        val messageOrBundle = MessageOrBundle()
        when {
            // HL7 to FHIR conversion
            inputFileType == "HL7" &&
                (
                    (isCli && outputFormat == MimeFormat.FHIR.toString()) ||
                        (receiver != null && receiver.format == MimeFormat.FHIR)
                    ) -> {
                val fhirMessage = convertHl7ToFhir(contents, inputSchema).first
                messageOrBundle.bundle = fhirMessage
                handleSendAndReceiverFhirEnrichments(messageOrBundle, receiver, senderSchema, isCli)

                return messageOrBundle
            }

            // FHIR to HL7 conversion
            (inputFileType == "FHIR" || inputFileType == "JSON") &&
                (
                    (isCli && outputFormat == MimeFormat.HL7.toString()) ||
                        (
                            receiver != null &&
                            (receiver.format == MimeFormat.HL7 || receiver.format == MimeFormat.HL7_BATCH)
                        )
                    ) -> {
                messageOrBundle.bundle = FhirTranscoder.decode(contents)
                handleSendAndReceiverFhirEnrichments(messageOrBundle, receiver, senderSchema, isCli)

                convertFhirToHl7(
                    (receiver?.translation ?: defaultHL7Configuration) as Hl7Configuration,
                    receiver,
                    isCli,
                    messageOrBundle
                )

                return messageOrBundle
            }

            // FHIR to FHIR conversion
            (inputFileType == "FHIR" || inputFileType == "JSON") &&
                (
                    (isCli && outputFormat == MimeFormat.FHIR.toString()) ||
                        (receiver != null && receiver.format == MimeFormat.FHIR)
                    ) -> {
                messageOrBundle.bundle = FhirTranscoder.decode(contents)
                handleSendAndReceiverFhirEnrichments(messageOrBundle, receiver, senderSchema, isCli)

                return messageOrBundle
            }

            // HL7 to FHIR to HL7 conversion
            inputFileType == "HL7" &&
                (
                    (isCli && outputFormat == MimeFormat.HL7.toString()) ||
                        (
                            receiver != null &&
                                (receiver.format == MimeFormat.HL7 || receiver.format == MimeFormat.HL7_BATCH)
                            )
                    ) -> {
                val (bundle2, inputMessage) = convertHl7ToFhir(contents, inputSchema)

                messageOrBundle.bundle = bundle2
                handleSendAndReceiverFhirEnrichments(messageOrBundle, receiver, senderSchema, isCli)

                convertFhirToHl7(
                    (receiver?.translation ?: defaultHL7Configuration) as Hl7Configuration,
                    receiver,
                    isCli,
                    messageOrBundle
                )
                if (isCli && diffHl7Output != null) {
                    val differences = hl7DiffHelper.diffHl7(messageOrBundle.message!!, inputMessage)
                    echo("-------diff output")
                    echo("There were ${differences.size} differences between the input and output")
                    differences.forEach { echo(it.toString()) }
                }
                return messageOrBundle
            }

            else -> throw CliktError("File extension ${inputFile.extension} is not supported.")
        }
    }

    private fun handleSendAndReceiverFhirEnrichments(
        messageOrBundle: MessageOrBundle,
        receiver: Receiver?,
        senderSchema: String?,
        isCli: Boolean,
    ) {
        stampObservations(messageOrBundle)

        val senderSchemaName = when {
            senderSchema != null -> senderSchema
            isCli && senderSchemaParam != null -> senderSchemaParam
            else -> null
        }

        if (senderSchemaName != null) {
            handleSenderTransforms(messageOrBundle, senderSchemaName)
        }

        if (receiver != null && messageOrBundle.bundle != null) {
            handleReceiverFilters(receiver, messageOrBundle, isCli)
        }

        val receiverEnrichmentSchemaNames = when {
            receiver != null && receiver.enrichmentSchemaNames.isNotEmpty() -> {
                receiver.enrichmentSchemaNames.joinToString(",")
            }

            isCli && enrichmentSchemaNames != null -> enrichmentSchemaNames
            else -> null
        }

        handleReceiverFhirEnrichments(messageOrBundle, receiverEnrichmentSchemaNames)
    }

    fun handleReceiverFhirEnrichments(messageOrBundle: MessageOrBundle, schemaNames: String?) {
        if (!schemaNames.isNullOrEmpty()) {
            schemaNames.split(",").forEach { currentEnrichmentSchemaName ->
                val transformer = FhirTransformer(
                    RouterSchemaReferenceResolverHelper.retrieveFhirSchemaReference(currentEnrichmentSchemaName),
                    errors = messageOrBundle.enrichmentSchemaErrors,
                    warnings = messageOrBundle.enrichmentSchemaWarnings
                )
                val output = transformer.process(
                    messageOrBundle.bundle!!
                )

                messageOrBundle.bundle = output
            }
        }
    }

    fun handleReceiverFilters(receiver: Receiver, messageOrBundle: MessageOrBundle, isCli: Boolean) {
        if (messageOrBundle.bundle?.identifier?.value == null) {
            // this is just for logging so it is fine to just make it up
            messageOrBundle.bundle?.identifier?.setValue(UUID.randomUUID().toString())
        }
        // TODO: https://github.com/CDCgov/prime-reportstream/issues/16407
        val fhirReceiverFilter = FHIRReceiverFilter(reportStreamEventService = NoopReportStreamEventService())

        evaluateReceiverFilters(receiver, messageOrBundle, fhirReceiverFilter)

        applyConditionFilter(receiver, messageOrBundle, fhirReceiverFilter)

        if (isCli && messageOrBundle.filterErrors.isNotEmpty()) {
            val errorMsgLines = messageOrBundle.filterErrors.map { filterError ->
                "${filterError.filterType} - ${filterError.message} :  \n ${filterError.filter}"
            }
            throw CliktError(errorMsgLines.joinToString("\n"))
        }
    }

    fun evaluateReceiverFilters(
        receiver: Receiver,
        messageOrBundle: MessageOrBundle,
        fhirReceiverFilter: FHIRReceiverFilter,
    ) {
        val bundle = messageOrBundle.bundle!!
        val actionLogger = ActionLogger()

        // filter groups for looped evaluation - condition filter evaluated separately
        val fhirFilters = listOf(
            Pair(receiver.jurisdictionalFilter, ReportStreamFilterType.JURISDICTIONAL_FILTER),
            Pair(receiver.qualityFilter, ReportStreamFilterType.QUALITY_FILTER),
            Pair(receiver.routingFilter, ReportStreamFilterType.ROUTING_FILTER),
            Pair(receiver.processingModeFilter, ReportStreamFilterType.PROCESSING_MODE_FILTER),
        )

        fhirFilters.forEach {
            val filter = it.first
            val filterType = it.second
            try {
                val result = fhirReceiverFilter.evaluateFhirExpressionFilters(
                    receiver,
                    bundle,
                    actionLogger,
                    bundle.identifier.value,
                    filter,
                    filterType.name
                )
                if (result is FhirExpressionEvaluationResult.Failure) {
                    messageOrBundle.filterErrors.add(
                        FilterError(
                            result.failingFilter.filters.joinToString("\n"),
                            "Filter failed",
                            filterType.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                messageOrBundle.filterErrors.add(
                    FilterError(
                        filter.joinToString("\n"),
                        "Invalid filter - ${e.message}",
                        filterType.toString()
                    )
                )
            }
        }
    }

    fun applyConditionFilter(
        receiver: Receiver,
        messageOrBundle: MessageOrBundle,
        fhirReceiverFilter: FHIRReceiverFilter,
    ) {
        val bundle = messageOrBundle.bundle!!

        try {
            val result = fhirReceiverFilter.evaluateObservationConditionFilters(
                receiver,
                bundle,
                ActionLogger(),
                bundle.identifier.value
            )

            if (result is ReceiverFilterEvaluationResult.Success) {
                // update the bundle since observations might have gotten pruned after condition filter evaluation
                messageOrBundle.bundle = result.bundle
            } else {
                result as ReceiverFilterEvaluationResult.Failure
                messageOrBundle.filterErrors.add(
                    FilterError(
                        result.failingFilter.filters.joinToString("\n"),
                        "Filter failed",
                        result.failingFilter.filterType
                    )
                )
            }
        } catch (e: SchemaException) {
            messageOrBundle.filterErrors.add(
                FilterError(
                    receiver.conditionFilter.joinToString("\n"),
                    "Invalid filter - ${e.message}",
                    ReportStreamFilterType.CONDITION_FILTER.toString()
                )
            )
        }
    }

    data class FilterError(val filter: String, val message: String, val filterType: String)

    abstract class MessageOrBundleParent(
        open var senderTransformErrors: MutableList<String> = mutableListOf(),
        open var senderTransformWarnings: MutableList<String> = mutableListOf(),
        open var enrichmentSchemaErrors: MutableList<String> = mutableListOf(),
        open var enrichmentSchemaWarnings: MutableList<String> = mutableListOf(),
        open var receiverTransformErrors: MutableList<String> = mutableListOf(),
        open var receiverTransformWarnings: MutableList<String> = mutableListOf(),
        open var filterErrors: MutableList<FilterError> = mutableListOf(),
    )

    class MessageOrBundle(
        var message: Message? = null,
        var bundle: Bundle? = null,
        override var senderTransformErrors: MutableList<String> = mutableListOf(),
        override var senderTransformWarnings: MutableList<String> = mutableListOf(),
        override var enrichmentSchemaErrors: MutableList<String> = mutableListOf(),
        override var enrichmentSchemaWarnings: MutableList<String> = mutableListOf(),
        override var receiverTransformErrors: MutableList<String> = mutableListOf(),
        override var receiverTransformWarnings: MutableList<String> = mutableListOf(),
        override var filterErrors: MutableList<FilterError> = mutableListOf(),
    ) : MessageOrBundleParent() {

        override fun toString(): String {
            val message = if (this.message != null) {
                this.message.toString()
            } else {
                null
            }

            val bundle = if (this.bundle != null) {
                FhirTranscoder.encode(this.bundle!!)
            } else {
                null
            }

            return ObjectMapper().configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false).writeValueAsString(
                MessageOrBundleStringified(
                    message,
                    bundle,
                    senderTransformErrors,
                    senderTransformWarnings,
                    enrichmentSchemaErrors,
                    senderTransformWarnings,
                    receiverTransformErrors,
                    receiverTransformWarnings,
                    filterErrors,
                )
            )
        }
    }

    private fun getReceiver(
        environment: Environment,
        receiverName: String?,
        orgName: String?,
        isCli: Boolean,
        accessToken: String,
    ): Receiver? {
        if (!receiverName.isNullOrBlank() && !orgName.isNullOrBlank()) {
            if (isCli && !outputFormat.isNullOrBlank()) {
                throw CliktError(
                    "Please specify either a receiver OR an output format. Not both."
                )
            }
            val organizations = GetMultipleSettings().getAll(
                environment = environment,
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
                "Output format is required if the receiver and org " +
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
        hl7Configuration: Hl7Configuration = defaultHL7Configuration,
        receiver: Receiver? = null,
        isCli: Boolean,
        messageOrBundle: MessageOrBundle,
    ) {
        if ((isCli && receiverSchema == null) && (receiver == null || receiver.schemaName.isBlank())) {
            throw CliktError("You must specify a receiver schema using --receiver-schema.")
        }

        val receiverTransformSchemaName = when {
            receiver != null && receiver.schemaName.isNotEmpty() -> receiver.schemaName
            isCli && receiverSchema != null -> receiverSchema
            else -> null
        }

        if (receiverTransformSchemaName != null) {
            val message = FhirToHl7Converter(
                RouterSchemaReferenceResolverHelper.retrieveHl7SchemaReference(
                    receiverTransformSchemaName,
                    BlobAccess.BlobContainerMetadata.build("metadata", Environment.get().storageEnvVar)
                ),
                context = FhirToHl7Context(
                    CustomFhirPathFunctions(),
                    config = HL7TranslationConfig(
                        hl7Configuration = hl7Configuration,
                        receiver
                    ),
                    translationFunctions = CustomTranslationFunctions(),
                ),
                warnings = messageOrBundle.receiverTransformWarnings,
                errors = messageOrBundle.receiverTransformErrors
            ).process(messageOrBundle.bundle!!)
            messageOrBundle.message = message
        }
    }

    private fun stampObservations(
        messageOrBundle: MessageOrBundle,
    ) {
        val stamper = ConditionStamper(LookupTableConditionMapper(Metadata.getInstance()))
        messageOrBundle.bundle?.getObservations()?.forEach { observation ->
            stamper.stampObservation(observation)
        }
    }

    /**
     * Convert an HL7 message or batch as a [hl7String] to a FHIR bundle. [actionLogger] will contain any
     * warnings or errors from the reading of the HL7 data to HL7 objects.  Note that the --hl7-msg-index
     * is required for HL7 batch messages as this function only returns one FHIR bundle.
     * Note: This does not require a schema in case it is being used to see what our internal format message
     * look like.
     * @return a FHIR bundle and the parsed HL7 input that represents the data in the one HL7 message
     */
    private fun convertHl7ToFhir(hl7String: String, schema: String): Pair<Bundle, Message> {
        val hasFiveEncodingChars = hl7MessageHasFiveEncodingChars(hl7String)
        // Some HL7 2.5.1 implementations have adopted the truncation character # that was added in 2.7
        // However, the library used to encode the HL7 message throws an error it there are more than 4 encoding
        // characters, so this work around exists for that scenario
        val stringToEncode = hl7String.replace("MSH|^~\\&#|", "MSH|^~\\&|")
        val hl7message = HL7Reader.parseHL7Message(stringToEncode)

        // if a hl7 parsing failure happens, throw error and show the message
        if (hl7message.toString().lowercase().contains("failed")) {
            throw CliktError("HL7 parser failure. $hl7message")
        }
        if (hasFiveEncodingChars) {
            val msh = hl7message.get("MSH") as Segment
            Terser.set(msh, 2, 0, 1, 1, "^~\\&#")
        }
        // search hl7 profile map and create translator with config path if found
        val fhirMessage = HL7toFhirTranslator(schema).translate(hl7message)

        val stamper = ConditionStamper(LookupTableConditionMapper(Metadata.getInstance()))
        fhirMessage.getObservations().forEach { observation ->
            stamper.stampObservation(observation)
        }

        return Pair(fhirMessage, hl7message)
    }

    /**
     * Apply sender schema to bundle
     * @throws CliktError if senderSchema is present, but unable to be read.
     */
    private fun handleSenderTransforms(messageOrBundle: MessageOrBundle, senderSchema: String) {
        val transformer = FhirTransformer(
            RouterSchemaReferenceResolverHelper.retrieveFhirSchemaReference(senderSchema),
            errors = messageOrBundle.senderTransformErrors,
            warnings = messageOrBundle.senderTransformWarnings
        )
        val returnedBundle = transformer.process(messageOrBundle.bundle!!)
        messageOrBundle.bundle = returnedBundle
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
class FhirPathCommand :
    CliktCommand(
        name = "fhirpath",
    ) {
    override fun help(context: Context): String = "Input FHIR paths to be resolved using the input FHIR bundle"

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
    private fun fhirBaseAsString(value: Base): String = when {
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

// This exists only because ProcessFhirCommands instantiates a FHIRReceiverFilter to access a function that likely could be
// made static
// TODO: https://github.com/CDCgov/prime-reportstream/issues/16407
class NoopReportStreamEventService : IReportStreamEventService {
    override fun sendQueuedEvents(): Unit = throw NotImplementedError()

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendReportEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendReportProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamReportProcessingErrorEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendItemEvent(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: ReportFile,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun sendItemProcessingError(
        eventName: ReportStreamEventName,
        childReport: Report,
        pipelineStepName: TaskAction,
        error: String,
        queueMessage: String,
        shouldQueue: Boolean,
        initializer: ReportStreamItemProcessingErrorEventBuilder.() -> Unit,
    ): Unit = throw NotImplementedError()

    override fun getReportEventData(
        childReportId: UUID,
        childBodyUrl: String,
        parentReportId: UUID?,
        pipelineStepName: TaskAction,
        topic: Topic?,
        queueMessage: String,
    ): ReportEventData = throw NotImplementedError()

    override fun getItemEventData(
        childItemIndex: Int,
        parentReportId: UUID,
        parentItemIndex: Int,
        trackingId: String?,
    ): ItemEventData = throw NotImplementedError()

    override fun getSubmissionEventData(
        childItemIndex: Int,
        parentReportId: UUID?,
        isItemEvent: Boolean,
    ): SubmissionEventData = throw NotImplementedError()
}