package gov.cdc.prime.router
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.time.Instant
import java.util.UUID

private val mapper = jacksonMapperBuilder().build()
/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 * @property row of csv related to message (set to -1 if not applicable)
 */
data class ActionDetail(
    val scope: DetailScope,
    val trackingId: String,
    val responseMessage: ResponseMessage,
    val row: Int = -1,
    val context: JsonNode = mapper.valueToTree(responseMessage),
    var reportId: UUID? = null,
    var action: Action? = null,
    val type: Type = Type.info,
    val time: Instant = Instant.now(),
) {
    val rowNumber: Int
        get() = row + 1

    fun getActionId(): Long {
        return action!!.actionId
    }

    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class DetailScope { parameter, report, item, translation }

    enum class Type { info, warning, error, filter }

    override fun toString(): String {
        val tracking = if (trackingId.isBlank()) "" else " $trackingId"
        return "${scope.toString().lowercase()}$tracking: ${responseMessage.detailMsg()}"
    }

    companion object {
        fun report(message: ResponseMessage, type: Type): ActionDetail {
            return ActionDetail(DetailScope.report, "", message, -1, type = type)
        }

        fun report(message: String, type: Type): ActionDetail {
            val reportMessage = InvalidReportMessage(message)
            return ActionDetail(DetailScope.report, "", reportMessage, -1, type = type)
        }

        fun item(trackingId: String, message: ResponseMessage, row: Int, type: Type): ActionDetail {
            return ActionDetail(DetailScope.item, trackingId, message, row, type = type)
        }

        fun param(trackingId: String, message: ResponseMessage, type: Type = Type.error): ActionDetail {
            return ActionDetail(DetailScope.parameter, trackingId, message, -1, type = type)
        }
    }
}

class ActionError(message: String?, val detail: ActionDetail) : Error(message)

class ActionErrors(val details: List<ActionDetail>) : Error()