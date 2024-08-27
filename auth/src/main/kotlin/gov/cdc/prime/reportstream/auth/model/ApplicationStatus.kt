package gov.cdc.prime.reportstream.auth.model

data class ApplicationStatus(
    val application: String,
    val status: String,
    val uptime: String,
//    val hash: String
)