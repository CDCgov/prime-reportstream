package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonValue
import gov.cdc.prime.reportstream.shared.queue_message.ITopic
import gov.cdc.prime.reportstream.shared.validation.AbstractItemValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrOnboardingValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrValidator
import gov.cdc.prime.reportstream.shared.validation.NoopItemValidator

/**
 * A submission with topic FULL_ELR will be processed using the full ELR pipeline (fhir engine), submissions
 * from a sender with topic COVID_19 will be processed using the covid-19 pipeline.
 */
enum class Topic: ITopic {

    FULL_ELR {
        @JsonValue override fun jsonVal(): String { return "full-elr" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    ETOR_TI {
        @JsonValue override fun jsonVal(): String { return "etor-ti" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    ELR_ELIMS {
        @JsonValue override fun jsonVal(): String { return "elr-elims" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return true }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    COVID_19 {
        @JsonValue override fun jsonVal(): String { return "covid-19" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    MONKEYPOX {
        @JsonValue override fun jsonVal(): String { return "monkeypox" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    TEST {
        @JsonValue override fun jsonVal(): String { return "test" }
        override fun isUniversalPipeline(): Boolean { return false }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
    },
    MARS_OTC_ELR {
        override fun jsonVal(): String { return "mars-otc-elr" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return MarsOtcElrValidator() }
    },
    MARS_OTC_ELR_ONBOARDING {
        override fun jsonVal(): String { return "mars-otc-elr-onboarding" }
        override fun isUniversalPipeline(): Boolean { return true }
        override fun isSendOriginal(): Boolean { return false }
        override fun validator(): AbstractItemValidator { return MarsOtcElrOnboardingValidator() }
    }

}