package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.doesNotHaveClass
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.UUID

class CustomFHIRFunctionsTests {

    @Test
    fun `test get function name enum`() {
        assertThat(CustomFHIRFunctions.CustomFHIRFunctionNames.get(null)).isNull()
        assertThat(CustomFHIRFunctions.CustomFHIRFunctionNames.get("someBadName")).isNull()
        val goodName = CustomFHIRFunctions.CustomFHIRFunctionNames.GetId.name
        assertThat(CustomFHIRFunctions.CustomFHIRFunctionNames.get(goodName)).isNotNull()
        val nameFormattedFromFhirPath = goodName.replaceFirstChar(Char::lowercase)
        assertThat(CustomFHIRFunctions.CustomFHIRFunctionNames.get(nameFormattedFromFhirPath)).isNotNull()
    }

    @Test
    fun `test resolve function name`() {
        assertThat(CustomFHIRFunctions.resolveFunction(null)).isNull()
        assertThat(
            CustomFHIRFunctions
                .resolveFunction("someBadName")
        ).isNull()
        val nameFormattedFromFhirPath = CustomFHIRFunctions.CustomFHIRFunctionNames.GetId.name
            .replaceFirstChar(Char::lowercase)
        assertThat(
            CustomFHIRFunctions
                .resolveFunction(nameFormattedFromFhirPath)
        ).isNotNull()

        CustomFHIRFunctions.CustomFHIRFunctionNames.values().forEach {
            assertThat(CustomFHIRFunctions.resolveFunction(it.name)).isNotNull()
        }
    }

    @Test
    fun `test execute function`() {
        assertFailure {
            CustomFHIRFunctions
                .executeFunction(null, "dummy", null)
        }

        val focus: MutableList<Base> = mutableListOf(StringType("data"))
        assertFailure {
            CustomFHIRFunctions
                .executeFunction(focus, "dummy", null)
        }

        // Just checking we can access all the functions.
        // Individual function results are tested on their own unit tests.
        CustomFHIRFunctions.CustomFHIRFunctionNames.values().forEach {
            if (it == CustomFHIRFunctions.CustomFHIRFunctionNames.ChangeTimezone) {
                // With bad inputs this will cause an error, but still verifies access to the function
                assertFailure {
                    CustomFHIRFunctions
                        .executeFunction(focus, it.name, null)
                }.doesNotHaveClass(IllegalStateException::class.java)
            }
        }
    }

