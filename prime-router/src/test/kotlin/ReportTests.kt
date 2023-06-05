package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.asFormattedString
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ReportTests {
    private val metadata = UnitTestUtils.simpleMetadata

    val rcvr = Receiver("name", "org", Topic.TEST, CustomerStatus.INACTIVE, "schema", Report.Format.CSV)

    /**
     * Create table's header
     */
    val oneWithAge = Schema(
        name = "one",
        topic = Topic.TEST,
        elements = listOf(
            Element("message_id"),
            Element("patient_age"),
            Element("specimen_collection_date_time"),
            Element("patient_dob")
        )
    )

    /**
     * Add Rows values to the table
     */
    val oneReport = Report(
        schema = oneWithAge,
        values = listOf(
            listOf("0", "100", "202110300809", "30300102"), // Good age, ... don't care -> patient_age=100
            // Bad age, good collect date, BAD DOB -> patient_age=null
            listOf("1", ")@*", "202110300809-0501", "30300101"),
            // Bad age, bad collect date, good dob -> patient_age=2
            listOf("2", "_", "202110300809", "20190101"),
            // Good age, bad collect date, bad dob -> patient_age=20
            listOf("3", "20", "adfadf", "!@!*@(7"),
            // Bad age, good collect date, good dob -> patient_age=2
            listOf("4", "0", "202110300809-0500", "20190101"),
            // Bad age, good collect data, good dob -> patient_age=10
            listOf("5", "-5", "202110300809-0502", "20111029"),
            // Good age, ... don't care -> patient_age = 40
            listOf("6", "40", "asajh", "20190101"),
            // Good age is blank, -> patient_age=null
            listOf("7", "", "asajh", "20190101")
        ),
        TestSource,
        metadata = metadata
    )

    @Test
    fun `test merge`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val report1 = Report(
            one,
            listOf(listOf("1", "2"), listOf("3", "4")),
            source = TestSource,
            metadata = metadata
        )
        val report2 = Report(
            one,
            listOf(listOf("5", "6"), listOf("7", "8")),
            source = TestSource,
            metadata = metadata
        )
        val mergedReport = Report.merge(listOf(report1, report2))
        assertThat(mergedReport.itemCount).isEqualTo(4)
        assertThat(report1.itemCount).isEqualTo(2)
        assertThat(mergedReport.getString(3, "b")).isEqualTo("8")
        assertThat(mergedReport.sources.size).isEqualTo(2)
        assertThat(report1.id).isEqualTo((mergedReport.sources[0] as ReportSource).id)
    }

    @Test
    fun `test filter`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findReportStreamFilterDefinitions("matches") ?: fail("cannot find filter")
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        assertThat(report1.itemCount).isEqualTo(2)
        val filteredReport = report1.filter(
            listOf(
                Pair(
                    jurisdictionalFilter,
                    listOf("a", "1")
                )
            ),
            rcvr,
            false,
            one.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )
        assertThat(filteredReport.schema).isEqualTo(one)
        assertThat(filteredReport.itemCount).isEqualTo(1)
        assertThat(filteredReport.getString(0, "b")).isEqualTo("2")
        assertThat(filteredReport.sources.size).isEqualTo(1)
    }

    @Test
    fun `test multiarg matches filter`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findReportStreamFilterDefinitions("matches") ?: fail("cannot find filter")
        // each sublist is a row.
        val report1 = Report(
            one,
            listOf(listOf("row1_a", "row1_b"), listOf("row2_a", "row2_b")),
            source = TestSource,
            metadata = metadata
        )
        assertThat(2).isEqualTo(report1.itemCount)
        val filteredReportA = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1.*", "row2_a"))),
            rcvr,
            false,
            one.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )
        assertThat(filteredReportA.itemCount).isEqualTo(2)
        assertThat(filteredReportA.getString(0, "b")).isEqualTo("row1_b")
        assertThat(filteredReportA.getString(1, "b")).isEqualTo("row2_b")

        val filteredReportB = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row.*"))),
            rcvr,
            false,
            one.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )
        assertThat(filteredReportA.itemCount).isEqualTo(2)
        assertThat(filteredReportB.getString(0, "b")).isEqualTo("row1_b")
        assertThat(filteredReportB.getString(1, "b")).isEqualTo("row2_b")

        val filteredReportC = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1_a", "foo", "bar", "baz"))),
            rcvr,
            false,
            one.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )
        assertThat(filteredReportC.itemCount).isEqualTo(1)
        assertThat(filteredReportC.getString(0, "b")).isEqualTo("row1_b")

        val filteredReportD = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "argle", "bargle"))),
            rcvr,
            false,
            one.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )
        assertThat(filteredReportD.itemCount).isEqualTo(0)
    }

    @Test
    fun `test isEmpty`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val emptyReport = Report(one, emptyList(), source = TestSource, metadata = metadata)
        assertThat(emptyReport.isEmpty()).isEqualTo(true)
        val report1 = Report(one, listOf(listOf("1", "2")), source = TestSource, metadata = metadata)
        assertThat(report1.isEmpty()).isEqualTo(false)
    }

    @Test
    fun `test create with list`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource, metadata = metadata)
        assertThat(report1.schema).isEqualTo(one)
        assertThat(report1.itemCount).isEqualTo(1)
        assertThat(TestSource).isEqualTo(report1.sources[0] as TestSource)
    }

    @Test
    fun `test applyMapping`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = Topic.TEST, elements = listOf(Element("b")))
        val metadata = UnitTestUtils.simpleMetadata
        metadata.loadSchemas(one, two)

        val oneReport = Report(
            schema = one,
            values = listOf(listOf("a1", "b1"), listOf("a2", "b2")),
            TestSource,
            metadata = metadata
        )
        assertThat(oneReport.itemCount).isEqualTo(2)
        val mappingOneToTwo = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())

        val twoTable = oneReport.applyMapping(mappingOneToTwo)
        assertThat(twoTable.itemCount).isEqualTo(2)
        assertThat(twoTable.getString(1, "b")).isEqualTo("b2")
    }

    @Test
    fun `test applyMapping with default`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(Element("a", default = "~"), Element("b"))
        )
        val two = Schema(name = "two", topic = Topic.TEST, elements = listOf(Element("b")))
        val metadata = UnitTestUtils.simpleMetadata
        metadata.loadSchemas(one, two)

        val twoReport = Report(
            schema = two,
            values = listOf(listOf("b1"), listOf("b2")),
            source = TestSource,
            metadata = metadata
        )
        assertThat(twoReport.itemCount).isEqualTo(2)
        val mappingTwoToOne = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap())

        val oneReport = twoReport.applyMapping(mappingTwoToOne)
        assertThat(oneReport.itemCount).isEqualTo(2)
        assertThat(oneReport.getString(0, "a")).isEqualTo("~")
        assertThat(oneReport.getString(1, "b")).isEqualTo("b2")
    }

    @Test
    fun `test patientZipentify`() {
        val one = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(Element("a", pii = true), Element("b"), Element("patient_zip_code"))
        )

        // Mock restricted_zip_code data
        val patientZip = """
            patient_zip
            036
            059
            102
            203
            205
            369
            556
            692
            821
        """.trimIndent()

        val restrictedZipTable = LookupTable.read(inputStream = ByteArrayInputStream(patientZip.toByteArray()))
        metadata.loadLookupTable("restricted_zip_code", restrictedZipTable)

        val oneReport = Report(
            schema = one,
            values = listOf(
                listOf("a1", "b1", "55555"),
                listOf("a2", "b2", "10266-1234"),
                listOf("a3", "b3", "03266-4567"),
                listOf("a4", "b3", "03655"),
                listOf("a5", "b5", "05926-9876"),
                listOf("a6", "b6", "20345-1596"),
                listOf("a7", "b7", "20589-7532"),
                listOf("a8", "b8", "36947"),
                listOf("a9", "b9", "55632-6478"),
                listOf("a10", "b10", "69283-3298"),
                listOf("a11", "b11", "82159")
            ),
            source = TestSource,
            metadata = metadata
        )

        val oneDeidentified = oneReport.deidentify("")
        assertThat(oneDeidentified.itemCount).isEqualTo(11) // Check row count
        assertThat(oneDeidentified.getString(0, "a")).isEqualTo("")
        assertThat(oneDeidentified.getString(0, "b")).isEqualTo("b1")
        assertThat(oneDeidentified.getString(0, "patient_zip_code")).isEqualTo("55500")
        assertThat(oneDeidentified.getString(1, "patient_zip_code")).isEqualTo("00000") // 102
        assertThat(oneDeidentified.getString(2, "patient_zip_code")).isEqualTo("03200")
        assertThat(oneDeidentified.getString(3, "patient_zip_code")).isEqualTo("00000") // 036
        assertThat(oneDeidentified.getString(4, "patient_zip_code")).isEqualTo("00000") // 059
        assertThat(oneDeidentified.getString(5, "patient_zip_code")).isEqualTo("00000") // 203
        assertThat(oneDeidentified.getString(6, "patient_zip_code")).isEqualTo("00000") // 205
        assertThat(oneDeidentified.getString(7, "patient_zip_code")).isEqualTo("00000") // 369
        assertThat(oneDeidentified.getString(8, "patient_zip_code")).isEqualTo("00000") // 556
        assertThat(oneDeidentified.getString(9, "patient_zip_code")).isEqualTo("00000") // 692
        assertThat(oneDeidentified.getString(10, "patient_zip_code")).isEqualTo("00000") // 821

        val two = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(Element("a", pii = true), Element("b"))
        )

        val twoReport = Report(
            schema = two,
            values = listOf(listOf("a1", "b1"), listOf("", "b2")),
            source = TestSource,
            metadata = metadata
        )

        val twoDeidentified = twoReport.deidentify("TEST")
        assertThat(twoDeidentified.itemCount).isEqualTo(2)
        assertThat(twoDeidentified.getString(0, "a")).isEqualTo("TEST")
        assertThat(twoDeidentified.getString(1, "a")).isEqualTo("")
        assertThat(twoDeidentified.getString(0, "b")).isEqualTo("b1")

        val oneDeidentifiedBlank = twoReport.deidentify("")
        assertThat(oneDeidentifiedBlank.itemCount).isEqualTo(2)
        assertThat(oneDeidentifiedBlank.getString(0, "a")).isEqualTo("")
        assertThat(oneDeidentifiedBlank.getString(1, "a")).isEqualTo("")
        assertThat(oneDeidentifiedBlank.getString(0, "b")).isEqualTo("b1")
    }

    @Test
    fun `test patient age deidentification`() {
        val patientAgeSchema = Schema(
            name = "patientAgeSchema",
            topic = Topic.TEST,
            elements = listOf(
                Element("patient_age", pii = true),
                Element("patient_dob", pii = true),
                Element("specimen_collection_date_time", pii = false)
            )
        )
        Report(
            schema = patientAgeSchema,
            values = listOf(
                // empty values
                listOf("", "", ""),
                // just specimen collection date
                listOf("", "", "2022-06-22 22:58:00"),
                // collection date and dob
                listOf("", "2021-06-21", "2022-06-22 22:58:00"),
                // just DOB
                listOf(
                    "",
                    DateUtilities
                        .nowAtZone(DateUtilities.utcZone)
                        .minusYears(2)
                        .asFormattedString("yyyy-MM-dd", false),
                    ""
                ),
                listOf(
                    "",
                    DateUtilities
                        .nowAtZone(DateUtilities.utcZone)
                        .minusYears(90)
                        .asFormattedString("yyyy-MM-dd", false),
                    ""
                ),
                listOf("10", "", ""),
                listOf("89", "", "")
            ),
            source = TestSource,
            metadata = metadata
        ).deidentify("<NULL>").run {
            assertThat(this.getString(0, "patient_age")).isEqualTo("<NULL>")
            assertThat(this.getString(1, "patient_age")).isEqualTo("<NULL>")
            assertThat(this.getString(2, "patient_age")).isEqualTo("1")
            assertThat(this.getString(3, "patient_age")).isEqualTo("2")
            assertThat(this.getString(4, "patient_age")).isEqualTo("0")
            assertThat(this.getString(5, "patient_age")).isEqualTo("10")
            assertThat(this.getString(6, "patient_age")).isEqualTo("0")
        }
    }

    @Test
    fun `test patient dob deidentification`() {
        val patientAgeSchema = Schema(
            name = "patientAgeSchema",
            topic = Topic.TEST,
            elements = listOf(
                Element("patient_age", pii = true),
                Element("patient_dob", pii = true),
                Element("specimen_collection_date_time", pii = false)
            )
        )
        Report(
            schema = patientAgeSchema,
            values = listOf(
                // empty values
                listOf("", "", ""),
                // should be deidentified
                listOf("", "1923-08-03", "2022-06-22 22:58:00"),
                // collection date and dob
                listOf("", "2000-12-01", "2022-06-22 22:58:00")
            ),
            source = TestSource,
            metadata = metadata
        ).deidentify("<NULL>").run {
            assertThat(this.getString(0, "patient_dob")).isEqualTo("<NULL>")
            assertThat(this.getString(1, "patient_dob")).isEqualTo("0000")
            assertThat(this.getString(2, "patient_dob")).isEqualTo("2000")
        }
    }

    @Test
    fun `test patient age validation`() {
        val covidResultMetadata = oneReport.getDeidentifiedResultMetaData()
        assertThat(covidResultMetadata).isNotNull()
        assertThat(covidResultMetadata[0].patientAge).isEqualTo("100")
        assertThat(covidResultMetadata[1].patientAge).isNull()
        assertThat(covidResultMetadata[2].patientAge).isEqualTo("2")
        assertThat(covidResultMetadata[3].patientAge).isEqualTo("20")
        assertThat(covidResultMetadata[4].patientAge).isEqualTo("2")
        assertThat(covidResultMetadata[5].patientAge).isEqualTo("10")
        assertThat(covidResultMetadata[6].patientAge).isEqualTo("40")
        assertThat(covidResultMetadata[7].patientAge).isNull()

        /**
         * Test table without patient_age
         */
        val twoWithoutAge = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("message_id"),
                Element("specimen_collection_date_time"),
                Element("patient_dob")
            )
        )

        /**
         * Add Rows values to the table
         */
        val twoReport = Report(
            schema = twoWithoutAge,
            values = listOf(
                listOf("0", "202110300809", "30300102"), // Bad specimen collection date -> patient_age=null
                listOf("1", "202110300809-0501", "30300101"), // good collect date, BAD DOB -> patient_age=null
                listOf("2", "202110300809-0500", "20190101")
            ), // Bad age, good collect date, good dob -> patient_age=2
            TestSource,
            metadata = metadata
        )

        val covidResultMetadata2 = twoReport.getDeidentifiedResultMetaData()
        assertThat(covidResultMetadata2).isNotNull()
        assertThat(covidResultMetadata2.get(0).patientAge).isNull()
        assertThat(covidResultMetadata2.get(1).patientAge).isNull()
        assertThat(covidResultMetadata2.get(2).patientAge).isEqualTo("2")
    }

    @Test
    fun `test covid metadata output`() {
        val covidResultMetadata = oneReport.getDeidentifiedResultMetaData()
        assertThat(covidResultMetadata).isNotNull()
        // there should never be a report index of 0 in covid result metadata, row indexing should start at 1
        assertThat(covidResultMetadata.filter { it.reportIndex == 0 }.size).isEqualTo(0)
        assertThat(covidResultMetadata.get(0).reportIndex).isEqualTo(1)
        assertThat(covidResultMetadata.get(1).reportIndex).isEqualTo(2)
        assertThat(covidResultMetadata.get(7).reportIndex).isEqualTo(8)
        assertThat(covidResultMetadata.filter { it.reportIndex == 9 }.size).isEqualTo(0)
    }

    // Tests for Item lineage
    @Test
    fun `test merge item lineage`() {
        val schema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(
            schema,
            listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")),
            source = TestSource,
            metadata = metadata
        )
        val report2 = Report(
            schema,
            listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")),
            source = TestSource,
            metadata = metadata
        )

        val merged = Report.merge(listOf(report1, report2))

        assertThat(merged.itemLineages!!.size).isEqualTo(4)
        val firstLineage = merged.itemLineages!![0]
        assertThat(firstLineage.parentReportId).isEqualTo(report1.id)
        assertThat(firstLineage.parentIndex).isEqualTo(1)
        assertThat(firstLineage.childReportId).isEqualTo(merged.id)
        assertThat(firstLineage.childIndex).isEqualTo(1)
        assertThat(firstLineage.trackingId).isEqualTo("rep1_row1_a")

        val lastLineage = merged.itemLineages!![3]
        assertThat(lastLineage.parentReportId).isEqualTo(report2.id)
        assertThat(lastLineage.parentIndex).isEqualTo(2)
        assertThat(lastLineage.childReportId).isEqualTo(merged.id)
        assertThat(lastLineage.childIndex).isEqualTo(4)
        assertThat(lastLineage.trackingId).isEqualTo("rep2_row2_a")
    }

    @Test
    fun `test split item lineage`() {
        val schema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(
            schema,
            listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")),
            source = TestSource,
            metadata = metadata
        )

        val reports = report1.split()

        assertThat(reports.size).isEqualTo(2)
        assertThat(reports[0].itemLineages!!.size).isEqualTo(1)
        assertThat(reports[1].itemLineages!!.size).isEqualTo(1)

        val firstLineage = reports[0].itemLineages!![0]
        assertThat(firstLineage.parentReportId).isEqualTo(report1.id)
        assertThat(firstLineage.parentIndex).isEqualTo(1)
        assertThat(firstLineage.childReportId).isEqualTo(reports[0].id)
        assertThat(firstLineage.childIndex).isEqualTo(1)
        assertThat(firstLineage.trackingId).isEqualTo("rep1_row1_a")

        val secondLineage = reports[1].itemLineages!![0]
        assertThat(secondLineage.parentReportId).isEqualTo(report1.id)
        assertThat(secondLineage.parentIndex).isEqualTo(2)
        assertThat(secondLineage.childReportId).isEqualTo(reports[1].id)
        assertThat(secondLineage.childIndex).isEqualTo(1)
        assertThat(secondLineage.trackingId).isEqualTo("rep1_row2_a")
    }

    @Test
    fun `test item lineage after jurisdictional filter`() {
        val schema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")), trackingElement = "a")
        val metadata = Metadata(schema = schema)
        val jurisdictionalFilter = metadata.findReportStreamFilterDefinitions("matches") ?: fail("cannot find filter")
        // each sublist is a row.
        val report1 = Report(
            schema,
            listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")),
            source = TestSource,
            metadata = metadata
        )

        val filteredReport = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "rep1_row2_a"))),
            rcvr,
            false,
            schema.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )

        val lineage = filteredReport.itemLineages!!
        assertThat(lineage.size).isEqualTo(1)
        assertThat(lineage[0].parentReportId).isEqualTo(report1.id)
        assertThat(lineage[0].parentIndex).isEqualTo(2)
        assertThat(lineage[0].childReportId).isEqualTo(filteredReport.id)
        assertThat(lineage[0].childIndex).isEqualTo(1)
        assertThat(lineage[0].trackingId).isEqualTo("rep1_row2_a")
    }

    @Test
    fun `test merge then split`() {
        val schema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(
            schema,
            listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")),
            source = TestSource,
            metadata = metadata
        )
        val report2 = Report(
            schema,
            listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")),
            source = TestSource,
            metadata = metadata
        )

        val merged = Report.merge(listOf(report1, report2))
        val reports = merged.split()

        assertThat(reports.size).isEqualTo(4)
        assertThat(reports[0].itemLineages!!.size).isEqualTo(1)
        assertThat(reports[3].itemLineages!!.size).isEqualTo(1)

        val firstLineage = reports[0].itemLineages!![0]
        assertThat(firstLineage.parentReportId).isEqualTo(report1.id)
        assertThat(firstLineage.parentIndex).isEqualTo(1)
        assertThat(firstLineage.childReportId).isEqualTo(reports[0].id)
        assertThat(firstLineage.childIndex).isEqualTo(1)
        assertThat(firstLineage.trackingId).isEqualTo("rep1_row1_a")

        val fourthLineage = reports[3].itemLineages!![0]
        assertThat(fourthLineage.parentReportId).isEqualTo(report2.id)
        assertThat(fourthLineage.parentIndex).isEqualTo(2)
        assertThat(fourthLineage.childReportId).isEqualTo(reports[3].id)
        assertThat(fourthLineage.childIndex).isEqualTo(1)
        assertThat(fourthLineage.trackingId).isEqualTo("rep2_row2_a")
    }

    @Test
    fun `test lineage insanity`() {
        val schema = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(
            schema,
            listOf(listOf("bbb"), listOf("aaa"), listOf("aaa")),
            source = TestSource,
            metadata = metadata
        )
        val metadata = Metadata(schema = schema)
        val jurisdictionalFilter = metadata.findReportStreamFilterDefinitions("matches") ?: fail("cannot find filter")

        // split, merge, split, merge, copy, copy, then filter.
        val reports1 = report1.split()
        val merge1 = Report.merge(reports1)
        val reports2 = merge1.split()
        val merge2 = Report.merge(reports2)
        val copy1 = merge2.copy()
        val copy2 = copy1.copy()
        val filteredReport = copy2.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "aaa"))),
            rcvr,
            false,
            schema.trackingElement,
            false,
            ReportStreamFilterType.JURISDICTIONAL_FILTER
        )

        val lineage = filteredReport.itemLineages!!
        assertThat(lineage.size).isEqualTo(2)

        assertThat(lineage[0].parentReportId).isEqualTo(report1.id)
        assertThat(lineage[0].parentIndex).isEqualTo(2)
        assertThat(lineage[0].childReportId).isEqualTo(filteredReport.id)
        assertThat(lineage[0].childIndex).isEqualTo(1)
        assertThat(lineage[0].trackingId).isEqualTo("aaa")

        assertThat(lineage[1].parentReportId).isEqualTo(report1.id)
        assertThat(lineage[1].parentIndex).isEqualTo(3)
        assertThat(lineage[1].childReportId).isEqualTo(filteredReport.id)
        assertThat(lineage[1].childIndex).isEqualTo(2)
        assertThat(lineage[1].trackingId).isEqualTo("aaa")
    }

    @Test
    fun `test synthesize data with empty strategy map`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = Topic.TEST,
            elements = listOf(
                Element("last_name"),
                Element("first_name")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(listOf("smith", "sarah"), listOf("jones", "mary"), listOf("white", "roberta")),
            source = TestSource,
            metadata = metadata
        )
        // act
        val synthesizedReport = report.synthesizeData(metadata = metadata)
        // assert
        assertThat(synthesizedReport.itemCount).isEqualTo(3)
        assertThat(synthesizedReport.getString(0, "last_name")).isEqualTo("smith")
        assertThat(synthesizedReport.getString(1, "last_name")).isEqualTo("jones")
        assertThat(synthesizedReport.getString(2, "last_name")).isEqualTo("white")
        assertThat(synthesizedReport.getString(0, "first_name")).isEqualTo("sarah")
        assertThat(synthesizedReport.getString(1, "first_name")).isEqualTo("mary")
        assertThat(synthesizedReport.getString(2, "first_name")).isEqualTo("roberta")
    }

    @Test
    fun `test synthesize data with pass through strategy map`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = Topic.TEST,
            elements = listOf(
                Element("last_name"),
                Element("first_name")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(listOf("smith", "sarah"), listOf("jones", "mary"), listOf("white", "roberta")),
            source = TestSource,
            metadata = metadata
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "first_name" to Report.SynthesizeStrategy.PASSTHROUGH
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        assertThat(synthesizedReport.itemCount).isEqualTo(3)
        assertThat(synthesizedReport.getString(0, "last_name")).isEqualTo("smith")
        assertThat(synthesizedReport.getString(1, "last_name")).isEqualTo("jones")
        assertThat(synthesizedReport.getString(2, "last_name")).isEqualTo("white")
        assertThat(synthesizedReport.getString(0, "first_name")).isEqualTo("sarah")
        assertThat(synthesizedReport.getString(1, "first_name")).isEqualTo("mary")
        assertThat(synthesizedReport.getString(2, "first_name")).isEqualTo("roberta")
    }

    @Test
    fun `test synthesize data with blank strategy`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = Topic.TEST,
            elements = listOf(
                Element("last_name"),
                Element("first_name"),
                Element("ssn")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(
                listOf("smith", "sarah", "000000000"),
                listOf("jones", "mary", "000000000"),
                listOf("white", "roberta", "000000000")
            ),
            source = TestSource,
            metadata = metadata
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "first_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "ssn" to Report.SynthesizeStrategy.BLANK
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        assertThat(synthesizedReport.itemCount).isEqualTo(3)
        assertThat(synthesizedReport.getString(0, "last_name")).isEqualTo("smith")
        assertThat(synthesizedReport.getString(1, "last_name")).isEqualTo("jones")
        assertThat(synthesizedReport.getString(2, "last_name")).isEqualTo("white")
        assertThat(synthesizedReport.getString(0, "first_name")).isEqualTo("sarah")
        assertThat(synthesizedReport.getString(1, "first_name")).isEqualTo("mary")
        assertThat(synthesizedReport.getString(2, "first_name")).isEqualTo("roberta")
        assertThat(synthesizedReport.getString(0, "ssn")).isEqualTo("")
        assertThat(synthesizedReport.getString(1, "ssn")).isEqualTo("")
        assertThat(synthesizedReport.getString(2, "ssn")).isEqualTo("")
    }

    // ignoring this test for now because shuffling is non-deterministic
    @Test
    @Ignore
    fun `test synthesize data with shuffle strategy`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = Topic.TEST,
            elements = listOf(
                Element("last_name"),
                Element("first_name")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(
                listOf("smith", "sarah"),
                listOf("jones", "mary"),
                listOf("white", "roberta"),
                listOf("stock", "julie"),
                listOf("chang", "emily"),
                listOf("rodriguez", "anna")
            ),
            source = TestSource
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.SHUFFLE,
            "first_name" to Report.SynthesizeStrategy.SHUFFLE
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        assertThat(synthesizedReport.getString(0, "last_name")).isNotEqualTo("smith")
        assertThat(synthesizedReport.getString(1, "last_name")).isNotEqualTo("jones")
        assertThat(synthesizedReport.getString(2, "last_name")).isNotEqualTo("white")
        assertThat(synthesizedReport.getString(0, "first_name")).isNotEqualTo("sarah")
        assertThat(synthesizedReport.getString(1, "first_name")).isNotEqualTo("mary")
        assertThat(synthesizedReport.getString(2, "first_name")).isNotEqualTo("roberta")
    }

    @Test
    fun `test setString`() {
        // arrange
        val schema = Schema(
            name = "one",
            topic = Topic.TEST,
            elements = listOf(
                Element("first_name"), // pii =  true
                Element("test_time"), // type = DATETIME
                Element("specimen_id"), // type = ID
                Element("observation") // type = TEXT
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(
                listOf("blue", "202110300809-0501", "2039784", "observationvalue"),
                listOf("", "", "", ""),
                listOf("green", "202110300809-0501", "123Fake", "words123"),
                listOf("red", "null", "null", "null"),
                listOf("black", "202110300809-0501", "!@#Fake", "asdlkj123!@#")
            ),
            source = TestSource,
            metadata = metadata
        )

        report.setString(1, "first_name", "square")
        report.setString(2, "test_time", "20220101")
        report.setString(3, "specimen_id", "")
        report.setString(4, "observation", "null")

        val firstName = report.getString(1, "first_name")
        val testTime = report.getString(2, "test_time")
        val specimenId = report.getString(3, "specimen_id")
        val observation = report.getString(4, "observation")

        assertThat(firstName).isEqualTo("square")
        assertThat(testTime).isEqualTo("20220101")
        assertThat(specimenId).isEqualTo("")
        assertThat(observation).isEqualTo("null")
    }

    @Test
    fun `test format from extension`() {
        var format = Report.Format.valueOfFromExt("csv")
        assertThat(format).isEqualTo(Report.Format.CSV)

        format = Report.Format.valueOfFromExt("fhir")
        assertThat(format).isEqualTo(Report.Format.FHIR)

        format = Report.Format.valueOfFromExt("hl7")
        assertThat(format).isEqualTo(Report.Format.HL7)

        try {
            format = Report.Format.valueOfFromExt("txt")
            fail("Expected IllegalArgumentException, instead got $format.")
        } catch (e: IllegalArgumentException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `test generateReportAndUploadBlob errors`() {
        val mockActionHistory = mockk<ActionHistory>()
        val mockMetadata = mockk<Metadata>()

        // No message body
        assertThat {
            Report.generateReportAndUploadBlob(
                Event.EventAction.BATCH,
                "".toByteArray(),
                listOf(UUID.randomUUID()),
                rcvr,
                mockMetadata,
                mockActionHistory,
                topic = Topic.FULL_ELR,
            )
        }.isFailure().hasClass(java.lang.IllegalStateException::class.java)

        // No report ID
        assertThat {
            Report.generateReportAndUploadBlob(
                Event.EventAction.BATCH,
                UUID.randomUUID().toString().toByteArray(),
                listOf(),
                rcvr,
                mockMetadata,
                mockActionHistory,
                topic = Topic.FULL_ELR,
            )
        }.isFailure().hasClass(java.lang.IllegalStateException::class.java)

        // Invalid receiver type
        assertThat {
            Report.generateReportAndUploadBlob(
                Event.EventAction.BATCH,
                UUID.randomUUID().toString().toByteArray(),
                listOf(UUID.randomUUID()),
                rcvr,
                mockMetadata,
                mockActionHistory,
                topic = Topic.FULL_ELR,
            )
        }.isFailure().hasClass(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `test generateReportAndUploadBlob for hl7`() {
        val mockMetadata = mockk<Metadata>() {
            every { fileNameTemplates } returns emptyMap()
        }
        val mockActionHistory = mockk<ActionHistory>() {
            every { trackCreatedReport(any(), any(), any()) } returns Unit
        }
        val hl7MockData = UUID.randomUUID().toString().toByteArray() // Just some data
        val receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            )
        )

        // Now test single report
        mockkObject(BlobAccess)
        every {
            BlobAccess.uploadBody(
                Report.Format.HL7, hl7MockData, any(), any(), Event.EventAction.PROCESS
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.HL7, "someurl", "digest".toByteArray())

        var reportIds = listOf(ReportId.randomUUID())
        val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
            Event.EventAction.PROCESS, hl7MockData, reportIds, receiver, mockMetadata, mockActionHistory,
            topic = Topic.FULL_ELR,
        )
        unmockkObject(BlobAccess)

        assertThat(report.bodyFormat).isEqualTo(Report.Format.HL7)
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.destination).isNotNull()
        assertThat(report.destination!!.name).isEqualTo(receiver.name)
        assertThat(report.itemLineages).isNotNull()
        assertThat(report.itemLineages!!.size).isEqualTo(1)
        assertThat(event.eventAction).isEqualTo(Event.EventAction.PROCESS)
        assertThat(blobInfo.blobUrl).isEqualTo("someurl")

        // Multiple reports
        reportIds = listOf(ReportId.randomUUID(), ReportId.randomUUID(), ReportId.randomUUID())
        mockkObject(BlobAccess)
        every {
            BlobAccess.uploadBody(
                Report.Format.HL7_BATCH, hl7MockData, any(), any(), Event.EventAction.SEND
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.HL7_BATCH, "someurl", "digest".toByteArray())
        val (report2, event2, _) = Report.generateReportAndUploadBlob(
            Event.EventAction.SEND, hl7MockData, reportIds, receiver, mockMetadata, mockActionHistory,
            topic = Topic.FULL_ELR,
        )
        unmockkObject(BlobAccess)
        assertThat(report2.bodyFormat).isEqualTo(Report.Format.HL7_BATCH)
        assertThat(report2.itemCount).isEqualTo(3)
        assertThat(report2.itemLineages).isNotNull()
        assertThat(report2.itemLineages!!.size).isEqualTo(3)
        assertThat(event2.eventAction).isEqualTo(Event.EventAction.SEND)
    }

    @Test
    fun `test generateReportAndUploadBlob for fhir`() {
        val mockMetadata = mockk<Metadata>() {
            every { fileNameTemplates } returns emptyMap()
        }
        val mockActionHistory = mockk<ActionHistory>() {
            every { trackCreatedReport(any(), any(), any()) } returns Unit
        }
        val fhirMockData = UUID.randomUUID().toString().toByteArray() // Just some data
        val receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = FHIRConfiguration(receivingOrganization = null)
        )

        // Now test single report
        mockkObject(BlobAccess)
        every {
            BlobAccess.uploadBody(
                Report.Format.FHIR, fhirMockData, any(), any(), Event.EventAction.PROCESS
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.FHIR, "someurl", "digest".toByteArray())

        var reportIds = listOf(ReportId.randomUUID())
        val (report, event, blobInfo) = Report.generateReportAndUploadBlob(
            Event.EventAction.PROCESS, fhirMockData, reportIds, receiver, mockMetadata, mockActionHistory,
            topic = Topic.FULL_ELR,
        )
        unmockkObject(BlobAccess)

        assertThat(report.bodyFormat).isEqualTo(Report.Format.FHIR)
        assertThat(report.itemCount).isEqualTo(1)
        assertThat(report.destination).isNotNull()
        assertThat(report.destination!!.name).isEqualTo(receiver.name)
        assertThat(report.itemLineages).isNotNull()
        assertThat(report.itemLineages!!.size).isEqualTo(1)
        assertThat(event.eventAction).isEqualTo(Event.EventAction.PROCESS)
        assertThat(blobInfo.blobUrl).isEqualTo("someurl")

        // Multiple reports
        reportIds = listOf(ReportId.randomUUID(), ReportId.randomUUID(), ReportId.randomUUID())
        mockkObject(BlobAccess)
        every {
            BlobAccess.uploadBody(
                Report.Format.FHIR, fhirMockData, any(), any(), Event.EventAction.SEND
            )
        } returns
            BlobAccess.BlobInfo(Report.Format.FHIR, "someurl", "digest".toByteArray())
        val (report2, event2, _) = Report.generateReportAndUploadBlob(
            Event.EventAction.SEND, fhirMockData, reportIds, receiver, mockMetadata, mockActionHistory,
            topic = Topic.FULL_ELR,
        )
        unmockkObject(BlobAccess)
        assertThat(report2.bodyFormat).isEqualTo(Report.Format.FHIR)
        assertThat(report2.itemCount).isEqualTo(3)
        assertThat(report2.itemLineages).isNotNull()
        assertThat(report2.itemLineages!!.size).isEqualTo(3)
        assertThat(event2.eventAction).isEqualTo(Event.EventAction.SEND)
    }
}

class OptionTests {
    @Test
    fun `test valueOfOrNone`() {
        val option = Options.valueOfOrNone("SkipSend")
        assertThat(option).equals(Options.SkipSend)

        val deprecatedOption = Options.valueOfOrNone("SkipInvalidItems")
        assertThat(deprecatedOption).equals(Options.SkipInvalidItems)

        val noneOption = Options.valueOfOrNone("None")
        assertThat(noneOption).equals(Options.None)

        val invalidOption = "INVALID OPTION"
        assertFailsWith<Options.InvalidOptionException>() { Options.valueOfOrNone(invalidOption) }
    }

    @Test
    fun `test isDeprecated`() {
        val deprecatedOption = Options.valueOfOrNone("SkipInvalidItems")
        assertThat(deprecatedOption.isDeprecated).isTrue()

        val option = Options.valueOfOrNone("SkipSend")
        assertThat(option.isDeprecated).isFalse()
    }
}