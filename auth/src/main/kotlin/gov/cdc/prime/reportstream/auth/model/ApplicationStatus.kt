package gov.cdc.prime.reportstream.auth.model

/**
 * Simple json response model for application status
 */
data class ApplicationStatus(val application: String, val status: String, val uptime: String)