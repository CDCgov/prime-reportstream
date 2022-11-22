package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import gov.cdc.prime.router.metadata.ConcatenateMapper
import gov.cdc.prime.router.metadata.ElementNames
import gov.cdc.prime.router.metadata.LIVDLookupMapper
import gov.cdc.prime.router.metadata.NullMapper
import gov.cdc.prime.router.metadata.TrimBlanksMapper
import kotlin.test.Test

class SchemaTests {
    @Test
    fun `create schema`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        assertThat(one).isNotNull()
    }

    @Test
    fun `compare schemas`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val oneAgain = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        assertThat(one)
            .isEqualTo(oneAgain)
        assertThat(one)
            .isNotEqualTo(two)
    }

    @Test
    fun `find element`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        assertThat(one.findElement("a")).isEqualTo(Element("a"))
        assertThat(one.findElement("c")).isNull()
    }

    @Test
    fun `test mapper ordering`() {
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c", mapperRef = NullMapper())
        val elementD = Element("d", mapperRef = ConcatenateMapper(), mapperArgs = listOf("a", "b"))
        val elementE = Element("e", mapperRef = TrimBlanksMapper(), mapperArgs = listOf("e"))
        // These two elements depend on each other, so they will be last
        val elementF = Element("f", mapperRef = ConcatenateMapper(), mapperArgs = listOf("a", "e", "g"))
        val elementG = Element("g", mapperRef = ConcatenateMapper(), mapperArgs = listOf("e", "f", "d"))
        // Some LIVD Mapper elements
        val livdElementA = Element(ElementNames.EQUIPMENT_MODEL_NAME.elementName, mapperRef = LIVDLookupMapper())
        val livdElementB = Element(ElementNames.TEST_KIT_NAME_ID.elementName, mapperRef = LIVDLookupMapper())

        val schema1 = Schema(
            name = "one", topic = Topic.TEST,
            elements = listOf(
                elementA, elementB, elementC, elementD, elementE, elementF, elementG, livdElementA,
                livdElementB
            )
        )
        var orderedElements = schema1.orderElementsByMapperDependencies()
        assertThat(orderedElements[0]).isEqualTo(elementA)
        assertThat(orderedElements[1]).isEqualTo(elementB)
        assertThat(orderedElements[2]).isEqualTo(elementC)
        assertThat(orderedElements[3]).isEqualTo(elementD)
        assertThat(orderedElements[4]).isEqualTo(elementE)
        assertThat(orderedElements[5]).isEqualTo(livdElementA)
        assertThat(orderedElements[6]).isEqualTo(livdElementB)
        assertThat(orderedElements[7]).isEqualTo(elementF)
        assertThat(orderedElements[8]).isEqualTo(elementG)

        val schema2 = Schema(
            name = "one", topic = Topic.TEST,
            elements = listOf(
                livdElementB, livdElementA, elementG, elementF, elementE, elementD, elementC, elementB,
                elementA
            )
        )
        orderedElements = schema2.orderElementsByMapperDependencies()
        assertThat(orderedElements[0]).isEqualTo(elementB)
        assertThat(orderedElements[1]).isEqualTo(elementA)
        assertThat(orderedElements[2]).isEqualTo(elementE)
        assertThat(orderedElements[3]).isEqualTo(elementD)
        assertThat(orderedElements[4]).isEqualTo(elementC)
        assertThat(orderedElements[5]).isEqualTo(livdElementA)
        assertThat(orderedElements[6]).isEqualTo(livdElementB)
        assertThat(orderedElements[7]).isEqualTo(elementG)
        assertThat(orderedElements[8]).isEqualTo(elementF)

        val schema3 = Schema(
            name = "one", topic = Topic.TEST,
            elements = listOf(
                elementD, elementC, elementF, livdElementB, livdElementA, elementG, elementE, elementB,
                elementA
            )
        )
        orderedElements = schema3.orderElementsByMapperDependencies()
        assertThat(orderedElements[0]).isEqualTo(elementB)
        assertThat(orderedElements[1]).isEqualTo(elementA)
        assertThat(orderedElements[2]).isEqualTo(elementD)
        assertThat(orderedElements[3]).isEqualTo(elementC)
        assertThat(orderedElements[4]).isEqualTo(elementE)
        assertThat(orderedElements[5]).isEqualTo(livdElementA)
        assertThat(orderedElements[6]).isEqualTo(livdElementB)
        assertThat(orderedElements[7]).isEqualTo(elementF)
        assertThat(orderedElements[8]).isEqualTo(elementG)
    }

    @Test
    fun `test process values`() {
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c", mapperRef = NullMapper())
        val elementD = Element("d", mapperRef = ConcatenateMapper(), mapperArgs = listOf("a", "b"))
        val elementE = Element(
            "e", mapperRef = TrimBlanksMapper(), mapperArgs = listOf("e"),
            mapperOverridesValue = true
        )
        val elementF = Element("f", mapperRef = ConcatenateMapper(), mapperArgs = listOf("a", "d", "e"))
        val schema1 = Schema(
            name = "one", topic = Topic.TEST,
            elements = listOf(elementA, elementB, elementC, elementD, elementE, elementF)
        )

        val allElementValues1 = mapOf(
            elementA.name to "1", elementB.name to "2", elementC.name to "3",
            elementE.name to " with blanks ", elementF.name to "6"
        )

        val modifiedValues = mutableMapOf<String, String>()
        modifiedValues.putAll(allElementValues1)
        schema1.processValues(modifiedValues, mutableListOf(), mutableListOf(), itemIndex = 1)
        assertThat(modifiedValues[elementA.name]).isEqualTo(allElementValues1[elementA.name])
        assertThat(modifiedValues[elementB.name]).isEqualTo(allElementValues1[elementB.name])
        assertThat(modifiedValues[elementC.name]).isEqualTo(allElementValues1[elementC.name])
        assertThat(modifiedValues[elementD.name])
            .isEqualTo("${allElementValues1[elementA.name]}, ${allElementValues1[elementB.name]}")
        assertThat(modifiedValues[elementE.name]).isEqualTo(allElementValues1[elementE.name]?.trim())
        assertThat(modifiedValues[elementF.name]).isEqualTo(allElementValues1[elementF.name])

        // Test the failure value
        modifiedValues.clear()
        modifiedValues.putAll(allElementValues1)
        modifiedValues[elementD.name] = "%%"
        schema1.processValues(
            modifiedValues, mutableListOf(), mutableListOf(), specialFailureValue = "%%",
            itemIndex = 1
        )
        assertThat(modifiedValues[elementD.name]).isNullOrEmpty()
    }
}