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
import assertk.assertions.isTrue
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test

class ConstantResolverTests {
    @Test
    fun `test custom context`() {
        assertThat(CustomContext.addConstants(mapOf(), null)).isNull()

        val constant = sortedMapOf("name1" to "value1")
        var context = CustomContext.addConstant(constant.firstKey(), constant[constant.firstKey()]!!, null)
        assertThat(context).isNotNull()
        assertThat(context!!.constants).isNotEmpty()
        assertThat(context.constants[constant.firstKey()]).isEqualTo(constant[constant.firstKey()])

        context = CustomContext.addConstants(constant, null)
        assertThat(context).isNotNull()
        assertThat(context!!.constants).isNotEmpty()
        assertThat(context.constants[constant.firstKey()]).isEqualTo(constant[constant.firstKey()])

        // Check that a new context is returned
        var context2 = CustomContext.addConstant("anotherconst", "value", context)
        assertThat(context2).isNotNull()
        assertThat(context2).isNotSameAs(context) // Test the reference is different

        context2 = CustomContext.addConstants(mapOf(), context)
        assertThat(context2).isNotNull()
        assertThat(context2).isSameAs(context)
    }

    @Test
    fun `test constant substitutortortortortortor - funny name`() {
        val constant = sortedMapOf("const1" to "value1")
        val context = CustomContext.addConstants(constant, null)

        var inputString = "Lorem ipsum %{const1} sit amet, consectetur adipiscing"
        val expectedString = "Lorem ipsum value1 sit amet, consectetur adipiscing"
        val result = ConstantSubstitutor.replace(inputString, context)
        assertThat(result).isEqualTo(expectedString)

        inputString = "Lorem ipsum %{const2} sit amet, consectetur adipiscing"
        assertThat { ConstantSubstitutor.replace(inputString, context) }.isFailure()
        assertThat { ConstantSubstitutor.replace(inputString, null) }.isFailure()
    }

    @Test
    fun `test fhir path resolver`() {
        assertThat { FhirPathCustomResolver().resolveConstant(null, null, false) }.isFailure()
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const1", false) }
            .isFailure().hasClass(PathEngineException::class.java)
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const1", false) }
            .isFailure().hasClass(PathEngineException::class.java)

        val integerValue = 99
        val constant = sortedMapOf("const1" to "value1", "int1" to integerValue.toString())
        val context = CustomContext.addConstants(constant, null)
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const2", false) }
            .isFailure().hasClass(PathEngineException::class.java)
        assertThat { FhirPathCustomResolver().resolveConstant(null, "const1", false) }
            .isFailure().hasClass(PathEngineException::class.java)

        // Now lets resolve a constant
        var result = FhirPathCustomResolver().resolveConstant(context, "const1", false)
        assertThat(result.isPrimitive).isTrue()
        assertThat(result).isInstanceOf(StringType::class.java)
        assertThat((result as StringType).value).isEqualTo(constant[constant.firstKey()])

        result = FhirPathCustomResolver().resolveConstant(context, "int1", false)
        assertThat(result.isPrimitive).isTrue()
        assertThat(result).isInstanceOf(IntegerType::class.java)
        assertThat((result as IntegerType).value).isEqualTo(integerValue)
    }
}