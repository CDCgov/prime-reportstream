package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapperTests {
    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        val element = Element("test")
        assertEquals("R", mapper.apply(element, args, listOf(ElementAndValue(element, "Rick"))))
        assertEquals("R", mapper.apply(element, args, listOf(ElementAndValue(element, "rick"))))
    }

    @Test
    fun `test LookupMapper`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        Metadata.addLookupTable("test", ByteArrayInputStream(csv.toByteArray()))
        val mapper = LookupMapper()
        val args = listOf("a")
        val elementIndex = Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a")
        val elementLookup = Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
        assertEquals(listOf("a"), mapper.valueNames(elementLookup, args))
        assertEquals("y", mapper.apply(elementLookup, args, listOf(ElementAndValue(elementIndex, "3"))))
    }

    @Test
    fun `test LookupMapper with two`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        Metadata.addLookupTable("test", ByteArrayInputStream(csv.toByteArray()))
        val mapper = LookupMapper()
        val args = listOf("a", "b")
        val elementIndex = Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a")
        val elementIndex2 = Element("b", type = Element.Type.TABLE, table = "test", tableColumn = "b")
        val elementLookup = Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
        val elementAndValues = listOf(ElementAndValue(elementIndex, "3"), ElementAndValue(elementIndex2, "4"))
        assertEquals(listOf("a", "b"), mapper.valueNames(elementLookup, args))
        assertEquals("y", mapper.apply(elementLookup, args, elementAndValues))
    }

    @Test
    fun `test ifPresent`() {
        val element = Element("a")
        val mapper = IfPresentMapper()
        val args = listOf("a", "const")
        assertEquals(listOf("a"), mapper.valueNames(element, args))
        assertEquals("const", mapper.apply(element, args, listOf(ElementAndValue(element, "3"))))
        assertNull(mapper.apply(element, args, emptyList()))
    }

    @Test
    fun `test use`() {
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val mapper = UseMapper()
        val args = listOf("b", "c")
        assertEquals(listOf("b", "c"), mapper.valueNames(elementA, args))
        assertEquals(
            "B",
            mapper.apply(elementA, args, listOf(ElementAndValue(elementB, "B"), ElementAndValue(elementC, "C")))
        )
        assertEquals("C", mapper.apply(elementA, args, listOf(ElementAndValue(elementC, "C"))))
        assertNull(mapper.apply(elementA, args, emptyList()))
    }

    @Test
    fun `test ConcatenateMapper`() {
        val mapper = ConcatenateMapper()
        val args = listOf("a", "b", "c")
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )

        assertEquals("string1, string2, string3", mapper.apply(elementA, args, values))
    }
}