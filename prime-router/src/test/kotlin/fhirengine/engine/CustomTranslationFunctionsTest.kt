package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.USTimeZone
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomFHIRFunctions
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.Hl7TranslationFunctions
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.TranslationFunctions
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
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(DateTimeType("2015")))
            .isEqualTo("2015")
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04")))
            .isEqualTo("201504")
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04-05")))
            .isEqualTo("20150405")
        // Hour only or hour and minute only is not supported by FHIR type
        assertThat(
            Hl7TranslationFunctions().convertDateTimeToHL7(DateTimeType("2015-04-05T12:22:11"))
        )
            .isEqualTo("20150405122211")
        assertThat(
            Hl7TranslationFunctions()
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
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("2015")

        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04")),
                timezoneParameters
            )[0] as DateTimeType
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("201504")

        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05")),
                timezoneParameters
            )[0] as DateTimeType
        assertThat(Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)).isEqualTo("20150405")

        // Fhir doesn't support hour/minute precision
        // With seconds, we should start to see timezone
        adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters
            )[0] as DateTimeType
        val tmp = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp).isEqualTo("20150405212211+0900")
    }

    @org.junit.jupiter.api.Test
    fun `test MajuroTimezone for Marshall Island timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Majuro")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150406002211+1200")
    }

    @org.junit.jupiter.api.Test
    fun `test Puerto_RicoTimezone for Puerto Rico timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("America/Puerto_Rico")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405082211-0400")
    }

    @org.junit.jupiter.api.Test
    fun `test US_Virgin_IslandsTimezone for US Virgin Island timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("America/St_Thomas")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405082211-0400")
    }

    @org.junit.jupiter.api.Test
    fun `test ChuukTimezone for Micronesia timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Chuuk")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405222211+1000")
    }

    @org.junit.jupiter.api.Test
    fun `test PohnpeiTimezone for Micronesia timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Pohnpei")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405232211+1100")
    }

    @org.junit.jupiter.api.Test
    fun `test KosraeTimezone for Micronesia timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Kosrae")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405232211+1100")
    }

    @org.junit.jupiter.api.Test
    fun `test Northern_Mariana_IslandsTimezone for Northern Mariana Islands timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Saipan")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405222211+1000")
    }

    @org.junit.jupiter.api.Test
    fun `test PauluTimezone for Palau timezone with convertDateTimeToHL7`() {
        val timezoneParameters1: MutableList<MutableList<Base>> =
            mutableListOf(mutableListOf(StringType("Pacific/Palau")))
        val adjustedDateTime =
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType("2015-04-05T12:22:11Z")),
                timezoneParameters1
            )[0] as DateTimeType
        val tmp1 = Hl7TranslationFunctions().convertDateTimeToHL7(adjustedDateTime)
        assertThat(tmp1).isEqualTo("20150405212211+0900")
    }

    @org.junit.jupiter.api.Test
    fun `test convertDateTimeToHL7 with CustomContext with receiver setting`() {
        val receiver = mockkClass(Receiver::class, relaxed = true)
        val appContext = mockkClass(CustomContext::class)
        val config = UnitTestUtils.createConfig(
            useHighPrecisionHeaderDateTimeFormat = true,
            convertPositiveDateTimeOffsetToNegative = false,
            convertDateTimesToReceiverLocalTime = true,
            convertTimestampToDateTime = "MSH-8, SPM-18"
        )
        every { appContext.customFhirFunctions }.returns(CustomFhirPathFunctions())
        every { appContext.config }.returns(HL7TranslationConfig(config, receiver))
        every { receiver.dateTimeFormat }.returns(null)
        every { receiver.translation }.returns(config)
        every { receiver.timeZone } returns (USTimeZone.UTC)
        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(
                    DateTimeType("2023-07-21T10:30:17.328-07:00"), appContext
                )
        ).isEqualTo("20230721173017.0000+0000")
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
        ).isEqualTo("20150411162201.0000+0000")

        assertThat(
            CustomTranslationFunctions()
                .convertDateTimeToHL7(
                    DateTimeType("2015-04-11T12:22:01-04:00"),
                    appContext,
                    ConverterSchemaElement(hl7Spec = listOf("MSH-8")),
                    ConstantSubstitutor()
                )
        ).isEqualTo("20150411162201")

        assertThat(
            CustomTranslationFunctions()
                .getDateTimeFormat(
                    "MSH-8, SPM-18",
                    ConverterSchemaElement(hl7Spec = listOf("MSH-8")),
                    ConstantSubstitutor(),
                    appContext,
                    receiver.dateTimeFormat
                )
        ).isEqualTo(DateUtilities.DateTimeFormat.LOCAL)

        assertThat(
            CustomTranslationFunctions()
                .getDateTimeFormat(
                    "MSH-8, SPM-18",
                    ConverterSchemaElement(hl7Spec = listOf("MSH-9")),
                    ConstantSubstitutor(),
                    appContext,
                    receiver.dateTimeFormat
                )
        ).isEqualTo(receiver.dateTimeFormat)
    }

    @Test
    fun `test HL7 Truncation`() {
        val translationFunctions: TranslationFunctions = CustomTranslationFunctions()
        val emptyTerser = Terser(ORU_R01())
        val customContext = UnitTestUtils.createCustomContext(
            config = HL7TranslationConfig(
                hl7Configuration = UnitTestUtils.createConfig(
                    truncateHDNamespaceIds = true,
                    truncateHl7Fields = "MSH-4-1, ORC-12-1, OBX-15-2, SPM-2-2-2",
                ),
                null
            )
        )

        val msh4InputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ T",
        )

        val orc12InputAndExpected = mapOf(
            "short" to "short",
            "Test value test value" to "Test value test",
        )

        val obx15InputAndExpected = mapOf(
            "short" to "short",
            "Test value test value longer Test value test value longer Test value test value longer" +
                "Test value test value longer Test value test value longer Test value test value longer" +
                "Test value test value Test TRUNCATE THIS"
            to
            "Test value test value longer Test value test value longer Test value test value longer" +
                "Test value test value longer Test value test value longer Test value test value longer" +
                "Test value test value Test"
        )

        val spm2InputAndExpected = mapOf(
            "short" to "short",
            "Test value test testTRUNCATE" to "Test value test test",
        )

        msh4InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT/PATIENT/MSH-4-1",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        orc12InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT(0)/ORDER_OBSERVATION(0)/ORC-12(0)-1",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        orc12InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT/ORDER_OBSERVATION/ORC-12-1",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        obx15InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT(0)/ORDER_OBSERVATION(0)/OBSERVATION(0)/OBX-15(0)-2",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        obx15InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT(0)/ORDER_OBSERVATION(0)/OBSERVATION(0)/OBX-15-2",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        spm2InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT(0)/ORDER_OBSERVATION(0)/SPECIMEN/SPM-2-2-2",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test HL7 Passthrough`() {
        val translationFunctions: TranslationFunctions = CustomTranslationFunctions()
        val emptyTerser = Terser(ORU_R01())
        val customContext = UnitTestUtils.createCustomContext(
            config = HL7TranslationConfig(
                hl7Configuration = UnitTestUtils.createConfig(),
                null
            )
        )

        val inputAndExpected = mapOf(
            "short" to "short",
            "Test & Value ~ Text ^ String" to "Test & Value ~ Text ^ String",
        )

        val obr16InputAndExpected = mapOf(
            "short" to "short",
            "Test value test value do not truncate" to "Test value test value do not truncate",
        )

        inputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT/PATIENT/MSH-4-1",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }

        obr16InputAndExpected.forEach { (input, expected) ->
            val actual = translationFunctions.maybeTruncateHL7Field(
                input,
                "/PATIENT_RESULT(0)/ORDER_OBSERVATION(0)/OBR-16(0)-1",
                emptyTerser,
                customContext
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}