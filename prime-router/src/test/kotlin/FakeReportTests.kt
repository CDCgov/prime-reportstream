package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkClass
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.fail

class FakeReportTests {
    private val schemaName = "test"
    private val metadata = Metadata(
        schema = UnitTestUtils.simpleSchema,
        valueSet = ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ"))),
    ).loadSchemas(
        Schema(
            schemaName, Topic.TEST,
            listOf(
                Element("name_of_testing_lab", type = Element.Type.TEXT),
                Element("facility_name", type = Element.Type.TEXT),
                Element("patient_state", type = Element.Type.CODE, valueSet = "fake"),
                Element("patient_phone", type = Element.Type.TELEPHONE),
                Element(
                    "formatted_patient_phone", type = Element.Type.TELEPHONE,
                    csvFields = listOf(
                        Element.CsvField("Patient Phone", "##########")
                    )
                ),
                Element("postal_code", type = Element.Type.POSTAL_CODE),
                Element("patient_ssn", type = Element.Type.ID_SSN),
                Element("default_date", type = Element.Type.DATE),
                Element("blank_column", type = Element.Type.BLANK),
                // a use mapper element
                Element("testing_lab_name", type = Element.Type.TEXT, mapper = "use(name_of_testing_lab)"),
                Element(
                    "testing_lab_and_facility",
                    type = Element.Type.TEXT,
                    mapper = "concat(name_of_testing_lab, facility_name)",
                    delimiter = "^"
                ),
                Element(
                    "testing_lab_and_facility2",
                    type = Element.Type.TEXT,
                    mapper = "concat(name_of_testing_lab, facility_name)"
                ),
            )
        )
    )
    private val rowContext = FakeReport.RowContext(metadata, schemaName = schemaName)
    private val fipsCounty = "fips-county"
    private val zipCodeData = "zip-code-data"
    private val ncesId = "nces_id"

    // Setup Mock data and known expected returns
    val zipCodeTable = """
            city,state,county,zipcode,state_abbr,state_fips
            New site,Alabama,Tallapoosa,35010,AL,1
    """.trimIndent()
    val zipCodeLookupTable = LookupTable.read(
        zipCodeData,
        inputStream = ByteArrayInputStream(zipCodeTable.toByteArray())
    )
    val ncesTable = """
            lzip, ncesid, schname 
            35010, 010003000001, Alexander City Middle School
    """.trimIndent()
    val ncesLookupTable = LookupTable.read(
        ncesId,
        inputStream = ByteArrayInputStream(ncesTable.toByteArray())
    )

    val orderingFacilityNameElement = Element(
        "ordering_facility_name",
        type = Element.Type.TEXT
    )
    val siteOfCareElement = Element(
        "site_of_care",
        type = Element.Type.TEXT
    )

    @Test
    fun `test blank column is blank`() {
        val blankColumn = metadata
            .findSchema("test")
            ?.findElement("blank_column")
            ?: fail("Lookup failure: blank_column")
        val fakeValue = FakeReport(metadata).buildColumn(blankColumn, rowContext)
        assertThat(fakeValue).isEqualTo("")
    }

    @Test
    fun `test a coded fake`() {
        val state = metadata.findSchema("test")?.findElement("patient_state") ?: fail("Lookup failure: patient_state")
        val fakeValue = FakeReport(metadata).buildColumn(state, rowContext)
        assertThat(fakeValue).isEqualTo("AZ")
    }

    @Test
    fun `test phone matches default pattern`() {
        val patientPhone = metadata
            .findSchema("test")
            ?.findElement("patient_phone") ?: fail("Lookup failure: patient_phone")
        val fakedNumber = FakeReport(metadata).buildColumn(patientPhone, rowContext)
        // default format for phones in FakeReport is "##########:1:". checking for that here
        // todo: update for different formats as we expand the offerings for other consumers
        val phoneRegex = "\\d{10}:1:".toRegex()

        assertThat(phoneRegex.matches(fakedNumber)).isTrue()
    }

    @Test
    fun `test phone matches specified pattern`() {
        val formattedPatientPhone = metadata
            .findSchema("test")
            ?.findElement("formatted_patient_phone") ?: fail("Lookup failure: formatted_patient_phone")
        val fakedNumber = FakeReport(metadata).buildColumn(formattedPatientPhone, rowContext)
        // default format for phones in FakeReport is "##########:1:". checking for that here
        // todo: update for different formats as we expand the offerings for other consumers
        val phoneRegex = "\\d{10}".toRegex()

        assertThat(phoneRegex.matches(fakedNumber)).isTrue()
    }

