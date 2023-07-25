package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
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
        mockkObject(FhirPathUtils)
        assertThat { FhirPathCustomResolver().resolveConstant(null, null, false) }.isFailure()
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
        assertThat(result!!).isNotEmpty()
        assertThat(result[0] is IntegerType).isTrue()
        assertThat((result[0] as IntegerType).value).isEqualTo(integerValue)

        // Now lets resolve a constant
        result = FhirPathCustomResolver().resolveConstant(context, "const1", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(
            constants[constants.firstKey()]!!.replace("'", "")
        )

        // Test the ability to resolve constants with suffix
        val urlSuffix = "SomeSuffix"
        result = FhirPathCustomResolver().resolveConstant(context, "`rsext-$urlSuffix`", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo("$urlPrefix$urlSuffix")

        result = FhirPathCustomResolver().resolveConstant(context, "`rsext`", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(urlPrefix)

        result = FhirPathCustomResolver().resolveConstant(context, "unknownconst", false)
        assertThat(result).isNull()
    }

    @Test
    fun `test fhir path resolver multiple values`() {
        val integerValue = 99
        val stringValue = "Ninety-Nine"

        mockkObject(FhirPathUtils)
        every { FhirPathUtils.evaluate(any(), any(), any(), any()) } returns
            listOf<Base>(StringType(stringValue), StringType(integerValue.toString()))

        val constants = sortedMapOf("const1" to "'value1'") // this does not matter but context wants something
        val context = CustomContext.addConstants(constants, CustomContext(Bundle(), Bundle()))
        val result = FhirPathCustomResolver().resolveConstant(context, "const1", false)
        assertThat(result).isNotNull()
        assertThat(result!!.isNotEmpty())
        assertThat(result.size == 2)
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(stringValue)
        assertThat(result[1].isPrimitive).isTrue()
        assertThat(result[1] is IntegerType).isTrue()
        assertThat((result[1] as IntegerType).value).isEqualTo(integerValue)
    }

    @Test
    fun `test execute additional FHIR functions`() {
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        val context = CustomContext(Bundle(), Bundle())
        assertThat {
            FhirPathCustomResolver(CustomFhirPathFunctions()).executeFunction(
                context,
                mutableListOf(Observation()),
                "livdTableLookup",
                null
            )
        }.isSuccess()
    }

    @Test
    fun `test execute additional FHIR functions unknown function`() {

        val context = CustomContext(Bundle(), Bundle())
        assertThat {
            FhirPathCustomResolver(CustomFhirPathFunctions()).executeFunction(
                context,
                mutableListOf(Observation()),
                "unknown",
                null
            )
        }.isFailure()
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
}