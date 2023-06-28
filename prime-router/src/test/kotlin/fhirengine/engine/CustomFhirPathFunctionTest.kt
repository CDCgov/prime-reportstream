package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.metadata.LivdLookup
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
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
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomFhirPathFunctionTest {
    private val loincCode = "906-1"

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
        assertThat {
            CustomFhirPathFunctions()
                .executeFunction(null, "dummy", null)
        }.isFailure()

        val focus: MutableList<Base> = mutableListOf(Observation())
        assertThat {
            CustomFhirPathFunctions()
                .executeFunction(focus, "dummy", null)
        }.isFailure()

        // Just checking we can access all the functions.
        // Individual function results are tested on their own unit tests.
        CustomFhirPathFunctions.CustomFhirPathFunctionNames.values().forEach {
            if (it == CustomFhirPathFunctions.CustomFhirPathFunctionNames.LivdTableLookup) {
                // With bad inputs this will cause an error, but still verifies access to the function
                assertThat {
                    CustomFhirPathFunctions()
                        .executeFunction(focus, it.name, null)
                }.isSuccess()
            }
        }
    }

    @Test
    fun `test livd lookup function`() {
        // Look up fails if focus element is not an Observation
        assertThat {
            CustomFhirPathFunctions().livdTableLookup(
                mutableListOf(Device()), mutableListOf(mutableListOf(StringType("1"))), UnitTestUtils.simpleMetadata
            )
        }.isFailure()

        // look up fails if more than one Observation
        assertThat {
            CustomFhirPathFunctions().livdTableLookup(
                mutableListOf(Observation(), Observation()),
                mutableListOf(mutableListOf(StringType("1"))),
                UnitTestUtils.simpleMetadata
            )
        }.isFailure()

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
}