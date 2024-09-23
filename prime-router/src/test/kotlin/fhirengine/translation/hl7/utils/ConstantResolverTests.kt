package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
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
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ConstantResolverTests {

    @Test
    fun `test cannot add reserved constant name`() {
        val context = CustomContext(Bundle(), Bundle())
        val constantValue = "value1"
        assertThrows<SchemaException> {
            CustomContext.addConstants(mapOf("context" to constantValue), context)
        }
    }

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
        assertThat(context2).isNotSameInstanceAs(context) // Test the reference is different

        context2 = CustomContext.addConstants(mapOf(), context)
        assertThat(context2).isNotNull()
        assertThat(context2).isSameInstanceAs(context)
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
        assertFailure { resolver.replace(inputString, context) }
        assertFailure { resolver.replace(inputString, null) }
    }

    @Test
    fun `test fhir path resolver`() {
        mockkObject(FhirPathUtils)
        assertFailure { FhirPathCustomResolver().resolveConstant(null, null, null, false, false) }
        assertFailure { FhirPathCustomResolver().resolveConstant(null, null, "const1", false, false) }
            .hasClass(PathEngineException::class.java)

        val integerValue = 99
        val urlPrefix = "https://reportstream.cdc.gov/fhir/StructureDefinition/"
        val constants = sortedMapOf("const1" to "'value1'", "int1" to "'$integerValue'", "rsext" to "'$urlPrefix'")
        val context = CustomContext.addConstants(constants, CustomContext(Bundle(), Bundle()))
        assertThat(FhirPathCustomResolver().resolveConstant(null, context, "const2", false, false)).isEmpty()
        assertThat(FhirPathCustomResolver().resolveConstant(null, context, "const1", false, false)).isNotNull()
        var result = FhirPathCustomResolver().resolveConstant(null, context, "int1", false, false)
        assertThat(result).isNotNull()
        assertThat(result).isNotEmpty()
        assertThat(result[0] is IntegerType).isTrue()
        assertThat((result[0] as IntegerType).value).isEqualTo(integerValue)

        // Now lets resolve a constant
        result = FhirPathCustomResolver().resolveConstant(null, context, "const1", false, false)
        assertThat(result).isNotNull()
        assertThat(result.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(
            constants[constants.firstKey()]!!.replace("'", "")
        )

        // Test the ability to resolve constants with suffix
        val urlSuffix = "SomeSuffix"
        result = FhirPathCustomResolver().resolveConstant(null, context, "`rsext-$urlSuffix`", false, false)
        assertThat(result).isNotNull()
        assertThat(result.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo("$urlPrefix$urlSuffix")

        result = FhirPathCustomResolver().resolveConstant(null, context, "`rsext`", false, false)
        assertThat(result).isNotNull()
        assertThat(result.isNotEmpty())
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(urlPrefix)

        result = FhirPathCustomResolver().resolveConstant(null, context, "unknownconst", false, false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test fhir path resolver multiple values`() {
        val integerValue = 99
        val stringValue = "Ninety-Nine"
        val giantStringValue = "9999999999999999999"

        mockkObject(FhirPathUtils)
        every { FhirPathUtils.evaluate(any(), any(), any(), any()) } returns
            listOf<Base>(StringType(stringValue), StringType(integerValue.toString()), StringType(giantStringValue))

        val constants = sortedMapOf("const1" to "'value1'") // this does not matter but context wants something
        val context = CustomContext.addConstants(constants, CustomContext(Bundle(), Bundle()))
        val result = FhirPathCustomResolver().resolveConstant(null, context, "const1", false, false)
        assertThat(result).isNotNull()
        assertThat(result.isNotEmpty())
        assertThat(result.size == 3)
        assertThat(result[0].isPrimitive).isTrue()
        assertThat(result[0]).isInstanceOf(StringType::class.java)
        assertThat((result[0] as StringType).value).isEqualTo(stringValue)
        assertThat(result[1].isPrimitive).isTrue()
        assertThat(result[1] is IntegerType).isTrue()
        assertThat((result[1] as IntegerType).value).isEqualTo(integerValue)
        assertThat(result[2].isPrimitive).isTrue()
        assertThat(result[2]).isInstanceOf(StringType::class.java)
        assertThat((result[2] as StringType).value).isEqualTo(giantStringValue)
    }

    @Test
    fun `test execute additional FHIR functions`() {
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        val context = CustomContext(Bundle(), Bundle())
        assertThat(
            FhirPathCustomResolver(CustomFhirPathFunctions()).executeFunction(
                null,
                context,
                mutableListOf(Observation()),
                "livdTableLookup",
                null
            )
        )
    }

    @Test
    fun `test execute additional FHIR functions unknown function`() {
        val context = CustomContext(Bundle(), Bundle())
        assertFailure {
            FhirPathCustomResolver(CustomFhirPathFunctions()).executeFunction(
                null,
                context,
                mutableListOf(Observation()),
                "unknown",
                null
            )
        }
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
        assertThat(FhirPathCustomResolver().resolveReference(null, customContext, org2Url, null)).isNull()

        bundle.addEntry().resource = org1
        bundle.entry[0].fullUrl = "Organization/${org1.id}"
        assertThat(FhirPathCustomResolver().resolveReference(null, customContext, org2Url, null)).isNull()

        bundle.addEntry().resource = org2
        bundle.entry[1].fullUrl = org2Url
        val reference = FhirPathCustomResolver().resolveReference(null, customContext, org2Url, null)
        assertThat(reference).isNotNull()
        assertThat(reference).isEqualTo(org2)
    }
}