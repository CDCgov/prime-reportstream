package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrontendFunctionsTests {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetActiveOrganizations {
        private val mapper = JacksonMapperUtilities.defaultMapper
        private val mockRequest = mockk<HttpRequestMessage<String?>>()
        private val settingsFacadeSpy = spyk(SettingsFacade(UnitTestUtils.simpleMetadata))

        private val testOrg = OrganizationAPI(
            "test",
            "Test Organization",
            Organization.Jurisdiction.FEDERAL,
            null,
            null,
            emptyList()
        )

        @BeforeAll
        fun initDependencies() {
            every { mockRequest.uri } returns URI.create("http://localhost:7071/api/lookuptables")
            every { mockRequest.httpMethod } returns HttpMethod.GET
        }

        private fun createResponseBuilder(): HttpResponseMessage.Builder {
            val mockResponseBuilder = mockk<HttpResponseMessage.Builder>()
            every { mockResponseBuilder.body(any()) } returns mockResponseBuilder
            every { mockResponseBuilder.header(any(), any()) } returns mockResponseBuilder
            every { mockResponseBuilder.build() } returns mockk()
            return mockResponseBuilder
        }

        private fun runTest(customerStatusParam: String?, expectedFetchedStatuses: List<CustomerStatus>) {
            every {
                settingsFacadeSpy.findOrganizationsByReceiverStatus(expectedFetchedStatuses, null)
            }.returns(listOf(testOrg))

            every { mockRequest.queryParameters } returns mapOf("customerStatus" to customerStatusParam)
            var mockResponseBuilder = createResponseBuilder()
            every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
            FrontendFunctions(settingsFacadeSpy).getActiveOrganizations(mockRequest)
            verify(exactly = 1) {
                mockResponseBuilder.body(
                    withArg {
                        // Check that we have JSON data in the response body
                        assertTrue(it is String)
                        val organizations = mapper.readValue<List<OrganizationAPI>>(it)
                        assertTrue(organizations.size == 1)
                        assertEquals("test", organizations[0].name)
                    }
                )
            }
        }

        @Test
        fun `defaults to 'active' customer status`() {
            runTest(null, listOf(CustomerStatus.ACTIVE))
        }

        @Test
        fun `fetches multiple statuses`() {
            runTest("active,inactive", listOf(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE))
        }

        @Test
        fun `ignores invalid status values`() {
            runTest("active,foo", listOf(CustomerStatus.ACTIVE))
        }

        @Test
        fun `trims whitespace`() {
            runTest(
                "active,   inactive, testing   ",
                listOf(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE, CustomerStatus.TESTING)
            )
        }
    }
}