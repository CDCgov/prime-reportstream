package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator

data class SenderConfiguration
@JsonCreator constructor(
    val processingModeCode: String? = null,
    val testingLabName: String? = null,
    val testingLabCLIA: String? = null
)