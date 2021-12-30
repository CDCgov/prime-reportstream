package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.records.ReportFileRecord
import gov.cdc.prime.router.common.MixedMultiPart
import gov.cdc.prime.router.tokens.OktaAuthentication
import java.time.OffsetDateTime
import java.util.UUID

class SourcesFunction(
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) {
    @FunctionName("getSources")
    fun getSources(
        @HttpTrigger(
            name = "getSources",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "sources"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            try {
                val parameters = try {
                    checkParameters(request)
                } catch (e: IllegalArgumentException) {
                    return@checkAccess HttpUtilities.badRequestResponse(request, e.message ?: "")
                }
                val (status, payload) = processRequest(parameters)
                when (status) {
                    Status.OK -> HttpUtilities.okResponse(request, payload)
                    Status.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, payload)
                    Status.NOT_FOUND -> HttpUtilities.badRequestResponse(request, payload)
                }
            } catch (e: Exception) {
                // TODO: Log
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    data class FunctionParameters(
        val reportId: UUID?,
        val reportFileName: String?,
        val externalFileName: String?,
        val synthesize: Boolean,
        val limit: Int,
        val offset: Int,
        val merge: Boolean,
    )

    internal fun checkParameters(request: HttpRequestMessage<String?>): FunctionParameters {
        TODO()
    }

    enum class Status {
        OK,
        NOT_FOUND,
        BAD_REQUEST,
    }

    internal fun processRequest(parameters: FunctionParameters): Pair<Status, String> {
        val reportFile = findOutputFile(parameters)
            ?: return Pair(Status.NOT_FOUND, "Report is not found")

        if (reportFile.nextAction != TaskAction.send || reportFile.nextAction != TaskAction.download)
            return Pair(Status.BAD_REQUEST, "Report is not a sent or downloaded")

        val ancestors = findItemAncestors(reportFile.reportId, parameters.offset, parameters.limit)
        if (ancestors.isEmpty())
            return Pair(Status.NOT_FOUND, "No source files found")

        val sources = fetchSources(ancestors, parameters)
        val emptySource = sources.find { it.body.isEmpty() }
        if (emptySource != null)
            return Pair(Status.BAD_REQUEST, "Could not fetch blob: ${emptySource.blobURL}")

        val multiPart = sources
            .synthesize(parameters)
            .merge(parameters)
            .buildMultipart(parameters)
        val payload = multiPart.serialize()
        return Pair(Status.OK, payload)
    }

    private fun findOutputFile(parameters: FunctionParameters): ReportFileRecord? {
        TODO()
    }

    data class ItemId(
        val reportId: UUID,
        val index: Int
    )

    private fun findItemAncestors(reportId: UUID, offset: Int, limit: Int): List<ItemId> {
        TODO()
    }

    private data class Source(
        val sourceReport: UUID,
        val sourceIndices: List<Int>,
        val outputReportId: UUID,
        val outputIndices: List<Int>,
        val sender: String,
        val blobURL: String,
        val receivedAt: OffsetDateTime,
        val fileName: String,
        val body: String,
        val format: Report.Format,
    )

    private fun fetchSources(items: List<ItemId>, parameters: FunctionParameters): List<Source> {
        TODO()
    }

    private fun List<Source>.synthesize(parameters: FunctionParameters): List<Source> {
        TODO()
    }

    private fun List<Source>.merge(parameters: FunctionParameters): List<Source> {
        TODO()
    }

    private fun List<Source>.buildMultipart(parameters: FunctionParameters): MixedMultiPart {
        TODO()
    }

    private fun List<Source>.createMetadata(parameters: FunctionParameters): String {
        TODO()
    }
}