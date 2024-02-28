package gov.cdc.prime.router.history

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.Topic
import java.time.OffsetDateTime
import kotlin.test.Test

class DeliveryHistoryTests {
    @Test
    fun `test DeliveryHistory properties init`() {
        DeliveryHistory(
            922,
            OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            "someVal",
            "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            Topic.COVID_19,
            14,
            "ca-dph",
            "elr-secondary",
            "http://anyblob.com",
            "test-schema",
            "CSV"
        ).run {
            assertThat(actionId).isEqualTo(922)
            assertThat(createdAt).isEqualTo(OffsetDateTime.parse("2022-04-19T18:04:26.534Z"))
            assertThat(receivingOrg).isEqualTo("ca-dph")
            assertThat(receivingOrgSvc).isEqualTo("elr-secondary")
            assertThat(externalName).isEqualTo("someVal")
            assertThat(reportId).isEqualTo("b9f63105-bbed-4b41-b1ad-002a90f07e62")
            assertThat(topic).isEqualTo(Topic.COVID_19)
            assertThat(reportItemCount).isEqualTo(14)
            assertThat(bodyUrl).isEqualTo("http://anyblob.com")
            assertThat(schemaName).isEqualTo("test-schema")
            assertThat(bodyFormat).isEqualTo("CSV")

            assertThat(expires).isEqualTo(OffsetDateTime.parse("2022-05-19T18:04:26.534Z"))

            // val compareFilename = Report.formExternalFilename(
            //     bodyUrl,
            //     ReportId.fromString(reportId),
            //     schemaName,
            //     Report.Format.safeValueOf(bodyFormat),
            //     createdAt
            // )

            // assertThat(filename).isEqualTo(compareFilename)
        }
        DeliveryHistory(
            922,
            OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            "",
            null,
            null,
            14,
            "ca-dph",
            null,
            null,
            "test-schema",
            "CSV"
        ).run {
            assertThat(actionId).isEqualTo(922)
            assertThat(createdAt).isEqualTo(OffsetDateTime.parse("2022-04-19T18:04:26.534Z"))
            assertThat(receivingOrg).isEqualTo("ca-dph")
            assertThat(receivingOrgSvc).isNull()
            assertThat(externalName).isEqualTo("")
            assertThat(reportId).isNull()
            assertThat(topic).isNull()
            assertThat(reportItemCount).isEqualTo(14)
            assertThat(bodyUrl).isNull()
            assertThat(schemaName).isEqualTo("test-schema")
            assertThat(bodyFormat).isEqualTo("CSV")

            assertThat(expires).isEqualTo(OffsetDateTime.parse("2022-05-19T18:04:26.534Z"))
        }
    }
}