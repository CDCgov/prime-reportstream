package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslatorTests {
    private val receiversYaml = """
        ---
          # Arizona PHD
          - name: phd1
            description: Arizona PHD
            jurisdiction: STATE
            stateCode: AZ
            receivers: 
            - name: elr
              organizationName: phd1
              topic: test
              jurisdictionalFilter: [ "matches(a, 1)"]
              translation: 
                type: CUSTOM
                schemaName: one
                format: CSV
    """.trimIndent()

    private val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))

    @Test
    fun `test buildMapping`() {
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap()).run {
            assertThat(fromSchema).isEqualTo(one)
            assertThat(toSchema).isEqualTo(two)
            assertThat(useDirectly.size).isEqualTo(1)
            assertThat(useDirectly["a"]).isEqualTo("a")
            assertThat(useDefault.contains("b")).isEqualTo(false)
            assertThat(missing.size).isEqualTo(0)
        }
        translator.buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap()).run {
            assertThat(useDirectly.size).isEqualTo(1)
            assertThat(useDirectly["a"]).isEqualTo("a")
            assertThat(useDefault.size).isEqualTo(0)
            assertThat(missing.size).isEqualTo(0)
        }
    }

    @Test
    fun `test buildMapping with default`() {
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b", default = "x")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = mapOf("b" to "foo")).run {
            assertThat(useDefault.contains("b")).isTrue()
            assertThat(useDefault["b"]).isEqualTo("foo")
        }
    }

    @Test
    fun `test buildMapping with missing`() {
        val three = Schema(
            name = "three",
            topic = "test",
            elements = listOf(Element("a"), Element("c", cardinality = Element.Cardinality.ONE))
        )
        val metadata = Metadata().loadSchemas(one, three)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = three, defaultValues = emptyMap()).run {
            assertThat(this.useDirectly.size).isEqualTo(1)
            assertThat(this.useDirectly["a"]).isEqualTo("a")
            assertThat(this.useDefault.size).isEqualTo(0)
            assertThat(this.missing.size).isEqualTo(1)
        }
    }

    @Test
    fun `test filterAndMapByReceiver`() {
        val metadata = Metadata()
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), TestSource)
        translator.filterAndTranslateByReceiver(table1, warnings = mutableListOf()).run {
            assertThat(this.size).isEqualTo(1)
            val (mappedTable, forReceiver) = this[0]
            assertThat(mappedTable.schema).isEqualTo(table1.schema)
            assertThat(mappedTable.itemCount).isEqualTo(1)
            assertThat(forReceiver).isEqualTo(settings.receivers.toTypedArray()[0])
        }
    }

    @Test
    fun `test mappingWithReplace`() {
//        val metadata = Metadata()
        val receiverAKYaml = """
        ---
          - name: ak-phd
            description: Alaska Public Health Department
            jurisdiction: STATE
            stateCode: AK
            receivers:
            - name: elr
              organizationName: ak-phd
              topic: covid-19
              jurisdictionalFilter:
                - orEquals(ordering_facility_state, AK, patient_state, AK)
              translation:
                type: HL7
                useBatchHeaders: true
                suppressHl7Fields: PID-5-7, ORC-12-1, OBR-16-1
                replaceValue:
                  PID-22-3: CDCREC
                  OBX-2-1: TestVal
              timing:
                operation: MERGE
                numberPerDay: 1440 # Every minute
                initialTime: 00:00
                timeZone: EASTERN
              transport:
                type: SFTP
                host: sftp
                port: 22
                filePath: ./upload
                credentialName: DEFAULT-SFTP
        """.trimIndent()
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(receiverAKYaml.toByteArray()))
        }
        val translation = settings.receivers.elementAt(0).translation as? Hl7Configuration?
        val replaceVal = translation?.replaceValue?.get("OBX-2-1")
        assertEquals(replaceVal, "TestVal")
    }
}