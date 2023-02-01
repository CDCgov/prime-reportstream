package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
import java.util.UUID

class GlobalCustomFHIRFunctionsTests {
    @Test
    fun `test get function name enum`() {
        assertThat(GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.get(null)).isNull()
        assertThat(GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.get("someBadName")).isNull()
        val goodName = GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.GetId.name
        assertThat(GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.get(goodName)).isNotNull()
        val nameFormattedFromFhirPath = goodName.replaceFirstChar(Char::lowercase)
        assertThat(GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.get(nameFormattedFromFhirPath)).isNotNull()
    }

    @Test
    fun `test resolve function name`() {
        assertThat(GlobalCustomFHIRFunctions.resolveFunction(null)).isNull()
        assertThat(GlobalCustomFHIRFunctions.resolveFunction("someBadName")).isNull()
        val nameFormattedFromFhirPath = GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.GetId.name
            .replaceFirstChar(Char::lowercase)
        assertThat(GlobalCustomFHIRFunctions.resolveFunction(nameFormattedFromFhirPath)).isNotNull()

        GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.values().forEach {
            assertThat(GlobalCustomFHIRFunctions.resolveFunction(it.name)).isNotNull()
        }
    }

    @Test
    fun `test execute function`() {
        assertThat { GlobalCustomFHIRFunctions.executeFunction(null, "dummy", null) }.isFailure()

        val focus: MutableList<Base> = mutableListOf(StringType("data"))
        assertThat { GlobalCustomFHIRFunctions.executeFunction(focus, "dummy", null) }.isFailure()

        // Just checking we can access all the functions. Individual function results are tested on their own unit tests.
        GlobalCustomFHIRFunctions.CustomFHIRFunctionNames.values().forEach {
            assertThat { GlobalCustomFHIRFunctions.executeFunction(focus, it.name, null) }.isSuccess()
        }
    }

    @Test
    fun `test get ID function`() {
        assertThat(GlobalCustomFHIRFunctions.getId(mutableListOf())).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getId(mutableListOf(MessageHeader()))).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getId(mutableListOf(DateTimeType()))).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getId(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val oid = OidType().also { it.value = "AA" } // Bad OID
        var id = GlobalCustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)

        val goodOid = "1.2.3.4.5.6.7"
        oid.value = goodOid // Not a real OID as it needs to start with urn:oid:
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)

        oid.value = "urn:oid:$goodOid" // Now with URN
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        val oidInString = StringType().also { it.value = goodOid } // As a string no URN
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        oidInString.value = "urn:oid:$goodOid" // As a string with URN
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        // UUID
        val goodUuid = UUID.randomUUID().toString()
        val uuidInString = StringType().also { it.value = "urn:uuid:$goodUuid" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(uuidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodUuid)

        // DNS
        val goodDns = "someDns"
        val dnsInString = StringType().also { it.value = "urn:dns:$goodDns" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(dnsInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodDns)

        // URI
        val goodUri = "someUri"
        val uriInString = StringType().also { it.value = "urn:uri:$goodUri" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(uriInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodUri)

        // CLIA
        val goodClia = "10D0999999"
        val cliaInString = StringType().also { it.value = "urn:clia:$goodClia" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(cliaInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodClia)

        // Generic ID
        val goodId = "dummy"
        val idInString = StringType().also { it.value = "urn:id:$goodId" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(idInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodId)

        // None of the above. Format per HL7 v2 to FHIR mapping
        val idString = StringType().also { it.value = "name-type:$goodId" }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodId)

        // Generic IDs
        val someId = "someId"
        val genId = StringType().also { it.value = someId }
        id = GlobalCustomFHIRFunctions.getId(mutableListOf(genId))
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

        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf())).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(MessageHeader()))).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(DateTimeType()))).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val goodOid = "1.2.3.4.5.6.7"
        val oid = OidType().also { it.value = goodOid } // Not a real OID as it needs to start with urn:oid:
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(oid))).isEmpty()
        oid.value = "urn:oid:$goodOid"
        var id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        val oidInString = StringType().also { it.value = goodOid }
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(oidInString))).isEmpty()
        oidInString.value = "urn:oid:$goodOid"
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        // CLIA
        val realClia = "15D2112066"
        val cliaId = StringType().also { it.value = "urn:clia:$realClia" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = realClia
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = "D5D9458360" // DoD-style CLIA
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = "D5D945836K" // letter where it's not allowed
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(cliaId))).isEmpty()

        // DNS
        val dnsInString = StringType().also { it.value = "urn:dns:someDns" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(dnsInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(dnsType)

        // UUID
        val uuidInString = StringType().also { it.value = "urn:uuid:${UUID.randomUUID()}" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(uuidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(uuidType)

        // URI
        val uriInString = StringType().also { it.value = "urn:uri:${UUID.randomUUID()}" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(uriInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(uriType)

        // Generic ID
        val idInString = StringType().also { it.value = "urn:id:dummy" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(idInString))
        assertThat(id).isEmpty()

        // None of the above. Format per HL7 v2 to FHIR mapping
        val idString = StringType().also { it.value = "name-ISO:$goodOid" }
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oidType)

        idString.value = "name-CLIA:$realClia"
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        idString.value = "name-UKN:someId"
        id = GlobalCustomFHIRFunctions.getIdType(mutableListOf(idString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo("UKN")

        // Generic IDs don't have a type
        val someId = "someId"
        val genId = StringType().also { it.value = "urn:id:$someId" }
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()
        genId.value = someId
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()

        // Non ID types
        val badId = StringType().also { it.value = "dummy" }
        assertThat(GlobalCustomFHIRFunctions.getIdType(mutableListOf(badId))).isEmpty()
    }

    @Test
    fun `test split function`() {
        val stringToSplit = StringType().also { it.value = "part1,part2,part3" }
        val delimiter = StringType().also { it.value = "," }
        assertThat(GlobalCustomFHIRFunctions.split(mutableListOf(), null)).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.split(mutableListOf(stringToSplit), null)).isEmpty()
        assertThat(GlobalCustomFHIRFunctions.split(mutableListOf(stringToSplit), mutableListOf())).isEmpty()
        assertThat(
            GlobalCustomFHIRFunctions.split(
                mutableListOf(stringToSplit),
                mutableListOf(mutableListOf())
            )
        ).isEmpty()
        assertThat(
            GlobalCustomFHIRFunctions.split(
                mutableListOf(IntegerType()),
                mutableListOf(mutableListOf(delimiter))
            )
        ).isEmpty()
        assertThat(
            GlobalCustomFHIRFunctions.split(
                mutableListOf(stringToSplit),
                mutableListOf(mutableListOf(delimiter, delimiter))
            )
        ).isEmpty()

        val parts = GlobalCustomFHIRFunctions.split(
            mutableListOf(stringToSplit),
            mutableListOf(mutableListOf(delimiter))
        )
        assertThat(parts).isNotEmpty()
        assertThat(parts.size).isEqualTo(3)
    }
}