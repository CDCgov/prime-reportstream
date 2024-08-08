package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.metadata.LivdLookup
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomFhirPathFunctionTest {
    private val loincCode = "906-1"
    private val fipsCode = "41051"

    @BeforeEach
    fun setupMocks() {
        mockkObject(LivdLookup, Metadata)

        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
        every { LivdLookup.find(any(), any(), any(), any(), any(), any(), any(), any()) } returns loincCode
    }

    @AfterEach
    fun unmockMocks() {
        unmockkObject(LivdLookup, Metadata)
    }

    @Test
    fun `test get function name enum`() {
        assertThat(CustomFhirPathFunctions.CustomFhirPathFunctionNames.get(null)).isNull()
        assertThat(CustomFhirPathFunctions.CustomFhirPathFunctionNames.get("someBadName")).isNull()
        val goodName = CustomFhirPathFunctions.CustomFhirPathFunctionNames.LivdTableLookup.name
        assertThat(CustomFhirPathFunctions.CustomFhirPathFunctionNames.get(goodName)).isNotNull()
        val nameFormattedFromFhirPath = goodName.replaceFirstChar(Char::lowercase)
        assertThat(CustomFhirPathFunctions.CustomFhirPathFunctionNames.get(nameFormattedFromFhirPath)).isNotNull()
    }

    @Test
    fun `test resolve function name`() {
        assertThat(CustomFhirPathFunctions().resolveFunction(null)).isNull()
        assertThat(
            CustomFhirPathFunctions()
                .resolveFunction("someBadName")
        ).isNull()
        val nameFormattedFromFhirPath = CustomFhirPathFunctions.CustomFhirPathFunctionNames.LivdTableLookup.name
            .replaceFirstChar(Char::lowercase)
        assertThat(
            CustomFhirPathFunctions()
                .resolveFunction(nameFormattedFromFhirPath)
        ).isNotNull()

        CustomFhirPathFunctions.CustomFhirPathFunctionNames.values().forEach {
            assertThat(CustomFhirPathFunctions().resolveFunction(it.name)).isNotNull()
        }
    }

    @Test
    fun `test execute function`() {
        assertFailure {
            CustomFhirPathFunctions()
                .executeFunction(null, "dummy", null)
        }

        val focus: MutableList<Base> = mutableListOf(Observation())
        assertFailure {
            CustomFhirPathFunctions()
                .executeFunction(focus, "dummy", null)
        }

        // Just checking we can access all the functions.
        // Individual function results are tested on their own unit tests.
        CustomFhirPathFunctions.CustomFhirPathFunctionNames.values().forEach {
            if (it == CustomFhirPathFunctions.CustomFhirPathFunctionNames.LivdTableLookup) {
                // With bad inputs this will cause an error, but still verifies access to the function
                assertThat(
                    CustomFhirPathFunctions()
                        .executeFunction(focus, it.name, null)
                )
            }
        }
    }

    @Test
    fun `test livd lookup function`() {
        // Look up fails if focus element is not an Observation
        assertFailure {
            CustomFhirPathFunctions().livdTableLookup(
                mutableListOf(Device()), mutableListOf(mutableListOf(StringType("1"))), UnitTestUtils.simpleMetadata
            )
        }

        // look up fails if more than one Observation
        assertFailure {
            CustomFhirPathFunctions().livdTableLookup(
                mutableListOf(Observation(), Observation()),
                mutableListOf(mutableListOf(StringType("1"))),
                UnitTestUtils.simpleMetadata
            )
        }

        // Test getting loinc code from device id
        var observation = Observation()
        observation.method.coding = mutableListOf(Coding(null, loincCode, null))
        var result = CustomFhirPathFunctions().livdTableLookup(
            mutableListOf(observation), mutableListOf(mutableListOf(StringType("1"))), UnitTestUtils.simpleMetadata
        )
        assertThat(
            (result[0] as StringType).value
        ).isEqualTo(loincCode)

        // Test getting loinc code from equipment model id
        val device = Device()
        device.id = "TestDevice"
        val identifier = Identifier()
        identifier.id = "TestIdentifier"
        device.identifier = mutableListOf(identifier)
        observation = Observation()
        observation.device = Reference(device.id)
        observation.device.resource = device
        result = CustomFhirPathFunctions().livdTableLookup(
            mutableListOf(observation), mutableListOf(mutableListOf(StringType("1"))), UnitTestUtils.simpleMetadata
        )
        assertThat(
            (result[0] as StringType).value
        ).isEqualTo(loincCode)

        // Test getting loinc code from device name
        device.identifier = null
        device.deviceName = mutableListOf(Device.DeviceDeviceNameComponent(StringType("Test"), null))
        result = CustomFhirPathFunctions().livdTableLookup(
            mutableListOf(observation), mutableListOf(mutableListOf(StringType("1"))), UnitTestUtils.simpleMetadata
        )
        assertThat(
            (result[0] as StringType).value
        ).isEqualTo(loincCode)
    }

    @Test
    fun `test getting the fips code for a county and state`() {
        val testTable = Table.create(
            "fips-county",
            StringColumn.create("state", "OR", "OR"),
            StringColumn.create("county", "multnomah", "clackamas"),
            StringColumn.create("FIPS", "41051", "41005")
        )
        val testLookupTable = LookupTable(name = "fips-county", table = testTable)

        mockkConstructor(Metadata::class)
        every { anyConstructed<Metadata>().findLookupTable("fips-county") } returns testLookupTable
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata

        // Test getting city
        val result = CustomFhirPathFunctions().fipsCountyLookup(
            mutableListOf(mutableListOf(StringType("Multnomah")), mutableListOf(StringType("OR"))),

        )

        assertThat(
            (result[0] as StringType).value
        ).isEqualTo("41051")
    }
}