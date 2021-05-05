package gov.cdc.prime.router

/**
 * @property scope of the result detail
 * @property id of the result (depends on scope)
 * @property details of the result
 */
data class ResultDetail(val scope: DetailScope, val id: String, val details: String) {
    /**
     * @property REPORT scope for the detail
     * @property ITEM scope for the detail
     */
    enum class DetailScope { PARAMETER, REPORT, ITEM, TRANSLATION }

    override fun toString(): String {
        return "${scope.toString().lowercase()}${if (id.isBlank()) "" else " $id"}: $details"
    }

    companion object {
        fun report(message: String): ResultDetail {
            return ResultDetail(DetailScope.REPORT, "", message)
        }

        fun item(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.ITEM, id, message)
        }

        fun param(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.PARAMETER, id, message)
        }

        fun translation(id: String, message: String): ResultDetail {
            return ResultDetail(DetailScope.TRANSLATION, id, message)
        }
    }
}