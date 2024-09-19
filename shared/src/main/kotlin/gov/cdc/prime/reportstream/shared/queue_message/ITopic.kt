package gov.cdc.prime.reportstream.shared.queue_message

import com.fasterxml.jackson.annotation.JsonValue

interface ITopic {

    @JsonValue fun jsonVal(): String

    fun isUniversalPipeline(): Boolean

    fun isSendOriginal(): Boolean

    fun validator(): Any
}