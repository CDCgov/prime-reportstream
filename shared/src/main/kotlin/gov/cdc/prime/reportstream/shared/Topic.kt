package gov.cdc.prime.reportstream.shared

import ca.uhn.hl7v2.llp.HL7Reader
import com.fasterxml.jackson.annotation.JsonValue
import gov.cdc.prime.reportstream.shared.queue_message.ITopic
import gov.cdc.prime.reportstream.shared.validation.AbstractItemValidator
import gov.cdc.prime.reportstream.shared.validation.IItemValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrOnboardingValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrValidator
import gov.cdc.prime.reportstream.shared.validation.NoopItemValidator



///**
// * A submission with topic FULL_ELR will be processed using the full ELR pipeline (fhir engine), submissions
// * from a sender with topic COVID_19 will be processed using the covid-19 pipeline.
// */
//enum class Topic(
//    @JsonValue val jsonVal: String,
//    val isUniversalPipeline: Boolean = true,
//    val isSendOriginal: Boolean = false,
//    val validator: IItemValidator = NoopItemValidator(),
//    val hl7ParseConfiguration: HL7Reader.Companion.HL7MessageParseAndConvertConfiguration? = null,
//) {
//    FULL_ELR("full-elr", true, false),
//    ETOR_TI("etor-ti", true, false),
//    ELR_ELIMS("elr-elims", true, true),
//    COVID_19("covid-19", false, false),
//    MONKEYPOX("monkeypox", false, false),
//    TEST("test", false, false),
//    MARS_OTC_ELR("mars-otc-elr", true, false, MarsOtcElrValidator()),
//    MARS_OTC_ELR_ONBOARDING("mars-otc-elr-onboarding", true, false, MarsOtcElrOnboardingValidator()),
//}



/**
 * A submission with topic FULL_ELR will be processed using the full ELR pipeline (fhir engine), submissions
 * from a sender with topic COVID_19 will be processed using the covid-19 pipeline.
 */
enum class Topic(
    @JsonValue val jsonVal: String,
    val isUniversalPipeline: Boolean = true,
    val isSendOriginal: Boolean = false,
    val validator: IItemValidator = NoopItemValidator(),
    val hl7ParseConfiguration: HL7MessageParseAndConvertConfiguration? = null,
) {
    FULL_ELR("full-elr", true, false),
    ETOR_TI("etor-ti", true, false),
    ELR_ELIMS("elr-elims", true, true),
    COVID_19("covid-19", false, false),
    MONKEYPOX("monkeypox", false, false),
    TEST("test", false, false),
    MARS_OTC_ELR("mars-otc-elr", true, false, MarsOtcElrValidator()),
    MARS_OTC_ELR_ONBOARDING("mars-otc-elr-onboarding", true, false, MarsOtcElrOnboardingValidator()),
}


//enum class Topic: ITopic {
//
//    FULL_ELR {
//        @JsonValue override fun jsonVal(): String { return "full-elr" }
//        override fun isUniversalPipeline(): Boolean { return true }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    ETOR_TI {
//        @JsonValue override fun jsonVal(): String { return "etor-ti" }
//        override fun isUniversalPipeline(): Boolean { return true }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    ELR_ELIMS {
//        @JsonValue override fun jsonVal(): String { return "elr-elims" }
//        override fun isUniversalPipeline(): Boolean { return true }
//        override fun isSendOriginal(): Boolean { return true }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    COVID_19 {
//        @JsonValue override fun jsonVal(): String { return "covid-19" }
//        override fun isUniversalPipeline(): Boolean { return false }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    MONKEYPOX {
//        @JsonValue override fun jsonVal(): String { return "monkeypox" }
//        override fun isUniversalPipeline(): Boolean { return false }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    TEST {
//        @JsonValue override fun jsonVal(): String { return "test" }
//        override fun isUniversalPipeline(): Boolean { return false }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return NoopItemValidator() }
//    },
//    MARS_OTC_ELR {
//        override fun jsonVal(): String { return "mars-otc-elr" }
//        override fun isUniversalPipeline(): Boolean { return true }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return MarsOtcElrValidator() }
//    },
//    MARS_OTC_ELR_ONBOARDING {
//        override fun jsonVal(): String { return "mars-otc-elr-onboarding" }
//        override fun isUniversalPipeline(): Boolean { return true }
//        override fun isSendOriginal(): Boolean { return false }
//        override fun validator(): AbstractItemValidator { return MarsOtcElrOnboardingValidator() }
//    }
// }

