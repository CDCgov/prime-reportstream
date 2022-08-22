package gov.cdc.prime.router

/**
 * A Source can either be a client, a test, or a local file or another report.
 * It is useful for debugging and auditing
 */
sealed class Source

data class FileSource(val fileName: String) : Source()

data class ReportSource(val id: ReportId, val action: String) : Source()

data class ClientSource(val organization: String, val client: String) : Source() {
    val name = "$organization.$client"
}

object TestSource : Source()