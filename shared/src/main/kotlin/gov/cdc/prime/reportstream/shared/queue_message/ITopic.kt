package gov.cdc.prime.reportstream.shared.queue_message

interface ITopic {

    fun jsonVal(): String

    fun isUniversalPipeline(): Boolean

    fun isSendOriginal(): Boolean
}