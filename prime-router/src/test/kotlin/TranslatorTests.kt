package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
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
            filters:
            # override the default set for any of the filters, to get a clean test
            - topic: test
              jurisdictionalFilter: [ "allowAll()" ]
              qualityFilter: [ "allowAll()" ]
              routingFilter: [ "allowAll()" ]
            receivers: 
            - name: elr
              organizationName: phd1
              topic: test
              customerStatus: active
              jurisdictionalFilter: [ "matches(a, 1)"]
              translation: 
                type: CUSTOM
                schemaName: one
                format: CSV
    """.trimIndent()

    /**
     * This covers several test cases:
     * jurisdictionalFilter :   default is missing, but both org and receiver level filters are applied.
     * qualityFilter:  has a default, and only org level filter is applied (no receiver level filtering)
     * routingFilter: has a default, and only receiver level filter is applied (no org level filtering)
     */
    private val filterTestYaml = """
        ---
          - name: phd
            description: Piled Higher and Deeper 
            jurisdiction: STATE
            filters:
            - topic: test
              jurisdictionalFilter: [ "matches(b,true)" ]
              qualityFilter: [ "matches(b,true)" ]
              # Missing routingFilter
            stateCode: IG
            receivers: 
            - name: elr
              organizationName: phd
              topic: test
              customerStatus: active
              jurisdictionalFilter: [ "matches(a,yes)"]
              # Missing qualityFilter
              routingFilter: [ "matches(a,yes)"]
              translation: 
                type: CUSTOM
                schemaName: two
                format: CSV
    """.trimIndent()

    /**
     * This covers several more test cases:
     * jurisdictionalFilter :   all are missing: default, org, receiver
     * qualityFilter:  has a default, but org and receiver filters are both missing. AND its reversed!
     * routingFilter:  has a default, but org and receiver filters are both missing.
     */
    private val onlyDefaultFiltersYaml = """
        ---
          - name: xyzzy
            description: A maze of twisty passages, all alike
            jurisdiction: STATE
            stateCode: IG
            receivers: 
            - name: elr
              organizationName: xyzzy
              topic: test
              customerStatus: active
              reverseTheQualityFilter: true
              translation: 
                type: CUSTOM
                schemaName: two
                format: CSV
    """.trimIndent()

    private val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))

    @Test
    fun `test filterByOneFilterType`() {
        val mySchema = Schema(
            name = "two", topic = "test", elements = listOf(Element("a"), Element("b"))
        )
        val metadata = Metadata().loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(filterTestYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 2 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("yes", "true"), // row 0
                listOf("no", "true"),
                listOf("yes", "false"),
                listOf("no", "false"), // row 3
            ),
            TestSource
        )
        val rcvr = settings.findReceiver("phd.elr")
        assertThat(rcvr).isNotNull()
        val org = settings.findOrganization("phd")
        assertThat(org).isNotNull()
        // Juris filter: No default exists, both org and receiver exist.
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.JURISDICTIONAL_FILTER, true
        ).run {
            assertThat(this.itemCount).isEqualTo(1)
            assertThat(this.getRow(0)[0]).isEqualTo("yes") // row 0
            assertThat(this.getRow(0)[1]).isEqualTo("true") // row 0
            assertThat(this.filteredItems.size).isEqualTo(2) // two rows eliminated, and two filter messages.
            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(2) // rows 2 and 3 eliminated (zero based)
            assertThat(this.filteredItems[0].filteredRows[0]).isEqualTo(2)
            assertThat(this.filteredItems[0].filteredRows[1]).isEqualTo(3)
            assertThat(this.filteredItems[1].filteredRows.size).isEqualTo(2) // rows 1 and 3 eliminated (zero based)
            assertThat(this.filteredItems[1].filteredRows[0]).isEqualTo(1)
            assertThat(this.filteredItems[1].filteredRows[1]).isEqualTo(3)
        }
        // Quality filter: Override the default; org filter exists.  No receiver filter.
        translator.filterByOneFilterType(table1, rcvr, org, ReportStreamFilterType.QUALITY_FILTER, true).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("yes")
            assertThat(this.getRow(0)[1]).isEqualTo("true")
            assertThat(this.getRow(1)[0]).isEqualTo("no")
            assertThat(this.getRow(1)[1]).isEqualTo("true")
            assertThat(this.filteredItems.size).isEqualTo(1) // two rows eliminated, but one filter message.
            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(2)
        }
        // Routing filter: Override the default; No org filter. Receiver filter exists.
        translator.filterByOneFilterType(table1, rcvr, org, ReportStreamFilterType.ROUTING_FILTER, true).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("yes")
            assertThat(this.getRow(0)[1]).isEqualTo("true")
            assertThat(this.getRow(1)[0]).isEqualTo("yes")
            assertThat(this.getRow(1)[1]).isEqualTo("false")
            assertThat(this.filteredItems.size).isEqualTo(1) // two rows eliminated, but one filter message.
            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(2)
        }
    }

    @Test
    fun `test filterByOneFilterType Defaults`() {
        val mySchema = Schema(
            name = "two", topic = "test", elements = listOf(Element("a"), Element("b"))
        )
        val metadata = Metadata().loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(onlyDefaultFiltersYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 2 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("yes", "true"), // row 0
                listOf("no", "true"),
                listOf("yes", "false"),
                listOf("no", "false"), // row 3
            ),
            TestSource
        )
        val rcvr = settings.findReceiver("xyzzy.elr")
        assertThat(rcvr).isNotNull()
        val org = settings.findOrganization("xyzzy")
        assertThat(org).isNotNull()
        // Juris filter: No default, org, or receiver filters exist.  No filtering done.
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.JURISDICTIONAL_FILTER, true
        ).run {
            assertThat(this.itemCount).isEqualTo(4)
            // just confirm the first and last rows
            assertThat(this.getRow(0)[0]).isEqualTo("yes")
            assertThat(this.getRow(0)[1]).isEqualTo("true")
            assertThat(this.getRow(3)[0]).isEqualTo("no")
            assertThat(this.getRow(3)[1]).isEqualTo("false")
            assertThat(this.filteredItems.size).isEqualTo(0) // logging turned on, but no rows eliminated.
        }
        // Quality filter: Default rules apply only.  No org or receiver level filters.  And its reversed!
        translator.filterByOneFilterType(table1, rcvr, org, ReportStreamFilterType.QUALITY_FILTER, false).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("yes")
            assertThat(this.getRow(0)[1]).isEqualTo("true")
            assertThat(this.getRow(1)[0]).isEqualTo("yes")
            assertThat(this.getRow(1)[1]).isEqualTo("false")
            assertThat(this.filteredItems.size).isEqualTo(0) // no logging done.
        }
        // Routing filter: Default rules apply only.  No org or receiver level filters. No weird reversing.
        translator.filterByOneFilterType(table1, rcvr, org, ReportStreamFilterType.ROUTING_FILTER, true).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("yes")
            assertThat(this.getRow(0)[1]).isEqualTo("false")
            assertThat(this.getRow(1)[0]).isEqualTo("no")
            assertThat(this.getRow(1)[1]).isEqualTo("false")
            assertThat(this.filteredItems.size).isEqualTo(1) // two rows eliminated, by one rule.
            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(2) // rows 0 and 1 eliminated (zero based)
            assertThat(this.filteredItems[0].filteredRows[0]).isEqualTo(0)
            assertThat(this.filteredItems[0].filteredRows[1]).isEqualTo(1)
        }
    }

    @Test
    fun `test filterByAllFilterTypes`() {
        val mySchema = Schema(
            name = "two", topic = "test", elements = listOf(Element("a"), Element("b"))
        )
        val metadata = Metadata().loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(filterTestYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 2 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("yes", "true"), // row 0
                listOf("no", "true"), // row 1
                listOf("yes", "false"), // row 2
                listOf("no", "false"), // row 3
            ),
            TestSource
        )
        val rcvr = settings.findReceiver("phd.elr")
        assertThat(rcvr).isNotNull()
        // Juris filter eliminates rows 1,2,3 (zero based), but does not create filteredItem entries.
        translator.filterByAllFilterTypes(settings, table1, rcvr!!).run {
            assertThat(this).isNotNull()
            assertThat(this!!.itemCount).isEqualTo(1)
            assertThat(this.getRow(0)[0]).isEqualTo("yes") // row 0
            assertThat(this.getRow(0)[1]).isEqualTo("true") // row 0
            assertThat(this.filteredItems.size).isEqualTo(0) // three rows eliminated, but nothing logged.
        }

        val settings2 = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(onlyDefaultFiltersYaml.toByteArray()))
        }
        val rcvr2 = settings2.findReceiver("xyzzy.elr")
        assertThat(rcvr2).isNotNull()
        // No juris filtering done.
        // Default matches a = "no" in qualityFilter, but its reversed.  So original rows 1,3 eliminated, rows 0,2 kept
        // But: no logging because its reversed!
        // Not done yet!  Then match on b = "false" in routingFilter.  At this point rows are numbered 0,1.
        // So new row 1 is kept ("yes", "false")
        translator.filterByAllFilterTypes(settings2, table1, rcvr2!!).run {
            assertThat(this).isNotNull()
            assertThat(this!!.itemCount).isEqualTo(1)
            assertThat(this.getRow(0)[0]).isEqualTo("yes") // row 3
            assertThat(this.getRow(0)[1]).isEqualTo("false") // row 3
            assertThat(this.filteredItems.size).isEqualTo(1) // three rows eliminated, only routingFilter message.
//            // rows 1 and 3 of the original four rows are eliminated by the qualityFilter:
//            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(2) // rows 1 and 3 eliminated (zero based)
//            assertThat(this.filteredItems[0].filteredRows[0]).isEqualTo(2)
//            assertThat(this.filteredItems[0].filteredRows[1]).isEqualTo(3)
            // so the routing filter only sees original rows 0 and 2, now called 0 and 1.  Sigh.
            assertThat(this.filteredItems[0].filteredRows.size).isEqualTo(1) // rows 0 eliminated
            assertThat(this.filteredItems[0].filteredRows[0]).isEqualTo(0)
        }
    }

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
        val twoWithDefault = Schema(
            name = "two", topic = "test",
            elements = listOf(Element("a"), Element("b", default = "x")),
        )
        val metadata = Metadata().loadSchemas(one, twoWithDefault)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = twoWithDefault, defaultValues = mapOf("b" to "foo")).run {
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
        val table1 = Report(
            one,
            listOf(
                listOf("1", "2"), // first row of data
                listOf("3", "4"), // second row of data
            ),
            TestSource
        )
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
              customerStatus: active
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