    @Test
    fun `test get ID function`() {
        assertThat(CustomFHIRFunctions.getId(mutableListOf())).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(MessageHeader()))).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(DateTimeType()))).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val oid = OidType().also { it.value = "AA" } // Bad OID
        var id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)

        val goodOid = "1.2.3.4.5.6.7"
        oid.value = goodOid // Not a real OID as it needs to start with urn:oid:
        id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)

        oid.value = "urn:oid:$goodOid" // Now with URN
        id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        val oidInString = StringType().also { it.value = goodOid } // As a string no URN
        id = CustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        oidInString.value = "urn:oid:$goodOid" // As a string with URN
        id = CustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        // UUID
        val goodUuid = UUID.randomUUID().toString()
        val uuidInString = StringType().also { it.value = "urn:uuid:$goodUuid" }
        id = CustomFHIRFunctions.getId(mutableListOf(uuidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodUuid)

        // DNS
        val goodDns = "someDns"
        val dnsInString = StringType().also { it.value = "urn:dns:$goodDns" }
        id = CustomFHIRFunctions.getId(mutableListOf(dnsInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodDns)

        // URI
        val goodUri = "someUri"
        val uriInString = StringType().also { it.value = "urn:uri:$goodUri" }
        id = CustomFHIRFunctions.getId(mutableListOf(uriInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodUri)

        // CLIA
        val goodClia = "10D0999999"
        val cliaInString = StringType().also { it.value = "urn:clia:$goodClia" }
        id = CustomFHIRFunctions.getId(mutableListOf(cliaInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodClia)

        // Generic ID
        val goodId = "dummy"
        val idInString = StringType().also { it.value = "urn:id:$goodId" }
        id = CustomFHIRFunctions.getId(mutableListOf(idInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodId)

        // None of the above. Format per HL7 v2 to FHIR mapping
        val idString = StringType().also { it.value = "name-type:$goodId" }
        id = CustomFHIRFunctions.getId(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodId)

        // Generic IDs
        val someId = "someId"
        val genId = StringType().also { it.value = someId }
        id = CustomFHIRFunctions.getId(mutableListOf(genId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(someId)
    }

    @Test
    fun `test get id type function`() {
        val oidType = "ISO"
        val cliaType = "CLIA"
        val dnsType = "DNS"
        val uuidType = "UUID"
        val uriType = "URI"

        assertThat(CustomFHIRFunctions.getIdType(mutableListOf())).isEmpty()
        assertThat(
            CustomFHIRFunctions
                .getIdType(mutableListOf(MessageHeader()))
        ).isEmpty()
        assertThat(
            CustomFHIRFunctions
                .getIdType(mutableListOf(DateTimeType()))
        ).isEmpty()
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val goodOid = "1.2.3.4.5.6.7"
        val oid = OidType().also { it.value = goodOid } // Not a real OID as it needs to start with urn:oid:
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(oid))).isEmpty()
        oid.value = "urn:oid:$goodOid"
        var id = CustomFHIRFunctions.getIdType(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        val oidInString = StringType().also { it.value = goodOid }
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(oidInString))).isEmpty()
        oidInString.value = "urn:oid:$goodOid"
        id = CustomFHIRFunctions.getIdType(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        // CLIA
        val realClia = "15D2112066"
        val cliaId = StringType().also { it.value = "urn:clia:$realClia" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = realClia
        id = CustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = "D5D9458360" // DoD-style CLIA
        id = CustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = "D5D945836K" // letter where it's not allowed
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(cliaId))).isEmpty()

        // DNS
        val dnsInString = StringType().also { it.value = "urn:dns:someDns" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(dnsInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(dnsType)

        // UUID
        val uuidInString = StringType().also { it.value = "urn:uuid:${UUID.randomUUID()}" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(uuidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(uuidType)

        // URI
        val uriInString = StringType().also { it.value = "urn:uri:${UUID.randomUUID()}" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(uriInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(uriType)

        // Generic ID
        val idInString = StringType().also { it.value = "urn:id:dummy" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(idInString))
        assertThat(id).isEmpty()

        // None of the above. Format per HL7 v2 to FHIR mapping
        val idString = StringType().also { it.value = "name-ISO:$goodOid" }
        id = CustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        idString.value = "name-CLIA:$realClia"
        id = CustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        idString.value = "name-UKN:someId"
        id = CustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo("UKN")

        // Generic IDs don't have a type
        val someId = "someId"
        val genId = StringType().also { it.value = "urn:id:$someId" }
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()
        genId.value = someId
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()

        // Non ID types
        val badId = StringType().also { it.value = "dummy" }
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(badId))).isEmpty()
    }

    @Test
    fun `test split function`() {
        val stringToSplit = StringType().also { it.value = "part1,part2,part3" }
        val delimiter = StringType().also { it.value = "," }
        assertThat(CustomFHIRFunctions.split(mutableListOf(), null)).isEmpty()
        assertThat(
            CustomFHIRFunctions
                .split(mutableListOf(stringToSplit), null)
        ).isEmpty()
        assertThat(
            CustomFHIRFunctions
                .split(mutableListOf(stringToSplit), mutableListOf())
        ).isEmpty()
        assertThat(
            CustomFHIRFunctions.split(
                mutableListOf(stringToSplit),
                mutableListOf(mutableListOf())
            )
        ).isEmpty()
        assertThat(
            CustomFHIRFunctions.split(
                mutableListOf(IntegerType()),
                mutableListOf(mutableListOf(delimiter))
            )
        ).isEmpty()
        assertThat(
            CustomFHIRFunctions.split(
                mutableListOf(stringToSplit),
                mutableListOf(mutableListOf(delimiter, delimiter))
            )
        ).isEmpty()

        val parts = CustomFHIRFunctions.split(
            mutableListOf(stringToSplit),
            mutableListOf(mutableListOf(delimiter))
        )
        assertThat(parts).isNotEmpty()
        assertThat(parts.size).isEqualTo(3)
    }

    @Test
    fun `test changeTimezone`() {
        // need to choose place without daylight savings time so that the test is not brittle
        val azt = StringType("America/Phoenix")

        val aztDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType("2021-08-09T08:52:34.567-04:00")),
            mutableListOf(mutableListOf(azt))
        )
        assertThat(aztDate[0]).isInstanceOf(DateTimeType::class.java)
        assertThat(aztDate[0].primitiveValue()).isEqualTo("2021-08-09T05:52:34.567-07:00")

        // Japan also doesn't have daylight savings time and is an example of a positive time change
        val jst = StringType("Asia/Tokyo")
        val jstDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType("2021-08-09T08:52:34.567-04:00")),
            mutableListOf(mutableListOf(jst))
        )
        assertThat(jstDate[0].primitiveValue()).isEqualTo("2021-08-09T21:52:34.567+09:00")

        // Verify the output can be adjusted again
        val aztDate2 = CustomFHIRFunctions.changeTimezone(jstDate, mutableListOf(mutableListOf(azt)))
        assertThat(aztDate2[0].primitiveValue()).isEqualTo("2021-08-09T05:52:34.567-07:00")
    }

    @Test
    fun `test changeTimezone with different precisions`() {
        // need to choose place without daylight savings time so that the test is not brittle
        val azt = StringType("America/Phoenix")
        val jst = StringType("Asia/Tokyo")
        val dateMilli = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("2021-08-09T08:52:34.567-04:00")
        val dateSecond = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2021-08-09T08:52:34-04:00")
        val dateDay = SimpleDateFormat("yyyy-MM-dd").parse("2021-08-09")
        val dateMonth = SimpleDateFormat("yyyy-MM").parse("2021-08")
        val dateYear = SimpleDateFormat("yyyy").parse("2021")

        val aztDateInstant = CustomFHIRFunctions.changeTimezone(
            mutableListOf(InstantType(dateMilli)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateInstant?.value).isEqualTo(dateMilli)
        assertThat(aztDateInstant?.precision).isEqualTo(TemporalPrecisionEnum.MILLI)
        assertThat(aztDateInstant?.primitiveValue()).isEqualTo("2021-08-09T05:52:34.567-07:00")
        assertThat(aztDateInstant?.timeZone).isEqualTo(TimeZone.getTimeZone(azt.value))

        val aztDateMilli = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateMilli, TemporalPrecisionEnum.MILLI)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateMilli?.value).isEqualTo(dateMilli)
        assertThat(aztDateMilli?.precision).isEqualTo(TemporalPrecisionEnum.MILLI)
        assertThat(aztDateMilli?.primitiveValue()).isEqualTo("2021-08-09T05:52:34.567-07:00")
        assertThat(aztDateMilli?.timeZone).isEqualTo(TimeZone.getTimeZone(azt.value))

        val aztDateSecond = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateSecond, TemporalPrecisionEnum.SECOND)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateSecond?.value).isEqualTo(dateSecond)
        assertThat(aztDateSecond?.precision).isEqualTo(TemporalPrecisionEnum.SECOND)
        assertThat(aztDateSecond?.primitiveValue()).isEqualTo("2021-08-09T05:52:34-07:00")
        assertThat(aztDateSecond?.timeZone).isEqualTo(TimeZone.getTimeZone(azt.value))

        val aztDateDay = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateDay, TemporalPrecisionEnum.DAY)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateDay?.value).isEqualTo(dateDay)
        assertThat(aztDateDay?.precision).isEqualTo(TemporalPrecisionEnum.DAY)
        assertThat(aztDateDay?.primitiveValue()).isEqualTo("2021-08-09")

        val aztDateMonth = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateMonth, TemporalPrecisionEnum.MONTH)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateMonth?.value).isEqualTo(dateMonth)
        assertThat(aztDateMonth?.precision).isEqualTo(TemporalPrecisionEnum.MONTH)
        assertThat(aztDateMonth?.primitiveValue()).isEqualTo("2021-08")

        val aztDateYear = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateYear, TemporalPrecisionEnum.YEAR)),
            mutableListOf(mutableListOf(azt))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(aztDateYear?.value).isEqualTo(dateYear)
        assertThat(aztDateYear?.precision).isEqualTo(TemporalPrecisionEnum.YEAR)
        assertThat(aztDateYear?.primitiveValue()).isEqualTo("2021")

        // Japan also doesn't have daylight savings time and is an example of a positive time change
        val jstDateInstant = CustomFHIRFunctions.changeTimezone(
            mutableListOf(InstantType(dateMilli)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateInstant?.value).isEqualTo(dateMilli)
        assertThat(jstDateInstant?.precision).isEqualTo(TemporalPrecisionEnum.MILLI)
        assertThat(jstDateInstant?.primitiveValue()).isEqualTo("2021-08-09T21:52:34.567+09:00")
        assertThat(jstDateInstant?.timeZone).isEqualTo(TimeZone.getTimeZone(jst.value))

        val jstDateMilli = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateMilli, TemporalPrecisionEnum.MILLI)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateMilli?.value).isEqualTo(dateMilli)
        assertThat(jstDateMilli?.precision).isEqualTo(TemporalPrecisionEnum.MILLI)
        assertThat(jstDateMilli?.primitiveValue()).isEqualTo("2021-08-09T21:52:34.567+09:00")
        assertThat(jstDateMilli?.timeZone).isEqualTo(TimeZone.getTimeZone(jst.value))

        val jstDateSecond = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateSecond, TemporalPrecisionEnum.SECOND)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateSecond?.value).isEqualTo(dateSecond)
        assertThat(jstDateSecond?.precision).isEqualTo(TemporalPrecisionEnum.SECOND)
        assertThat(jstDateSecond?.primitiveValue()).isEqualTo("2021-08-09T21:52:34+09:00")
        assertThat(jstDateSecond?.timeZone).isEqualTo(TimeZone.getTimeZone(jst.value))

        val jstDateDay = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateDay, TemporalPrecisionEnum.DAY)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateDay?.value).isEqualTo(dateDay)
        assertThat(jstDateDay?.precision).isEqualTo(TemporalPrecisionEnum.DAY)
        assertThat(jstDateDay?.primitiveValue()).isEqualTo("2021-08-09")

        val jstDateMonth = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateMonth, TemporalPrecisionEnum.MONTH)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateMonth?.value).isEqualTo(dateMonth)
        assertThat(jstDateMonth?.precision).isEqualTo(TemporalPrecisionEnum.MONTH)
        assertThat(jstDateMonth?.primitiveValue()).isEqualTo("2021-08")

        val jstDateYear = CustomFHIRFunctions.changeTimezone(
            mutableListOf(DateTimeType(dateYear, TemporalPrecisionEnum.YEAR)),
            mutableListOf(mutableListOf(jst))
        ).getOrNull(0) as? BaseDateTimeType
        assertThat(jstDateYear?.value).isEqualTo(dateYear)
        assertThat(jstDateYear?.precision).isEqualTo(TemporalPrecisionEnum.YEAR)
        assertThat(jstDateYear?.primitiveValue()).isEqualTo("2021")
    }

    @Test
    fun `test changeTimezone invalid inputs`() {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        val date: Date = isoFormat.parse("2021-08-09T08:52:00-04:00")
        val timezoneParameters: MutableList<MutableList<Base>> = mutableListOf(mutableListOf(StringType("Asia/Tokyo")))

        assertFailure {
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType(date), DateTimeType(date)),
                timezoneParameters
            )
        }

        assertFailure {
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(TimeType("12:34:56")),
                timezoneParameters
            )
        }

        assertFailure {
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType(date)),
                null
            )
        }
    }

    @Test
    fun `test changeTimezone invalid timezone`() {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        val date: Date = isoFormat.parse("2021-08-09T08:52:00-04:00")

        val pst = StringType().also { it.value = "America/Blah" }
        assertFailure {
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType(date)),
                mutableListOf(mutableListOf(pst))
            )
        }.hasClass(SchemaException::class.java)

        // Different exception for single character, confirm still gets converted to schema exception
        pst.value = "A"
        assertFailure {
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(DateTimeType(date)),
                mutableListOf(mutableListOf(pst))
            )
        }.hasClass(SchemaException::class.java)
    }

    @Test
    fun `test changeTimezone with date as string - success`() {
        val date = StringType("20241220194528.4230+0000")
        val timezone = StringType("UTC")
        val dateTimeFormat = StringType("HIGH_PRECISION_OFFSET")
        val convertToNegative = StringType("true")
        val useHighPrecision = StringType("false")

        // test format
        var outputDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(date),
            mutableListOf(
                mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                mutableListOf(useHighPrecision)
            )
        )
        assertThat(outputDate[0]).isInstanceOf(StringType::class.java)
        assertThat(outputDate[0].primitiveValue()).isEqualTo("20241220194528.4230-0000")

        // test timezone change with offset format
        timezone.value = "America/Phoenix"
        dateTimeFormat.value = "OFFSET"
        convertToNegative.value = "false"
        useHighPrecision.value = "false"

        outputDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(date),
            mutableListOf(
                mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                mutableListOf(useHighPrecision)
            )
        )
        assertThat(outputDate[0]).isInstanceOf(StringType::class.java)
        assertThat(outputDate[0].primitiveValue()).isEqualTo("20241220124528-0700")

        // test different format for input date
        val date2 = StringType("2021-08-09T08:52:34-04:00")
        timezone.value = "America/Phoenix"
        dateTimeFormat.value = "LOCAL"
        convertToNegative.value = "false"
        useHighPrecision.value = "false"

        outputDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(date2),
            mutableListOf(
                mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                mutableListOf(useHighPrecision)
            )
        )
        assertThat(outputDate[0]).isInstanceOf(StringType::class.java)
        assertThat(outputDate[0].primitiveValue()).isEqualTo("20210809055234")

        // test date without time should return same date string
        val date3 = StringType("2021-08-09")
        timezone.value = "America/Phoenix"
        dateTimeFormat.value = "OFFSET"
        convertToNegative.value = "false"
        useHighPrecision.value = "false"

        outputDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(date3),
            mutableListOf(
                mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                mutableListOf(useHighPrecision)
            )
        )
        assertThat(outputDate[0]).isInstanceOf(StringType::class.java)
        assertThat(outputDate[0].primitiveValue()).isEqualTo("2021-08-09")

        // test timezone change with required param only
        timezone.value = "America/Phoenix"

        outputDate = CustomFHIRFunctions.changeTimezone(
            mutableListOf(date),
            mutableListOf(mutableListOf(timezone))
        )
        assertThat(outputDate[0]).isInstanceOf(StringType::class.java)
        assertThat(outputDate[0].primitiveValue()).isEqualTo("20241220124528-0700")
    }

    @Test
    fun `test changeTimezone with date as string - exception`() {
        val date = StringType("20241220194528.4230+0000")
        val timezone = StringType("UTC")
        val dateTimeFormat = StringType("HIGH_PRECISION_OFFSET")
        val convertToNegative = StringType("true")
        val useHighPrecision = StringType("false")

        assertFailure {
            dateTimeFormat.value = "HIGH_PRECISION_OFFS"
            // test invalid dateTime format
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(date),
                mutableListOf(
                    mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                    mutableListOf(useHighPrecision)
                )
            )
        }.hasClass(SchemaException::class.java)

        assertFailure {
            dateTimeFormat.value = "HIGH_PRECISION_OFFSET"
            date.value = "2021-08-09T"
            // test invalid dateTime string input
            CustomFHIRFunctions.changeTimezone(
                mutableListOf(date),
                mutableListOf(
                    mutableListOf(timezone), mutableListOf(dateTimeFormat), mutableListOf(convertToNegative),
                    mutableListOf(useHighPrecision)
                )
            )
        }.hasClass(SchemaException::class.java)
    }

    @Test
    fun `test deidentifies a human name`() {
        val name = HumanName()
        name.given = mutableListOf(StringType("foo"), StringType("bar"))
        name.family = "family"

        CustomFHIRFunctions.deidentifyHumanName(mutableListOf(name), mutableListOf())

        assertThat(name.given).isEmpty()
        assertThat(name.family).isNull()

        val name2 = HumanName()
        name2.given = mutableListOf(StringType("foo"), StringType("bar"))
        name2.family = "family"

        CustomFHIRFunctions.deidentifyHumanName(
            mutableListOf(name2),
            mutableListOf(mutableListOf(StringType("baz")))
        )

        assertThat(name2.given).transform { it -> it.map { st -> st.value } }
            .containsOnly("baz", "baz")
        assertThat(name2.family).isEqualTo("baz")
    }
}