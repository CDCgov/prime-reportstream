package gov.cdc.prime.router.fhirengine.translation.hl7

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import kotlin.test.Test

class ExceptionTests {
    @Test
    fun `test exceptions`() {
        val aMsg = "some text"
        val anException = IllegalArgumentException()

        assertThat(SchemaException(aMsg).message).isEqualTo(aMsg)
        assertThat(HL7ConversionException(aMsg).message).isEqualTo(aMsg)

        assertThat(SchemaException(aMsg, anException).cause is IllegalArgumentException).isTrue()
        assertThat(HL7ConversionException(aMsg, anException).cause is IllegalArgumentException).isTrue()

        val element = ConverterSchemaElement("abc123")
        assertThat(RequiredElementException(element).message).isNotNull()
        assertThat(RequiredElementException(element).message!!.contains(element.name!!)).isTrue()
    }
}