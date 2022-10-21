package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
import java.util.UUID

class ConstantResolverTests {
    @Test
    fun `test custom context`() {
        val previousContext = CustomContext(Bundle(), Bundle())
        assertThat(CustomContext.addConstants(mapOf(), previousContext)).isNotNull()

        val constantValue = "value1"
        val constant = sortedMapOf("name1" to "'$constantValue'")
        var context = CustomContext.addConstant(constant.firstKey(), constant[constant.firstKey()]!!, previousContext)
        assertThat(context).isNotNull()
        assertThat(context.constants).isNotEmpty()
        assertThat(context.constants[constant.firstKey()]).isEqualTo(constant[constant.firstKey()])

        context = CustomContext.addConstants(constant, previousContext)
        assertThat(context).isNotNull()
        assertThat(context.constants).isNotEmpty()
        assertThat(context.constants[constant.firstKey()]).isEqualTo(constant[constant.firstKey()])

        // Check that a new context is returned
        var context2 = CustomContext.addConstant("anotherconst", "'value'", context)
        assertThat(context2).isNotNull()
        assertThat(context2).isNotSameAs(context) // Test the reference is different

        context2 = CustomContext.addConstants(mapOf(), context)
        assertThat(context2).isNotNull()
        assertThat(context2).isSameAs(context)
    }

    @Test
    fun `test constant substitutortortortortortor - funny name`() {
        val constant = sortedMapOf("const1" to "value1")
        val context = CustomContext.addConstants(constant, CustomContext(Bundle(), Bundle()))
        val resolver = ConstantSubstitutor()

        var inputString = "Lorem ipsum %{const1} sit amet, consectetur adipiscing"
        val expectedString = "Lorem ipsum value1 sit amet, consectetur adipiscing"
        val result = resolver.replace(inputString, context)
        assertThat(result).isEqualTo(expectedString)

        inputString = "Lorem ipsum %{const2} sit amet, consectetur adipiscing"
        assertThat { resolver.replace(inputString, context) }.isFailure()
        assertThat { resolver.replace(inputString, null) }.isFailure()
    }

    @Test
    fun `test fhir path resolver`() {
        assertThat { FhirPathCustomResolver().resolveConstant(null, null, false) }.isFailure()
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const1", false) }
            .isFailure().hasClass(PathEngineException::class.java)
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const1", false) }
            .isFailure().hasClass(PathEngineException::class.java)

        val integerValue = 99
        val urlPrefix = "https://reportstream.cdc.gov/fhir/StructureDefinition/"
        val constants = sortedMapOf("const1" to "'value1'", "int1" to "'$integerValue'", "rsext" to "'$urlPrefix'")
        val context = CustomContext.addConstants(constants, CustomContext(Bundle(), Bundle()))
        assertThat(FhirPathCustomResolver().resolveConstant(context, "const2", false)).isNull()
        assertThat(FhirPathCustomResolver().resolveConstant(context, "const1", false)).isNotNull()
        var result = FhirPathCustomResolver().resolveConstant(context, "int1", false)
        assertThat(result).isNotNull()
        assertThat(result is IntegerType).isTrue()
        assertThat((result as IntegerType).value).isEqualTo(integerValue)

        // Now lets resolve a constant
        result = FhirPathCustomResolver().resolveConstant(context, "const1", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isPrimitive).isTrue()
        assertThat(result).isInstanceOf(StringType::class.java)
        assertThat((result as StringType).value).isEqualTo(
            constants[constants.firstKey()]!!.replace("'", "")
        )

        // Text the ability to resolve constants with suffix
        val urlSuffix = "SomeSuffix"
        result = FhirPathCustomResolver().resolveConstant(context, "`rsext-$urlSuffix`", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isPrimitive).isTrue()
        assertThat(result).isInstanceOf(StringType::class.java)
        assertThat((result as StringType).value).isEqualTo("$urlPrefix$urlSuffix")

        result = FhirPathCustomResolver().resolveConstant(context, "`rsext`", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isPrimitive).isTrue()
        assertThat(result).isInstanceOf(StringType::class.java)
        assertThat((result as StringType).value).isEqualTo(urlPrefix)

        result = FhirPathCustomResolver().resolveConstant(context, "unknownconst", false)
        assertThat(result).isNull()
    }

