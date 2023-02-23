package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.unittest.UnitTestUtils
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
              processingModeFilter: [ "allowAll()" ]
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

    private val filterTestYamlFilterOutNegAntigenTestType = """
        ---
          - name: phd
            description: Piled Higher and Deeper 
            jurisdiction: STATE
            filters:
            - topic: test
              qualityFilter: [ "filterOutNegativeAntigenTestType(test_result, 260385009, 260415000, 895231008)" ]
            stateCode: IG
            receivers: 
            - name: elr
              organizationName: phd
              topic: test
              customerStatus: active
              translation: 
                type: CUSTOM
                schemaName: two
                format: CSV
    """.trimIndent()

    private val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")))

    @Test
    fun `test filterOutNegativeAntigenTestUsingQualityFilter`() {
        val mySchema = Schema(
            name = "two", topic = Topic.TEST, trackingElement = "id",
            elements = listOf(
                Element("id"), Element("order_test_date"),
                Element("ordered_test_code"), Element("test_result"), Element("test_type")
            )
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(filterTestYamlFilterOutNegAntigenTestType.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 3 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("0", "20221103202920", "94531-1", "260385009", "antigen"), // Negative Antigen
                listOf("1", "20221103202921", "94531-2", "10828004", "Antigen"), // Positive Antigen
                listOf("2", "20221103202922", "94531-3", "260415000", "Antigen"), // Not Detected Antigen
                listOf("3", "20221103202923", "94531-1", "10828004", "Serology"), // Positive but NOT Antigen
                listOf("4", "20221103202924", "94531-2", "895231008", "antigen"), // Not detected in pooled specimen
                listOf("5", "20221103202925", "94531-3", "260373001", "Antigen"), // Detected (Positive) Antigen
                listOf("6", "20221103202926", "94531-1", "260385009", "Serology"), // Negative but NOT Antigen
            ),
            TestSource,
            metadata = metadata,
            itemCountBeforeQualFilter = 4,
        )
        val rcvr = settings.findReceiver("phd.elr")
        assertThat(rcvr).isNotNull()
        val org = settings.findOrganization("phd")
        assertThat(org).isNotNull()

        // Quality filter: Override the default; org filter exists.  No receiver filter.
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.QUALITY_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(4)
            assertThat(this.getRow(0)[0]).isEqualTo("1")
            assertThat(this.getRow(1)[0]).isEqualTo("3")
            assertThat(this.getRow(2)[0]).isEqualTo("5")
            assertThat(this.getRow(3)[0]).isEqualTo("6")
            assertThat(this.filteringResults.size).isEqualTo(3) // two rows eliminated, but one filter message.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("0")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo("2")
            assertThat(this.filteringResults[2].filteredTrackingElement).isEqualTo("4")
        }
    }

    @Test
    fun `test filterByOneFilterType`() {
        val mySchema = Schema(
            name = "two", topic = Topic.TEST, trackingElement = "id",
            elements = listOf(Element("id"), Element("a"), Element("b"))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(filterTestYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 3 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("0", "yes", "true"), // row 0
                listOf("1", "no", "true"),
                listOf("2", "yes", "false"),
                listOf("3", "no", "false"), // row 3
            ),
            TestSource,
            metadata = metadata,
            itemCountBeforeQualFilter = 4,
        )
        val rcvr = settings.findReceiver("phd.elr")
        assertThat(rcvr).isNotNull()
        val org = settings.findOrganization("phd")
        assertThat(org).isNotNull()
        // Juris filter: No default exists, both org and receiver exist.
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.JURISDICTIONAL_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(1)
            assertThat(this.itemCountBeforeQualFilter).isEqualTo(4)
            assertThat(this.getRow(0)[0]).isEqualTo("0") // row 0 is only one left.
            assertThat(this.filteringResults.size).isEqualTo(4) // two rows eliminated, and two filter messages.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("2")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo("3")
            assertThat(this.filteringResults[2].filteredTrackingElement).isEqualTo("1")
            assertThat(this.filteringResults[3].filteredTrackingElement).isEqualTo("3")
        }
        // Quality filter: Override the default; org filter exists.  No receiver filter.
        translator.filterByOneFilterType(
            table1, rcvr, org, ReportStreamFilterType.QUALITY_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("0")
            assertThat(this.getRow(1)[0]).isEqualTo("1")
            assertThat(this.filteringResults.size).isEqualTo(2) // two rows eliminated, but one filter message.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("2")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo("3")
        }
        // Routing filter: Override the default; No org filter. Receiver filter exists.
        translator.filterByOneFilterType(
            table1, rcvr, org, ReportStreamFilterType.ROUTING_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.getRow(0)[0]).isEqualTo("0")
            assertThat(this.getRow(1)[0]).isEqualTo("2")
            assertThat(this.filteringResults.size).isEqualTo(2) // two rows eliminated, but one filter message.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("1")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo("3")
        }
    }

    @Test
    fun `test filterByOneFilterType Defaults`() {
        val mySchema = Schema(
            name = "two", topic = Topic.TEST, trackingElement = "id",
            elements = listOf(Element("id"), Element("a"), Element("b"))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(onlyDefaultFiltersYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 3 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("0", "yes", "true"), // row 0
                listOf("1", "no", "true"),
                listOf("2", "yes", "false"),
                listOf("3", "no", "false"), // row 3
            ),
            TestSource,
            metadata = metadata,
            itemCountBeforeQualFilter = 4,
        )
        val rcvr = settings.findReceiver("xyzzy.elr")
        assertThat(rcvr).isNotNull()
        val org = settings.findOrganization("xyzzy")
        assertThat(org).isNotNull()
        // Juris filter: No default, org, or receiver filters exist.  No filtering done.
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.JURISDICTIONAL_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(4)
            assertThat(this.itemCountBeforeQualFilter).isEqualTo(4)
            // just confirm the first and last rows
            assertThat(this.getRow(0)[0]).isEqualTo("0")
            assertThat(this.getRow(1)[0]).isEqualTo("1")
            assertThat(this.getRow(2)[0]).isEqualTo("2")
            assertThat(this.getRow(3)[0]).isEqualTo("3")
            assertThat(this.filteringResults.size).isEqualTo(0) // logging turned on, but no rows eliminated.
        }
        // Quality filter: Default rules apply only.  No org or receiver level filters.  And its reversed!
        translator.filterByOneFilterType(
            table1, rcvr, org, ReportStreamFilterType.QUALITY_FILTER, mySchema.trackingElement, false
        ).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.itemCountBeforeQualFilter).isEqualTo(4)
            assertThat(this.getRow(0)[0]).isEqualTo("0")
            assertThat(this.getRow(1)[0]).isEqualTo("2")
            assertThat(this.filteringResults.size).isEqualTo(0) // no logging done.
        }
        // Routing filter: Default rules apply only.  No org or receiver level filters. No weird reversing.
        translator.filterByOneFilterType(
            table1, rcvr, org, ReportStreamFilterType.ROUTING_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.itemCountBeforeQualFilter).isEqualTo(4)
            assertThat(this.getRow(0)[0]).isEqualTo("2")
            assertThat(this.getRow(1)[0]).isEqualTo("3")
            assertThat(this.filteringResults.size).isEqualTo(2) // two rows eliminated, by one rule.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("0")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo("1")
        }
    }

    @Test
    fun `test filterByAllFilterTypes`() {
        val mySchema = Schema(
            name = "two", topic = Topic.TEST, trackingElement = "id",
            elements = listOf(Element("id"), Element("a"), Element("b"))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(filterTestYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 3 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("0", "yes", "true"), // row 0
                listOf("1", "no", "true"),
                listOf("2", "yes", "false"),
                listOf("3", "no", "false"), // row 3
            ),
            TestSource,
            metadata = metadata
        )
        val rcvr = settings.findReceiver("phd.elr")
        assertThat(rcvr).isNotNull()
        // Juris filter eliminates rows 1,2,3 (zero based), but does not create filteredItem entries.
        translator.filterByAllFilterTypes(settings, table1, rcvr!!).run {
            assertThat(this).isNotNull()
            assertThat(this!!.itemCount).isEqualTo(1)
            assertThat(this.getRow(0)[0]).isEqualTo("0") // row 0
            assertThat(this.filteringResults.size).isEqualTo(0) // three rows eliminated, but nothing logged.
        }

        val settings2 = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(onlyDefaultFiltersYaml.toByteArray()))
        }
        val rcvr2 = settings2.findReceiver("xyzzy.elr")
        assertThat(rcvr2).isNotNull()
        // No juris filtering done.
        // Default matches a = "no" in qualityFilter, but its reversed.  So original rows 1,3 eliminated, rows 0,2 kept
        // But: no logging because its reversed!
        // Not done yet!  Then keep b = "false" in routingFilter.
        // So row 2 is kept ("yes", "false")
        translator.filterByAllFilterTypes(settings2, table1, rcvr2!!).run {
            assertThat(this).isNotNull()
            assertThat(this!!.itemCount).isEqualTo(1)
            assertThat(this.itemCountBeforeQualFilter).isEqualTo(4)
            assertThat(this.getRow(0)[0]).isEqualTo("2")
            assertThat(this.filteringResults.size).isEqualTo(1) // three rows eliminated, only routingFilter message.
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("0")
        }
    }

    @Test
    fun `test filter with missing tracking element value`() {
        val mySchema = Schema(
            name = "one", topic = Topic.TEST, trackingElement = "id",
            elements = listOf(Element("id"), Element("a"))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(mySchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray()))
        }
        val org = settings.findOrganization("phd1")
        val translator = Translator(metadata, settings)
        // Table has 4 rows and 2 columns.
        val table1 = Report(
            mySchema,
            listOf(
                listOf("x", "1"),
                listOf("", "1"), // missing trackingElement value
                listOf("y", "2"),
                listOf("", "2"), // missing trackingElement value
            ),
            TestSource,
            metadata = metadata
        )
        val rcvr = settings.findReceiver("phd1.elr")
        assertThat(rcvr).isNotNull()
        assertThat(org).isNotNull()
        // Juris filter keeps data where on a == 1.  Run with logging on, to force creation of [filteringResults].
        translator.filterByOneFilterType(
            table1, rcvr!!, org!!, ReportStreamFilterType.JURISDICTIONAL_FILTER, mySchema.trackingElement, true
        ).run {
            assertThat(this).isNotNull()
            assertThat(this.itemCount).isEqualTo(2)
            assertThat(this.itemCountBeforeQualFilter).isNull()
            assertThat(this.getRow(0)[0]).isEqualTo("x") // row 0
            assertThat(this.getRow(1)[0]).isEqualTo("") // row 0
            assertThat(this.filteringResults.size).isEqualTo(2) // two rows eliminated by one filter
            assertThat(this.filteringResults[0].filteredTrackingElement).isEqualTo("y")
            assertThat(this.filteringResults[1].filteredTrackingElement).isEqualTo(
                ReportStreamFilterResult.DEFAULT_TRACKING_VALUE
            )
        }
    }

    @Test
    fun `test buildMapping`() {
        val two = Schema(name = "two", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(one, two)
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
            name = "two", topic = Topic.TEST, elements = listOf(Element("a"), Element("b", default = "x"))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(one, twoWithDefault)
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
            topic = Topic.TEST,
            elements = listOf(Element("a"), Element("c", cardinality = Element.Cardinality.ONE))
        )
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(one, three)
        val translator = Translator(metadata, FileSettings())
        translator.buildMapping(fromSchema = one, toSchema = three, defaultValues = emptyMap()).run {
            assertThat(this.useDirectly.size).isEqualTo(1)
            assertThat(this.useDirectly["a"]).isEqualTo("a")
            assertThat(this.useDefault.size).isEqualTo(0)
            assertThat(this.missing.size).isEqualTo(1)
        }
    }

    @Test
    fun `test filterAndTranslateByReceiver`() {
        val theSchema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(theSchema)
        val settings = FileSettings().also {
            it.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray()))
        }
        val translator = Translator(metadata, settings)
        val table1 = Report(
            theSchema,
            listOf(
                listOf("1", "2"), // first row of data
                listOf("3", "4"), // second row of data
            ),
            TestSource,
            metadata = metadata
        )
        translator.filterAndTranslateByReceiver(table1).run {
            assertThat(this.reports.size).isEqualTo(1)
            val (mappedTable, forReceiver) = this.reports[0]
            assertThat(mappedTable.schema).isEqualTo(table1.schema)
            assertThat(mappedTable.itemCount).isEqualTo(1)
            assertThat(forReceiver).isEqualTo(settings.receivers.toTypedArray()[0])
        }
    }

    @Test
    fun `test mappingWithReplace`() {
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