    // todo: update when we provide different formats for different consumers
    @Test
    fun `test postal code matches pattern expected`() {
        val postalCodeElement = metadata.findSchema("test")
            ?.findElement("postal_code") ?: fail("Lookup failure: postal_code")
        val fakedPostalCode = FakeReport(metadata).buildColumn(postalCodeElement, rowContext)
        val postalCodeRegex = "^\\d{5}$".toRegex()
        val postalCodeRegex2 = "^\\d{5}-\\d{4}$".toRegex()

        assertThat(
            postalCodeRegex.matches(fakedPostalCode) || postalCodeRegex2.matches(fakedPostalCode)
        ).isTrue()
    }

    @Test
    fun `test default date format matches`() {
        val defaultDateElement = metadata.findSchema("test")
            ?.findElement("default_date") ?: fail("Lookup failure: default_date")
        val fakedDate = FakeReport(metadata).buildColumn(defaultDateElement, rowContext)
        val defaultDateFormatRegex = "^\\d{8}$".toRegex()

        assertThat(defaultDateFormatRegex.matches(fakedDate)).isTrue()
    }

    @Test
    fun `test passing state abbreviation into row context`() {
        // arrange
        val csv = """
            FIPS,County,State
            04017,Navajo,AZ
            04019,Pima,AZ
            04021,Pinal,AZ
            04023,Santa Cruz,AZ
            12077,Liberty,FL
            12079,Madison,FL
            12081,Manatee,FL
            12083,Marion,FL
            42005,Armstrong,PA
            42007,Beaver,PA
            42009,Bedford,PA
            42011,Berks,PA
            48117,Deaf Smith,TX
            48119,Delta,TX
            48121,Denton,TX
            48123,De Witt,TX
        """.trimIndent()

        val fipsCountyTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val flRowContext = FakeReport.RowContext(metadata, "FL")
        val orderingFacilityStateElement = Element(
            "ordering_facility_state",
            type = Element.Type.TABLE,
            table = fipsCounty,
            cardinality = Element.Cardinality.ONE,
            tableColumn = "State",
            tableRef = fipsCountyTable
        )
        // add the look up table for fips county
        val metadata = metadata.loadLookupTable(fipsCounty, fipsCountyTable)

        // act
        val states = (1..10).map { _ ->
            FakeReport(metadata).buildColumn(orderingFacilityStateElement, flRowContext)
        }

        val setOfStates = states.toSet()

        // assert
        assertThat(setOfStates.contains("FL")).isTrue()
        assertThat(setOfStates.count() == 1).isTrue()
    }

    @Test
    fun `testBuildOrderFacilityNameAndSiteOfCareColumnIncludeNCESFacilityFlag`() {

        // Setup to return expected mock data
        val mockMetadata = mockkClass(Metadata::class)
        every { mockMetadata.findLookupTable(fipsCounty) } returns null
        every { mockMetadata.findLookupTable(zipCodeData) } returns zipCodeLookupTable
        every { mockMetadata.findLookupTable(ncesId) } returns ncesLookupTable

        // Build RowContext test case: given reportState = "AL", reportCounty = "Tallapposa",
        // .. and includeNcesFacilities = true
        val alRowContext = FakeReport.RowContext(
            mockMetadata,
            "AL",
            reportCounty = "Tallapoosa",
            includeNcesFacilities = true
        )

        // assert expected value of alRowContext.facilitiesName = "Alexander City Middle School"
        assertThat(alRowContext.facilitiesName?.contains("Alexander City Middle School"))

        // Test: Build the "ording_facility_name" column.
        val orderingFacilityName = FakeReport(mockMetadata).buildColumn(orderingFacilityNameElement, alRowContext)

        // assert expected return value of orderingFacilityName: "Alexander City Middle School"
        assertThat(orderingFacilityName.contains("Alexander City Middle School")).isTrue()

        // Test: Build the site_of_care column.
        val siteOfCare = FakeReport(mockMetadata).buildColumn(siteOfCareElement, alRowContext)

        // assert expected return value of siteOfCare = "k12"
        assertThat(siteOfCare.contains("k12")).isTrue()
    }

