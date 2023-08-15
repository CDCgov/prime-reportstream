package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomFHIRFunctions
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkClass
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.StringType
import kotlin.test.Test

class CustomTranslationFunctionsTest {

    @Test
    fun `test convertDateTimeToHL7`() {
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(DateTimeType("2015")))
            .isEqualTo("2015")
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04")))
            .isEqualTo("201504")
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04-05")))
            .isEqualTo("20150405")
        // Hour only or hour and minute only is not supported by FHIR type
        assertThat(
            CustomTranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11"))
        )
            .isEqualTo("20150405122211")
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(DateTimeType("2015-04-11T12:22:01-04:00"))
        )
            .isEqualTo("20150411122201-0400")
    }

    @org.junit.jupiter.api.Test
    fun `test changeTimezone with convertDateTimeToHL7`() {
        val timezoneParameters: MutableList<MutableList<Base>> = mutableListOf(mutableListOf(StringType("Asia/Tokyo")))
        var adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015")),
                timezoneParameters
            )[0] as DateTimeType
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("2015")

        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType("2015-04")),
            timezoneParameters
        )[0] as DateTimeType
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("201504")

        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType("2015-04-05")),
            timezoneParameters
        )[0] as DateTimeType
        assertThat(CustomTranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("20150405")

        // Fhir doesn't support hour/minute precision
        // With seconds, we should start to see timezone
        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
            timezoneParameters
        )[0] as DateTimeType
        val tmp = CustomTranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp).isEqualTo("20150405212211+0900")
    }

    @org.junit.jupiter.api.Test
    fun `test convertDateTimeToHL7 with CustomContext with receiver setting`() {
        val receiver = mockkClass(Receiver::class)
        val appContext = mockkClass(CustomContext::class)
        every { appContext.customFhirFunctions }.returns(CustomFhirPathFunctions())
        every { appContext.config }.returns(receiver)
        every { receiver.dateTimeFormat }.returns(null)
        every { receiver.translation }.returns(
            UnitTestUtils.createConfig(
                useHighPrecisionHeaderDateTimeFormat = true,
                convertPositiveDateTimeOffsetToNegative = false
            )
        )
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(
                    DateTimeType("2023-07-21T10:30:17.328-07:00"), appContext
                )
        ).isEqualTo("20230721103017.0000-0700")
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(
                    DateTimeType("2015-04-05T12:22:11.567Z"), appContext
                )
        ).isEqualTo("20150405122211.5670+0000")
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11.567891Z"), appContext)
        ).isEqualTo("20150405122211.5678+0000")
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(DateTimeType("2015-04-11T12:22:01-04:00"), appContext)
        ).isEqualTo("20150411122201.0000-0400")
    }
}