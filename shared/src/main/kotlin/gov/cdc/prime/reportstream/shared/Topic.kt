package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonValue
import gov.cdc.prime.reportstream.shared.validation.IItemValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrOnboardingValidator
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrValidator
import gov.cdc.prime.reportstream.shared.validation.NoopItemValidator

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