    @Test
    fun `testFaileBuildOrderFacilityNameAndSiteOfCareColumnIncludeNCESFacilityFlag`() {

        // Setup to return expected mock data
        val mockMetadata = mockkClass(Metadata::class)
        every { mockMetadata.findLookupTable(fipsCounty) } returns null
        every { mockMetadata.findLookupTable(zipCodeData) } returns zipCodeLookupTable
        every { mockMetadata.findLookupTable(ncesId) } returns ncesLookupTable

        // Build RowContext test case: given reportState = "XY", reportCounty = "fakeCountry",
        // .. and includeNcesFacilities = true
        val alRowContext = FakeReport.RowContext(
            mockMetadata,
            "XY",
            reportCounty = "fakeCountry",
            includeNcesFacilities = true
        )

        // assert expected value of alRowContext.facilitiesName = null
        assertThat(alRowContext.facilitiesName).isNull()

        // Test: Build the "ording_facility_name" column.
        val orderingFacilityName = FakeReport(mockMetadata).buildColumn(orderingFacilityNameElement, alRowContext)

        // assert expected return value of orderingFacilityName: "Any facility USA"
        assertThat(orderingFacilityName.contains("Any facility USA")).isTrue()

        // Test: Build the site_of_care column.
        val siteOfCare = FakeReport(mockMetadata).buildColumn(siteOfCareElement, alRowContext)

        // assert expected return value of siteOfCare != "k12"
        assertThat(siteOfCare.contains("k12")).isFalse()
    }

    @Test
    fun `testBuildOrderFacilityNameAndSiteOfCareColumnNOTIncludeNCESFacilityFlag`() {
        // Setup to return expected mock data
        val mockMetadata = mockkClass(Metadata::class)
        every { mockMetadata.findLookupTable(fipsCounty) } returns null
        every { mockMetadata.findLookupTable(zipCodeData) } returns zipCodeLookupTable
        every { mockMetadata.findLookupTable(ncesId) } returns ncesLookupTable

        // Build RowContext test case: given reportState = "AL", reportCounty = "Tallapposa",
        // .. and includeNcesFacilities = true
        val alRowContext = FakeReport.RowContext(
            mockMetadata,
            "AL",
            reportCounty = "Tallapoosa"
        )

        // assert expected value of alRowContext.facilitiesName = null
        assertThat(alRowContext.facilitiesName).isNull()

        // Test: Build the "ording_facility_name" column.
        val orderingFacilityName = FakeReport(mockMetadata).buildColumn(orderingFacilityNameElement, alRowContext)

        // assert expected return value of orderingFacilityName: "Any facility USA"
        assertThat(orderingFacilityName.contains("Any facility USA")).isTrue()

        // Test: Build the site_of_care column.
        val siteOfCare = FakeReport(mockMetadata).buildColumn(siteOfCareElement, alRowContext)

        // assert expected return value of siteOfCare != "k12"
        assertThat(siteOfCare.contains("k12")).isFalse()
    }

    @Test
    fun `test use mapper in fake data`() {
        val fieldName = "testing_lab_name"
        val useField = metadata.findSchema(schemaName)
            ?.findElement(fieldName) ?: fail("Lookup failure: $fieldName")

        val actual = FakeReport(metadata).buildMappedColumn(useField, rowContext)
        val expected = "Any lab USA"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test concatenate mapper in fake data`() {
        val fieldName = "testing_lab_and_facility"
        val concatField = metadata.findSchema(schemaName)
            ?.findElement(fieldName) ?: fail("Lookup failure: $fieldName")
        val actual = FakeReport(metadata).buildMappedColumn(concatField, rowContext)
        val expected = "Any lab USA^Any facility USA"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test concatenate mapper with default delimiter in fake data`() {
        val fieldName = "testing_lab_and_facility2"
        val concatField = metadata.findSchema(schemaName)
            ?.findElement(fieldName) ?: fail("Lookup failure: $fieldName")
        val actual = FakeReport(metadata).buildMappedColumn(concatField, rowContext)
        val expected = "Any lab USA, Any facility USA"
        assertThat(actual).isEqualTo(expected)
    }
}