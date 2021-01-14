package gov.cdc.prime.router.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

typealias RetryItems = List<String>

data class RetryTransport(
    val index: Int,
    val items: List<String> = emptyList()
)

data class RetryToken(
    var retryCount: Int,
    val transports: List<RetryTransport>
) {
    fun toJSON(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        private val mapper = ObjectMapper().registerModule(KotlinModule())

        fun fromJSON(json: String?): RetryToken? {
            if (json == null || json.isBlank()) return null
            return mapper.readValue(json, RetryToken::class.java)
        }

        fun isAllItems(items: RetryItems?): Boolean {
            if (items == null) return false
            return items.size == 1 && items[0] == "*"
        }

        val allItems: RetryItems = listOf("*")
    }
}