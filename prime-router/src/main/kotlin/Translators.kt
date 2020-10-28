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
        return null
    }
}

class SpecimenTypeFreeTranslator : Translator {
    override val topic = "covid-19"
    override val toElement = "standard.Specimen_type_free_text"
    override val fromElements = listOf("standard.Specimen_type_code")

    override fun apply(values: List<String>): String? {
        val codeToText = mapOf(
            "258500001" to "Nasopharyngeal swab",
            "871810001" to "Mid-turbinate nasal swab",
            "697989009" to "Anterior nares swab",
            "258411007" to "Nasopharyngeal aspirate",
            "429931000124105" to "Nasal aspirate",
            "258529004" to "Throat swab",
            "119334006" to "Sputum specimen",
            "119342007" to "Saliva specimen",
            "258607008" to "Bronchoalveolar lavage fluid sample",
            "119364003" to "Serum specimen",
            "119361006" to "Plasma specimen",
            "440500007" to "Dried blood spot specimen",
            "258580003" to "Whole blood sample",
            "122555007" to "Venous blood specimen",
        )
        return codeToText[values[0]]
    }
}

class LabTestResultTranslator : Translator {
    override val topic = "covid-19"
    override val toElement = "standard.Test_result_free_text"
    override val fromElements = listOf("standard.Test_result_coded")

    override fun apply(values: List<String>): String? {
        val codeToText = mapOf(
            "260373001" to "Detected",
            "260415000" to "Not detected",
            "895231008" to "Not detected in pooled specimen",
            "462371000124108" to "Detected in pooled specimen",
            "419984006" to "Inconclusive",
        )
        return codeToText[values[0]]
    }
}


