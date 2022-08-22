package gov.cdc.prime.router.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

typealias RetryItems = List<String>

data class RetryToken(
    var retryCount: Int,
    val items: List<String>
) {
    fun toJSON(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        private val mapper = ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
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