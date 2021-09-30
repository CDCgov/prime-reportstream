package gov.cdc.prime.router

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 * @property row of csv related to message (set to -1 if not applicable)
 */
data class ResultDetail(
    val scope: DetailScope,
    val id: String,
    val responseMessage: ResponseMessage,
    val row: Int = -1
) {
    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class DetailScope { PARAMETER, REPORT, ITEM, TRANSLATION }

    override fun toString(): String {
        return "${scope.toString().lowercase()}${if (id.isBlank()) "" else " $id"}: ${responseMessage.detailMsg()}"
    }

    companion object {
        fun report(message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.REPORT, "", message, -1)
        }

        fun item(id: String, message: ResponseMessage, row: Int): ResultDetail {
            return ResultDetail(DetailScope.ITEM, id, message, row)
        }

        fun param(id: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.PARAMETER, id, message, -1)
        }

        fun translation(id: String, message: ResponseMessage): ResultDetail {
            return ResultDetail(DetailScope.TRANSLATION, id, message, -1)
        }
    }
}