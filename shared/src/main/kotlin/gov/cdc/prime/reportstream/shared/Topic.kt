package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonValue

/**
 * A submission with topic FULL_ELR will be processed using the full ELR pipeline (fhir engine), submissions
 * from a sender with topic COVID_19 will be processed using the covid-19 pipeline.
 */
enum class Topic: ITopic {

    FULL_ELR {
        @JsonValue override fun jsonVal(): String { return "full-elr" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
    },
    ETOR_TI {
        @JsonValue override fun jsonVal(): String { return "etor-ti" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
    },
    ELR_ELIMS {
        @JsonValue override fun jsonVal(): String { return "elr-elims" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return true }
    },
    COVID_19 {
        @JsonValue override fun jsonVal(): String { return "covid-19" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
    },
    MONKEYPOX {
        @JsonValue override fun jsonVal(): String { return "monkeypox" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
    },
    TEST {
        @JsonValue override fun jsonVal(): String { return "test" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
    }

}