    @Test
    fun `test fhir reference resolver`() {
        val org1 = Organization()
        org1.id = UUID.randomUUID().toString()
        val org2 = Organization()
        org2.id = UUID.randomUUID().toString()
        val org2Url = "Organization/${org2.id}"

        val bundle = Bundle()
        val customContext = CustomContext(bundle, bundle)
        assertThat(FhirPathCustomResolver().resolveReference(customContext, org2Url)).isNull()

        bundle.addEntry().resource = org1
        bundle.entry[0].fullUrl = "Organization/${org1.id}"
        assertThat(FhirPathCustomResolver().resolveReference(customContext, org2Url)).isNull()

        bundle.addEntry().resource = org2
        bundle.entry[1].fullUrl = org2Url
        val reference = FhirPathCustomResolver().resolveReference(customContext, org2Url)
        assertThat(reference).isNotNull()
        assertThat(reference).isEqualTo(org2)
    }

    @Test
    fun `test get ID function`() {
        assertThat(CustomFHIRFunctions.getId(mutableListOf())).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(MessageHeader()))).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(DateTimeType()))).isEmpty()
        assertThat(CustomFHIRFunctions.getId(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val goodOid = "1.2.3.4.5.6.7"
        val oid = OidType()
        oid.value = "AA" // Some non OID
        var id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)
        oid.value = goodOid // Not a real OID as it needs to start with urn:oid:
        id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(oid.value)
        oid.value = "urn:oid:$goodOid"
        id = CustomFHIRFunctions.getId(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        val oidInString = StringType()
        oidInString.value = goodOid
        id = CustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        oidInString.value = "urn:oid:$goodOid"
        id = CustomFHIRFunctions.getId(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(goodOid)

        // Generic IDs
        val someId = "someId"
        val genId = StringType()
        genId.value = "urn:id:$someId"
        id = CustomFHIRFunctions.getId(mutableListOf(genId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(someId)

        genId.value = someId
        id = CustomFHIRFunctions.getId(mutableListOf(genId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(genId.value)

        // CLIA IDs
        val cliaId = StringType()
        cliaId.value = "dummy"
        id = CustomFHIRFunctions.getId(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaId.value)

        val realClia = "15D2112066"
        cliaId.value = "urn:id:$realClia"
        id = CustomFHIRFunctions.getId(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(realClia)

        cliaId.value = realClia
        id = CustomFHIRFunctions.getId(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(realClia)
    }

    @Test
    fun `test get id type function`() {
        val isoType = "ISO"
        val cliaType = "CLIA"

        assertThat(CustomFHIRFunctions.getIdType(mutableListOf())).isEmpty()
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(MessageHeader()))).isEmpty()
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(DateTimeType()))).isEmpty()
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(OidType()))).isEmpty()

        // OID tests
        val goodOid = "1.2.3.4.5.6.7"
        val oid = OidType()
        oid.value = "AA" // Some non OID
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(oid))).isEmpty()
        oid.value = goodOid // Not a real OID as it needs to start with urn:oid:
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(oid))).isEmpty()
        oid.value = "urn:oid:$goodOid"
        var id = CustomFHIRFunctions.getIdType(mutableListOf(oid))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(isoType)

        val oidInString = StringType()
        oidInString.value = goodOid
        id = CustomFHIRFunctions.getIdType(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(isoType)

        oidInString.value = "urn:oid:$goodOid"
        id = CustomFHIRFunctions.getIdType(mutableListOf(oidInString))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(isoType)

        // Generic IDs don't have a type
        val someId = "someId"
        val genId = StringType()
        genId.value = "urn:id:$someId"
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()
        genId.value = someId
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(genId))).isEmpty()

        // CLIA IDs
        val cliaId = StringType()
        cliaId.value = "dummy"
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(cliaId))).isEmpty()

        val realClia = "15D2112066"
        cliaId.value = "urn:id:$realClia"
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

        cliaId.value = "P5D9458369" // other valid CLIA
        id = CustomFHIRFunctions.getIdType(mutableListOf(cliaId))
        assertThat(id.size).isEqualTo(1)
        assertThat(id[0].primitiveValue()).isEqualTo(cliaType)

        cliaId.value = "D5D945836K" // letter where it's not allowed
        assertThat(CustomFHIRFunctions.getIdType(mutableListOf(cliaId))).isEmpty()
    }
}