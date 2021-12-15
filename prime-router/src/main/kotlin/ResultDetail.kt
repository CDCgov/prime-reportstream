package gov.cdc.prime.router
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import java.util.UUID

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 * @property row of csv related to message (set to -1 if not applicable)
 */
data class ResultDetail(
    val scope: DetailScope,
    val trackingId: String,
    val responseMessage: ResponseMessage,
    val row: Int = -1,
    var reportId: UUID? = null,
    var action: Action? = null,
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
    enum class DetailScope { PARAMETER, REPORT, ITEM, TRANSLATION }
    enum class Type { INFO, TRANSFORMATION, WARNING, ERROR }

    override fun toString(): String {
        val tracking = if (trackingId.isBlank()) "" else " $trackingId"
        return "${scope.toString().lowercase()}$tracking: ${responseMessage.detailMsg()}"
    }

    companion object {
        fun report(message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.REPORT, "", message, -1)
        }

        fun item(trackingId: String, message: ResponseMessage, row: Int): ResultDetail {
            return ResultDetail(DetailScope.ITEM, trackingId, message, row)
        }

        fun param(trackingId: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.PARAMETER, trackingId, message, -1)
        }

        fun translation(trackingId: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.TRANSLATION, trackingId, message, -1)
        }
    }
}

class ResultError(message: String?, val detail: ResultDetail) : Error(message)

class ResultErrors(val details: List<ResultDetail>) : Error()