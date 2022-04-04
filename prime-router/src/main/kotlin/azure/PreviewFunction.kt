package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.messages.PreviewMessage
import gov.cdc.prime.router.messages.PreviewResponseMessage
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging

class PreviewFunction(
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN),
    private val workflowEngine: WorkflowEngine = WorkflowEngine()
) : Logging {
    /**
     * The preview end-point does a translation of the input message to the output payload.
     */
    @FunctionName("preview")
    fun preview(
        @HttpTrigger(
            name = "preview",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "preview"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            try {
                val parameters = checkRequest(request)
                val response = processRequest(parameters)
                val body = mapper.writeValueAsString(response)
                HttpUtilities.okResponse(request, body)
            } catch (ex: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, ex.message ?: "")
            } catch (ex: Exception) {
                logger.error("Internal error for ${it.userName} request: $ex")
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Encapsulate the parameters of the functions.
     */
    data class FunctionParameters(
        val previewMessage: PreviewMessage,
        val receiver: Receiver,
        val sender: Sender,
    )

    /**
     * Encapsulate the result with warnings
     */
    data class ResultWithWarnings(
        val report: Report,
        val warnings: List<ActionLog>,
    )

    /**
     * Throw an [IllegalArgumentException] with a message based on [message], [errors] and [warnings].
     */
    private fun badRequest(
        message: String,
        errors: List<ActionLog> = emptyList(),
        warnings: List<ActionLog> = emptyList()
    ): Nothing {
        val previewErrorMessage = PreviewResponseMessage.Error(
            message,
            errors.map { it.toSummary() },
            warnings.map { it.toSummary() }
        )
        val response = mapper.writeValueAsString(previewErrorMessage)
        throw IllegalArgumentException(response)
    }

    /**
     * Look at the request body.
     * Check for valid values with defaults are assumed if not specified.
     */
    fun checkRequest(request: HttpRequestMessage<String?>): FunctionParameters {
        fun lookupReceiver(organizations: List<DeepOrganization>?, receiverName: String): Receiver? {
            if (organizations == null) return null
            return FileSettings()
                .loadOrganizationList(organizations)
                .findReceiver(receiverName)
        }

        fun lookupSender(organizations: List<DeepOrganization>?, senderName: String): Sender? {
            if (organizations == null) return null
            return FileSettings()
                .loadOrganizationList(organizations)
                .findSender(senderName)
        }

        val body = request.body
            ?: badRequest("Missing body")
        val previewMessage = mapper.readValue(body, PreviewMessage::class.java)
        val receiver = previewMessage.receiver
            ?: lookupReceiver(previewMessage.deepOrganizations, previewMessage.receiverName)
            ?: workflowEngine.settings.findReceiver(previewMessage.receiverName)
            ?: badRequest("Missing receiver")
        val sender = previewMessage.sender
            ?: lookupSender(previewMessage.deepOrganizations, previewMessage.senderName)
            ?: workflowEngine.settings.findSender(previewMessage.senderName)
            ?: badRequest("Missing sender")
        return FunctionParameters(previewMessage, receiver, sender)
    }

    /**
     * Main logic of the Azure function. Useful for unit testing.
     */
    fun processRequest(parameters: FunctionParameters): PreviewResponseMessage.Success {
        return readReport(parameters)
            .translate(parameters)
            .buildResponse(parameters)
    }

    /**
     * Read and parse the preview reports
     */
    private fun readReport(parameters: FunctionParameters): ResultWithWarnings {
        return try {
            val readResult = workflowEngine.parseReport(
                parameters.sender,
                parameters.previewMessage.inputContent,
                emptyMap()
            )
            ResultWithWarnings(readResult.report, readResult.actionLogs.warnings)
        } catch (ae: ActionError) {
            badRequest("Unable to parse input report", errors = ae.details)
        }
    }

    /**
     * Translate a report into a preview report
     */
    private fun ResultWithWarnings.translate(parameters: FunctionParameters): ResultWithWarnings {
        val routedReportResult = workflowEngine.translator.filterAndTranslateForReceiver(
            input = this.report,
            defaultValues = emptyMap(),
            receiver = parameters.receiver,
        )
        if (routedReportResult.reports.isEmpty()) {
            badRequest(
                message = "Unable to translate the report. May not match filters",
                warnings = warnings + routedReportResult.details
            )
        }
        return ResultWithWarnings(routedReportResult.reports.first().report, this.warnings)
    }

    /**
     * Build a preview response from a report and warnings
     */
    private fun ResultWithWarnings.buildResponse(parameters: FunctionParameters): PreviewResponseMessage.Success {
        val content = String(workflowEngine.blob.createBodyBytes(this.report))
        val externalName = Report.formFilename(
            report.id,
            report.schema.name,
            report.bodyFormat,
            report.createdDateTime,
            parameters.receiver.translation,
            workflowEngine.metadata
        )
        return PreviewResponseMessage.Success(
            receiverName = parameters.receiver.fullName,
            externalFileName = externalName,
            content = content,
            warnings = warnings.map { it.toSummary() }
        )
    }

    /**
     * Summarize the [ActionLog]
     */
    private fun ActionLog.toSummary(): String {
        return "${detail.message}"
    }

    companion object {
        private val mapper = jacksonMapperBuilder()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
            .registerModule(JavaTimeModule())
    }
}