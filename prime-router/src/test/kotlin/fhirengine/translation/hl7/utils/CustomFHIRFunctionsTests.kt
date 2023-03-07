package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import gov.cdc.prime.router.unittest.UnitTestUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
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
        assertThat {
            CustomFHIRFunctions
                .executeFunction(null, "dummy", null)
        }.isFailure()

        val focus: MutableList<Base> = mutableListOf(StringType("data"))
        assertThat {
            CustomFHIRFunctions
                .executeFunction(focus, "dummy", null)
        }.isFailure()

        // Just checking we can access all the functions.
        // Individual function results are tested on their own unit tests.
        CustomFHIRFunctions.CustomFHIRFunctionNames.values().forEach {
            // todo: this is temporary until this code is moved
            if (it != CustomFHIRFunctions.CustomFHIRFunctionNames.LivdTableLookup) {
                assertThat {
                    CustomFHIRFunctions
                        .executeFunction(focus, it.name, null)
                }.isSuccess()
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
    fun `test livdTableLookup is Observation`() {
        assertThat(
            CustomFHIRFunctions.livdTableLookup(
                mutableListOf(Observation()), mutableListOf(), UnitTestUtils.simpleMetadata
            ) == mutableListOf(StringType(null))
        )
    }

    @Test
    fun `test livdTableLookup is not Observation`() {
        assertThat {
            CustomFHIRFunctions.livdTableLookup(
                mutableListOf(Device()), mutableListOf(), UnitTestUtils.simpleMetadata
            )
        }.isFailure()
    }
}