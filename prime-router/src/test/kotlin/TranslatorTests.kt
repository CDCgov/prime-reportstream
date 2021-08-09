package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
            assertThat(one).isEqualTo(fromSchema)
            assertThat(two).isEqualTo(toSchema)
            assertThat(1).isEqualTo(useDirectly.size)
            assertThat("a").isEqualTo(useDirectly["a"])
            assertThat(false).isEqualTo(useDefault.contains("b"))
            assertThat(0).isEqualTo(missing.size)
        }
        translator.buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap()).run {
            assertThat(1).isEqualTo(useDirectly.size)
            assertThat("a").isEqualTo(useDirectly["a"])
            assertThat(0).isEqualTo(useDefault.size)
            assertThat(0).isEqualTo(missing.size)
        }
    }

    @Test
    fun `test buildMapping with default`() {
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b", default = "x")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = mapOf("b" to "foo")).run {
            assertThat(useDefault.contains("b")).isTrue()
            assertThat("foo").isEqualTo(useDefault["b"])
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
            assertThat(1).isEqualTo(this.useDirectly.size)
            assertThat("a").isEqualTo(this.useDirectly["a"])
            assertThat(0).isEqualTo(this.useDefault.size)
            assertThat(1).isEqualTo(this.missing.size)
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
            assertThat(1).isEqualTo(this.size)
            val (mappedTable, forReceiver) = this[0]
            assertThat(table1.schema).isEqualTo(mappedTable.schema)
            assertThat(1).isEqualTo(mappedTable.itemCount)
            assertThat(settings.receivers.toTypedArray()[0]).isEqualTo(forReceiver)
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
        val replaceVal = translation?.replaceValue?.get("PID-22-3")
        assertEquals(replaceVal, "CDCREC")
    }
}