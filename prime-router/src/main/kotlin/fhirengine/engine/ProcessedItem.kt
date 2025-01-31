package fhirengine.engine

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import org.hl7.fhir.r4.model.Bundle

interface IProcessedItem<ParsedType> {

    companion object {

        /**
         * Extracts a tracking id from an HL7 message (MSH-10) which serves as a unique identifier for the item
         * that can be referenced throughout the pipeline
         *
         *
         * @param hl7Message the message to get the tracking ID
         */
        fun extractTrackingId(hl7Message: Message): String = Terser(hl7Message).get("MSH-10") ?: ""

        /**
         * Extracts a tracking id from an FHIR Bundle (Bundle.identifier) which
         * serves as a unique identifier for the item that can be referenced throughout the pipeline
         *
         *
         * @param bundle the message to get the tracking ID
         */
        fun extractTrackingId(bundle: Bundle): String = bundle.identifier?.value ?: ""
    }

    val rawItem: String
    val index: Int
    val parsedItem: ParsedType?
    val parseError: ActionLogDetail?
    val validationError: ActionLogDetail?
    val bundle: Bundle?

    fun updateParsed(parsed: ParsedType): IProcessedItem<ParsedType>

    fun updateParsed(error: FHIRConverter.InvalidItemActionLogDetail): IProcessedItem<ParsedType>

    fun updateValidation(error: FHIRConverter.InvalidItemActionLogDetail): IProcessedItem<ParsedType>

    fun getError(): FHIRConverter.InvalidItemActionLogDetail?

    fun setBundle(bundle: Bundle): IProcessedItem<ParsedType>

    fun getTrackingId(): String
}

data class ProcessedFHIRItem(
    override val rawItem: String,
    override val index: Int,
    override val parsedItem: Bundle? = null,
    override val parseError: FHIRConverter.InvalidItemActionLogDetail? = null,
    override val validationError: FHIRConverter.InvalidItemActionLogDetail? = null,
    override val bundle: Bundle? = null,
) : IProcessedItem<Bundle> {
    override fun updateParsed(
        error: FHIRConverter.InvalidItemActionLogDetail,
    ): ProcessedFHIRItem = this.copy(parseError = error)

    override fun updateParsed(parsed: Bundle): ProcessedFHIRItem = this.copy(parsedItem = parsed)

    override fun updateValidation(error: FHIRConverter.InvalidItemActionLogDetail): ProcessedFHIRItem {
        if (parseError == null && parsedItem != null) {
            return this.copy(validationError = error)
        }
        throw RuntimeException("Validation should not be set since item was not parseable")
    }

    override fun setBundle(bundle: Bundle): ProcessedFHIRItem {
        if (parseError == null && validationError == null) {
            return this.copy(bundle = bundle)
        }
        throw RuntimeException("Bundle should not be set if the item was not parseable or valid")
    }

    override fun getTrackingId(): String {
        if (bundle != null) {
            return IProcessedItem.extractTrackingId(bundle)
        }
        return ""
    }

    override fun getError(): FHIRConverter.InvalidItemActionLogDetail? = parseError ?: validationError
}

data class ProcessedHL7Item(
    override val rawItem: String,
    override val index: Int,
    override val parsedItem: Message? = null,
    override val parseError: FHIRConverter.InvalidItemActionLogDetail? = null,
    override val validationError: FHIRConverter.InvalidItemActionLogDetail? = null,
    val conversionError: FHIRConverter.InvalidItemActionLogDetail? = null,
    override val bundle: Bundle? = null,
) : IProcessedItem<Message> {
    override fun updateParsed(
        error: FHIRConverter.InvalidItemActionLogDetail,
    ): ProcessedHL7Item = this.copy(parseError = error)

    override fun updateParsed(parsed: Message): ProcessedHL7Item = this.copy(parsedItem = parsed)

    override fun updateValidation(error: FHIRConverter.InvalidItemActionLogDetail): ProcessedHL7Item {
        if (parseError == null && parsedItem != null) {
            return this.copy(validationError = error)
        }
        throw RuntimeException("Validation should not be set since item was not parseable")
    }

    override fun setBundle(bundle: Bundle): ProcessedHL7Item {
        if (parseError == null && validationError == null && conversionError == null) {
            return this.copy(bundle = bundle)
        }
        throw RuntimeException("Bundle should not be set if the item was not parseable or valid")
    }

    override fun getTrackingId(): String {
        if (bundle != null) {
            return IProcessedItem.extractTrackingId(bundle)
        } else if (parsedItem != null) {
            return IProcessedItem.extractTrackingId(parsedItem)
        }
        return ""
    }

    fun setConversionError(error: FHIRConverter.InvalidItemActionLogDetail): ProcessedHL7Item =
        this.copy(conversionError = error)

    override fun getError(): FHIRConverter.InvalidItemActionLogDetail? =
        parseError ?: validationError ?: conversionError
}