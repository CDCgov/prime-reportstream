package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Practitioner
import kotlin.test.Test
import kotlin.test.assertEquals

class PerformerSummaryTest {

    @Test
    fun `test fromPerformer with Practitioner containing name, address, and CLIA`() {
        val practitioner = Practitioner().apply {
            // Create a name for the practitioner
            addName().apply {
                family = "Doe"
                given = listOf(org.hl7.fhir.r4.model.StringType("John"))
            }
            // Create an address for the practitioner
            addAddress().apply {
                state = "GA"
            }
            // Add an identifier with CLIA
            addIdentifier().apply {
                type.coding[0].code = "http://some.system/CLIA"
                value = "123456"
            }
        }

        val summary = PerformerSummary.fromPerformer(practitioner)
        assertEquals("John Doe", summary.performerName, "Practitioner name should match")
        assertEquals("GA", summary.performerState, "Practitioner state should match")
        assertEquals("123456", summary.performerCLIA, "Practitioner CLIA should match")
    }

    @Test
    fun `test fromPerformer with Practitioner missing name, address, and CLIA`() {
        val practitioner = Practitioner()
        // No name, no address, no CLIA

        val summary = PerformerSummary.fromPerformer(practitioner)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerName)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerState)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerCLIA)
    }

    @Test
    fun `test fromPerformer with Practitioner containing an identifier but not CLIA`() {
        val practitioner = Practitioner().apply {
            // Provide a name
            addName().apply {
                family = "Roe"
                given = listOf(org.hl7.fhir.r4.model.StringType("Jane"))
            }
            addAddress().apply {
                state = "TX"
            }
            addIdentifier().apply {
                type.coding[0].code = "http://some.system/NPI"
                value = "999999"
            }
        }

        val summary = PerformerSummary.fromPerformer(practitioner)
        assertEquals("Jane Roe", summary.performerName, "Practitioner name should match")
        assertEquals("TX", summary.performerState, "Practitioner state should match")
        // Should fall back to UNKNOWN for CLIA
        assertEquals(PerformerSummary.UNKNOWN, summary.performerCLIA)
    }

    @Test
    fun `test fromPerformer with Organization containing name, address, and CLIA`() {
        val organization = Organization().apply {
            name = "Test Org"
            addAddress().apply {
                state = "CA"
            }
            addIdentifier().apply {
                type.coding[0].code = "http://another.system/CLIA"
                value = "ORG-CLIA-001"
            }
        }

        val summary = PerformerSummary.fromPerformer(organization)
        assertEquals("Test Org", summary.performerName, "Organization name should match")
        assertEquals("CA", summary.performerState, "Organization state should match")
        assertEquals("ORG-CLIA-001", summary.performerCLIA, "Organization CLIA should match")
    }

    @Test
    fun `test fromPerformer with Organization missing name, address, and CLIA`() {
        val organization = Organization()
        // No name, no address, no CLIA

        val summary = PerformerSummary.fromPerformer(organization)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerName)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerState)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerCLIA)
    }

    @Test
    fun `test fromPerformer with unknown performer type`() {
        // Create a Patient object, which isn't a Practitioner or Organization
        val patient = org.hl7.fhir.r4.model.Patient()
        val summary = PerformerSummary.fromPerformer(patient)

        assertEquals(PerformerSummary.UNKNOWN, summary.performerName)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerState)
        assertEquals(PerformerSummary.UNKNOWN, summary.performerCLIA)
    }
}