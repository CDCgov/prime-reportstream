package gov.cdc.prime.router.cli.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompareDataTests {

    @Test
    fun compareCsvRows() {
        val schema = Schema(
            name = "dummy",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = Element.csvFields("a")),
                Element("b", csvFields = Element.csvFields("b")),
                Element("c", csvFields = Element.csvFields("c"))
            )
        )

        val twoHeaders = listOf("a", "b")
        val threeHeaders = listOf("a", "b", "c")
        val rowA = listOf("a", "b", "c")
        val rowB = listOf("f", "b", "c")
        val rowC = listOf("a", "b")
        val rowD = emptyList<String>()
        val rowE = listOf("a", "b", "")
        assertThat(schema).isNotNull()

        // Rows equal
        var result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowA, rowA, threeHeaders, schema, 1, null, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(0)

        // Rows same number of cols, but different data
        result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowA, rowB, threeHeaders, schema, 1, null, result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        // Actual has more cols
        result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowA, rowC, twoHeaders, schema, 1, null, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(2)

        // Expected has more cols
        result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowC, rowA, threeHeaders, schema, 1, null, result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        // Actual has no cols
        result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowD, rowA, threeHeaders, schema, 1, null, result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        // Actual has value, but no expected value for col
        result = CompareData.Result()
        CompareCsvData().compareCsvRow(rowA, rowE, threeHeaders, schema, 1, null, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(1)
    }

    @Test
    fun compareHl7Component() {
        val xtnA = "XTN[999-999-9999]"
        val xtnB = "XTN[888-888-8888]"
        val xtnC = "XTN[^PRN^PH^^999^999^9999]"
        val xtnD = "XTN[^PRN^PH^^888^999^9999]"
        val stringA = "John"
        val stringB = "Karl"
        val emptyValue = ""

        // Simple string component the same
        var result = CompareData.Result()
        CompareHl7Data().compareComponent(stringA, stringA, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(0)

        // Simple string do not match
        result = CompareData.Result()
        CompareHl7Data().compareComponent(stringA, stringB, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        // HAPI data type equals
        result = CompareData.Result()
        CompareHl7Data().compareComponent(xtnA, xtnA, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(0)

        result = CompareData.Result()
        CompareHl7Data().compareComponent(xtnC, xtnC, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(0)

        // HAPI data type different
        result = CompareData.Result()
        CompareHl7Data().compareComponent(xtnA, xtnB, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        result = CompareData.Result()
        CompareHl7Data().compareComponent(xtnC, xtnD, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.warnings.size).isEqualTo(0)

        // Expected value is empty
        result = CompareData.Result()
        CompareHl7Data().compareComponent(xtnC, emptyValue, 1, "MSG-1", "dummy", result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
        assertThat(result.warnings.size).isEqualTo(5)
    }

    @Test
    fun `test HL7 ignored fields`() {
        var result = CompareData.Result()
        // A simple HL7 message to use for this test
        val sample1 = """
            MSH|^~\&||Any facility USA^31D2827476^CLIA|0.0.0.0.1|0.0.0.0.1|20200331082902||ORU^R01^ORU_R01|63774|T|2.5.1||||||||||
            PID|1|1731-TEST734|5000104^^^^MR||TEST^RESULTS||19950615|M||U||||||S|||111-11-1111
            PV1|1|O|||||||||||||||||1731-T734-40923
            OBR|6|||40923^TONSILLITIS -neg strep screen|||19980601184619|||||||||51789||OV
            OBX|1|ST|MLI-4000.15^TEMPERATURE||97.7|deg f|||||R|||19980601184619
        """.trimIndent()
        val msh7Diff = """
            MSH|^~\&||Any facility USA^31D2827476^CLIA|0.0.0.0.1|0.0.0.0.1|20180331082902||ORU^R01^ORU_R01|63774|T|2.5.1||||||||||
            PID|1|1731-TEST734|5000104^^^^MR||TEST^RESULTS||19950615|M||U||||||S|||111-11-1111
            PV1|1|O|||||||||||||||||1731-T734-40923
            OBR|6|||40923^TONSILLITIS -neg strep screen|||19980601184619|||||||||51789||OV
            OBX|1|ST|MLI-4000.15^TEMPERATURE||97.7|deg f|||||R|||19980601184619
        """.trimIndent()

        // Use the default COVID 19 ignored fields which ignores MSH-7.
        result = CompareHl7Data(result).compare(sample1.byteInputStream(), msh7Diff.byteInputStream())
        assertThat(result.passed).isTrue()

        // Now do the same test with no ignored fields
        result = CompareHl7Data(result, emptyList()).compare(sample1.byteInputStream(), msh7Diff.byteInputStream())
        assertThat(result.passed).isFalse()

        // And with some other ignored fields
        result = CompareHl7Data(result, listOf("MSH-6", "MSH-8"))
            .compare(sample1.byteInputStream(), msh7Diff.byteInputStream())
        assertThat(result.passed).isFalse()

        // Lets test multiple ignored fields
        val msh7AndObx3Diff = """
            MSH|^~\&||Any facility USA^31D2827476^CLIA|0.0.0.0.1|0.0.0.0.1|20180331082902||ORU^R01^ORU_R01|63774|T|2.5.1||||||||||
            PID|1|1731-TEST734|5000104^^^^MR||TEST^RESULTS||19950615|M||U||||||S|||111-11-1111
            PV1|1|O|||||||||||||||||1731-T734-40923
            OBR|6|||40923^TONSILLITIS -neg strep screen|||19980601184619|||||||||51789||OV
            OBX|1|ST|MLI-4999.15^TEMPERATURE||97.7|deg f|||||R|||19980601184619
        """.trimIndent()
        result = CompareHl7Data(result, listOf("MSH-6", "MSH-8"))
            .compare(sample1.byteInputStream(), msh7AndObx3Diff.byteInputStream())
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(2)

        result = CompareHl7Data(result, listOf("MSH-6", "MSH-8", "OBX-3"))
            .compare(sample1.byteInputStream(), msh7AndObx3Diff.byteInputStream())
        assertThat(result.passed).isFalse()
        assertThat(result.errors.size).isEqualTo(1)

        result = CompareHl7Data(result, listOf("MSH-6", "MSH-8", "OBX-3", "MSH-7"))
            .compare(sample1.byteInputStream(), msh7AndObx3Diff.byteInputStream())
        assertThat(result.passed).isTrue()
        assertThat(result.errors.size).isEqualTo(0)
    }
}