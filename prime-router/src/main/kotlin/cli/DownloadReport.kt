package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder

class DownloadReport : CliktCommand(
    name = "downloadMessage",
    help = "Download a message from a given environment with PII removed."
) {
    private val env by option("-e", "--env", help = "The environment to grab the record from").required()

    private val reportId by option("-r", "--report-id", help = "The report id to grab").required()

    private val outputFile by option("-o", "--output-file", help = "output file")
        .file()

    private val removePII by option("--remove-pii", help = "True or false value. Must be true or not set for prod.")

    override fun run() {
        if (!CommandUtilities.isApiAvailable(Environment.get(env))) {
            abort("The $env environment's API is not available or you have an invalid access token.")
        }

        val reportId = ReportId.fromString(reportId)
        val requestedReport = WorkflowEngine().db.fetchReportFile(reportId)

        if (requestedReport.bodyUrl != null && requestedReport.bodyUrl.toString().lowercase().endsWith("fhir")) {
            val contents = BlobAccess.downloadBlobAsByteArray(requestedReport.bodyUrl)

            val content = if (removePII == null || removePII.toBoolean()) {
                PIIRemovalCommands().removePii(FhirTranscoder.decode(contents.toString(Charsets.UTF_8)))
            } else {
                if (env == "prod") {
                    abort("Must remove PII for messages from prod.")
                }

                val jsonObject = JacksonMapperUtilities.defaultMapper
                    .readValue(contents.toString(Charsets.UTF_8), Any::class.java)
                JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)
            }

            if (outputFile != null) {
                outputFile!!.writeText(content, Charsets.UTF_8)
            } else {
                echo("-- MESSAGE OUTPUT ------------------------------------------")
                echo(content)
                echo("-- END MESSAGE OUTPUT --------------------------------------")
            }
        } else if (requestedReport.bodyUrl == null) {
            abort("The requested report does not exist.")
        } else {
            abort("The requested report is not fhir.")
        }
    }
}