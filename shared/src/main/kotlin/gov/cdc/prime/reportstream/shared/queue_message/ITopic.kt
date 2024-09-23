package gov.cdc.prime.reportstream.shared.queue_message

import com.fasterxml.jackson.annotation.JsonValue
import gov.cdc.prime.reportstream.shared.validation.IItemValidator

interface ITopic {

    @JsonValue fun jsonVal(): String

    fun isUniversalPipeline(): Boolean

    fun isSendOriginal(): Boolean

    fun validator(): IItemValidator
}