package gov.cdc.prime.router

interface Translator {
    val topic: String
    val toElement: String
    val fromElements: List<String>
    fun apply(values: List<String>): String?
}

class MITranslator : Translator {
    override val topic = "covid-19"
    override val toElement = "standard.Patient_middle_initial"
    override val fromElements = listOf("standard.patient_middle_name")

    override fun apply(values: List<String>): String? {
        return values[0].substring(0..0).toUpperCase()
    }
}

class SendingAppTranslator : Translator {
    override val topic = "covid-19"
    override val toElement = "standard.Sending_application"
    override val fromElements = emptyList<String>()

    override fun apply(values: List<String>): String {
        return "PRIME"
    }
}

class SendingAppIdTranslator : Translator {
    override val topic = "covid-19"
    override val toElement = "standard.Sending_application_id"
    override val fromElements = emptyList<String>()

    override fun apply(values: List<String>): String? {
        return "1.0.0.1" // TODO: Real OID when we have it
    }
}



