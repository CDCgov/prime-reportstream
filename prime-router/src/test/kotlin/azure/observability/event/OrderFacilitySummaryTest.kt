package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderingFacilitySummaryTest {

    @Test
    fun `fromOrganization with valid Organization returns correct name and state`() {
        val organization = Organization().apply {
            name = "Test Organization"
            addAddress().apply {
                state = "TX"
            }
        }

        val summary = OrderingFacilitySummary.fromOrganization(organization)
        assertEquals("Test Organization", summary.orderingFacilityName)
        assertEquals("TX", summary.orderingFacilityState)
    }

    @Test
    fun `fromOrganization with Organization missing name returns UNKNOWN name`() {
        val organization = Organization().apply {
            // No name
            addAddress().apply {
                state = "CA"
            }
        }

        val summary = OrderingFacilitySummary.fromOrganization(organization)
        assertEquals(OrderingFacilitySummary.UNKNOWN, summary.orderingFacilityName)
        assertEquals("CA", summary.orderingFacilityState)
    }

    @Test
    fun `fromOrganization with Organization missing state returns UNKNOWN state`() {
        val organization = Organization().apply {
            name = "Organization Without State"
            // No address or no state
        }

        val summary = OrderingFacilitySummary.fromOrganization(organization)
        assertEquals("Organization Without State", summary.orderingFacilityName)
        assertEquals(OrderingFacilitySummary.UNKNOWN, summary.orderingFacilityState)
    }

    @Test
    fun `fromOrganization with Organization having multiple addresses picks the first`() {
        val organization = Organization().apply {
            name = "Multi-Address Org"
            addAddress().apply { state = "WA" }
            addAddress().apply { state = "AK" }
        }

        val summary = OrderingFacilitySummary.fromOrganization(organization)
        // Should pick the first address's state
        assertEquals("Multi-Address Org", summary.orderingFacilityName)
        assertEquals("WA", summary.orderingFacilityState)
    }

    @Test
    fun `fromOrganization with non-Organization type returns UNKNOWN for all`() {
        // For example, we can pass a Patient to simulate "unknown" type
        val patient = Patient().apply { id = "some-id" }

        val summary = OrderingFacilitySummary.fromOrganization(patient)
        assertEquals(OrderingFacilitySummary.UNKNOWN, summary.orderingFacilityName)
        assertEquals(OrderingFacilitySummary.UNKNOWN, summary.orderingFacilityState)
    }
}