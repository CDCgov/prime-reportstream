package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DownloadReportTest {
    @Suppress("ktlint:standard:max-line-length")
    val report = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347",
        |"meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}
""".trimMargin()

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `invalid access token`() {
        val mockDb = mockk<DatabaseAccess>()
        val downloadReport = DownloadReport()
        downloadReport.databaseAccess = mockDb
        assertFailsWith<IllegalStateException>(
            block = {
                downloadReport.test(
                    "-r ${UUID.randomUUID()} -e staging --remove-pii true",
                    ansiLevel = AnsiLevel.TRUECOLOR
                )
            }
        )
    }

    @Test
    fun `valid access token, no report`() {
        assertFailsWith<IllegalStateException>(
            block = {
                DownloadReport().test(
                    "-r ${UUID.randomUUID()} -e local --remove-pii true",
                    ansiLevel = AnsiLevel.TRUECOLOR
                )
            }
        )
    }

    @Test
    fun `valid access token, report found, PII removal`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        every { mockDb.fetchReportFile(any()) } returns reportFile
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns report.toByteArray(Charsets.UTF_8)
        val downloadReport = DownloadReport()
        downloadReport.databaseAccess = mockDb

        val result = downloadReport.test(
            "-r ${UUID.randomUUID()} -e local --remove-pii true",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assert(result.output.contains("MESSAGE OUTPUT"))
    }

    @Test
    fun `valid access token, report found, asked for no removal on prod`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        every { mockDb.fetchReportFile(any()) } returns reportFile
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns report.toByteArray(Charsets.UTF_8)
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile
        mockkObject(CommandUtilities)
        every { CommandUtilities.isApiAvailable(any(), any()) } returns true
        val downloadReport = DownloadReport()
        downloadReport.databaseAccess = mockDb

        val result = downloadReport.test(
            "-r ${UUID.randomUUID()} -e prod --remove-pii false",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assert(result.stderr.isNotBlank())
    }

    @Test
    fun `valid access token, report found, no PII removal`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.fhir"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        every { mockDb.fetchReportFile(any()) } returns reportFile
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"
        val blobConnectionInfo = mockk<BlobAccess.BlobContainerMetadata>()
        every { blobConnectionInfo.getBlobEndpoint() } returns "http://endpoint/metadata"
        every { BlobAccess.downloadBlobAsByteArray(any<String>()) } returns report.toByteArray(Charsets.UTF_8)
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile
        val downloadReport = DownloadReport()
        downloadReport.databaseAccess = mockDb

        val result = downloadReport.test(
            "-r ${UUID.randomUUID()} -e local --remove-pii false",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assert(result.output.contains("MESSAGE OUTPUT"))
    }

    @Test
    fun `valid access token, report found, body URL not FHIR`() {
        val reportFile = ReportFile()
        reportFile.bodyUrl = "fakeurl.hl7"
        mockkObject(AuthenticatedClaims)
        val mockDb = mockk<DatabaseAccess>()
        every { mockDb.fetchReportFile(any()) } returns reportFile
        mockkConstructor(WorkflowEngine::class)
        every { anyConstructed<WorkflowEngine>().db } returns mockDb
        every { mockDb.fetchReportFile(reportId = any(), null, null) } returns reportFile
        val downloadReport = DownloadReport()
        downloadReport.databaseAccess = mockDb

        val result = downloadReport.test(
            "-r ${UUID.randomUUID()} -e local --remove-pii true",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assert(result.stderr.contains("not fhir"))
    }
}