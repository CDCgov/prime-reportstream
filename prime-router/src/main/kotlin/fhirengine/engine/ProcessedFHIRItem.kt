package fhirengine.engine

import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.hl7.fhir.r4.model.Bundle

interface IProcessedItem<ParsedType> {

    val rawItem: String
    val index: Int
    val parsedItem: ParsedType?
    val parseError: ActionLogDetail?
    val validationError: ActionLogDetail?
    val bundle: Bundle?

    fun updateParsed(parsed: ParsedType): IProcessedItem<ParsedType>

    fun updateParsed(error: ActionLogDetail): IProcessedItem<ParsedType>

    fun updateValidation(error: ActionLogDetail): IProcessedItem<ParsedType>

    fun getError(): ActionLogDetail?

    fun setBundle(bundle: Bundle): IProcessedItem<ParsedType>
}

data class ProcessedFHIRItem(
    override val rawItem: String,
    override val index: Int,
    override val parsedItem: Bundle? = null,
    override val parseError: ActionLogDetail? = null,
    override val validationError: ActionLogDetail? = null,
    override val bundle: Bundle? = null,
) : IProcessedItem<Bundle> {
    override fun updateParsed(error: ActionLogDetail): ProcessedFHIRItem {
        return this.copy(parseError = error)
    }

    override fun updateParsed(parsed: Bundle): ProcessedFHIRItem {
        return this.copy(parsedItem = parsed)
    }

    override fun updateValidation(error: ActionLogDetail): ProcessedFHIRItem {
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

    override fun getError(): ActionLogDetail? {
        return parseError ?: validationError
    }
}

data class ProcessedHL7Item(
    override val rawItem: String,
    override val index: Int,
    override val parsedItem: Message? = null,
    override val parseError: ActionLogDetail? = null,
    override val validationError: ActionLogDetail? = null,
    val conversionError: ActionLogDetail? = null,
    val parseConfiguration: HL7Reader.Companion.MessageParseConfiguration? = null,
    override val bundle: Bundle? = null,
) : IProcessedItem<Message> {
    override fun updateParsed(error: ActionLogDetail): ProcessedHL7Item {
        return this.copy(parseError = error)
    }

    override fun updateParsed(parsed: Message): ProcessedHL7Item {
        return this.copy(parsedItem = parsed)
    }

    override fun updateValidation(error: ActionLogDetail): ProcessedHL7Item {
        if (parseError == null && parsedItem != null) {
            return this.copy(validationError = error)
        }
        throw RuntimeException("Validation should not be set since item was not parseable")
    }

    override fun setBundle(bundle: Bundle): ProcessedHL7Item {
        if (parseError == null && validationError == null) {
            return this.copy(bundle = bundle)
        }
        throw RuntimeException("Bundle should not be set if the item was not parseable or valid")
    }

    fun setParseConfiguration(parseConfiguration: HL7Reader.Companion.MessageParseConfiguration?): ProcessedHL7Item {
        return this.copy(parseConfiguration = parseConfiguration)
    }

    fun setConversionError(error: ActionLogDetail): ProcessedHL7Item {
        return this.copy(conversionError = error)
    }

    override fun getError(): ActionLogDetail? {
        return parseError ?: validationError ?: conversionError
    }
}