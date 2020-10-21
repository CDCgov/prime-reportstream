package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream

data class Receiver(
    val name: String,
    val topic: String,
    val schema: String,
    val description: String = "",
    val patterns: Map<String, String> = emptyMap(),
    val transforms: Map<String, String> = emptyMap(),
    val address: String = "",
    val format: TopicFormat = TopicFormat.CSV
) {

    enum class TopicFormat {
        CSV,
        HL7,
        FHIR
    }

    companion object {
        const val defaultReceivers = "metadata/recievers.yml"

        val receivers: List<Receiver> get() = receiversStore
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        data class ReceiversList(
            val receivers: List<Receiver>
        )

        fun loadReceiversList(receiversStream: InputStream? = null) {
            val loadingStream = receiversStream ?: File(defaultReceivers).inputStream()
            val receiversList = mapper.readValue<ReceiversList>(loadingStream)
            loadReceivers(receiversList.receivers)
        }

        fun loadReceivers(receivers: List<Receiver>) {
            receiversStore = receivers
        }

        fun get(name: String): Receiver? {
            return receiversStore.first { it.name == name }
        }

        private var receiversStore: List<Receiver> = ArrayList()
    }
}