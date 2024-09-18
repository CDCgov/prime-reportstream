package gov.cdc.prime.reportstream.shared

interface ITopic {

    fun jsonVal(): String

    fun isUniversalPipeline(): Boolean

    fun isSendOriginal(): Boolean
}