package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.GAENUUIDFormat
import java.util.UUID
import kotlin.test.Test

class GAENTransportTests {

    @Test
    fun `test formatUUID`() {
        val reportId = UUID.randomUUID()
        // Test example sent by WA
        assertThat(
            GAENTransport.formatUUID(
                GAENUUIDFormat.WA_NOTIFY,
                reportId = reportId,
                phone = "15555555555",
                testDate = "2022-03-31",
                uuidIV = "1"
            )
        ).isEqualTo("26e0844004867d563359edcc2f4f8d62")

        // Using default
        assertThat(
            GAENTransport.formatUUID(
                GAENUUIDFormat.PHONE_DATE,
                reportId = reportId,
                phone = "15555555555",
                testDate = "2022-03-31",
                uuidIV = "1"
            )
        ).isEqualTo("86454c037eeed17a8a542371fdc1683e")

        // Using report-id
        assertThat(
            GAENTransport.formatUUID(
                GAENUUIDFormat.REPORT_ID,
                reportId = reportId,
                phone = "15555555555",
                testDate = "2022-03-31",
                uuidIV = "1"
            )
        ).isEqualTo(reportId.toString())

        // No key test case
        assertThat(
            GAENTransport.formatUUID(
                GAENUUIDFormat.PHONE_DATE,
                reportId = reportId,
                phone = "15555555555",
                testDate = "2022-03-31",
                uuidIV = null
            )
        ).isEqualTo(reportId.toString())
    }
}