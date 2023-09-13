package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class HistoryFunctionsTests {

    @Test
    fun `test isAuthorizedIgnoreDashes`() {
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(emptyList<String>(), "md-phd")).isFalse()
        var oktaOrgs = listOf<String?>("DHPrimeAdmins")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isTrue()
        oktaOrgs = listOf("DHPrimeAdmins", "DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        oktaOrgs = listOf(null, "DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        oktaOrgs = listOf("DHmd-phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md_phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd-")).isFalse()
        oktaOrgs = listOf("DHmd_phd")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md_phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "md-phd-")).isFalse()
        oktaOrgs = listOf("DHfoobar")
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foobar")).isTrue()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "FOOBAR")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "foo")).isFalse()
        assertThat(BaseHistoryFunction.isAuthorizedIgnoreDashes(oktaOrgs, "PrimeAdmin")).isFalse()
    }

    @Nested
    inner class GetReportTests {

        @AfterEach
        fun teardown() {
            clearAllMocks()
        }

        @BeforeEach
        fun setup() {
            mockkObject(Metadata.Companion)
            every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
        }

        @Test
        fun `test get report wrong organization`() {
            val request = MockHttpRequestMessage()
            request.httpHeaders["organization"] = "test1"
            val jwt = mapOf("organization" to listOf("DHSender_test1Admins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns claims
            val context = mockkClass(ExecutionContext::class)
            val reportFile = ReportFile()
            reportFile.reportId = UUID.randomUUID()
            reportFile.receivingOrg = "test2"
            val mockDb = mockk<DatabaseAccess>()
            every { mockDb.fetchReportFile(any()) } returns reportFile
            mockkConstructor(WorkflowEngine::class)
            every { anyConstructed<WorkflowEngine>().db } returns mockDb
            val response = BaseHistoryFunction().getReportById(request, reportFile.reportId.toString(), context)
            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `test get report missing organization header`() {
            val request = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf("DHSender_test1Admins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns claims
            val context = mockkClass(ExecutionContext::class)
            val reportFile = ReportFile()
            reportFile.reportId = UUID.randomUUID()
            reportFile.receivingOrg = "test2"
            val mockDb = mockk<DatabaseAccess>()
            every { mockDb.fetchReportFile(any()) } returns reportFile
            mockkConstructor(WorkflowEngine::class)
            every { anyConstructed<WorkflowEngine>().db } returns mockDb
            val response = BaseHistoryFunction().getReportById(request, reportFile.reportId.toString(), context)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `test get report for report id`() {
            val request = MockHttpRequestMessage()
            request.httpHeaders["organization"] = "test1"

            val jwt = mapOf("organization" to listOf("DHSender_test1Admins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns claims

            val reportFile = ReportFile()
            reportFile.reportId = UUID.randomUUID()
            reportFile.receivingOrg = "test1"
            reportFile.bodyUrl = "http://bodyUrl"
            reportFile.createdAt = OffsetDateTime.now()
            reportFile.itemCount = 1
            reportFile.bodyFormat = Report.Format.HL7.toString()
            reportFile.receivingOrgSvc = "default"
            reportFile.schemaName = "default"
            reportFile.externalName = "external-name"

            mockkObject(BlobAccess.Companion)
            every { BlobAccess.downloadBlob(reportFile.bodyUrl) } returns "test".toByteArray()

            val context = mockkClass(ExecutionContext::class)

            val mockDb = mockk<DatabaseAccess>()
            every { mockDb.fetchReportFile(any()) } returns reportFile
            every { mockDb.fetchItemLineagesForReport(reportFile.reportId, reportFile.itemCount) } returns emptyList()
            mockkConstructor(WorkflowEngine::class)
            every { anyConstructed<WorkflowEngine>().db } returns mockDb
            every { anyConstructed<WorkflowEngine>().recordAction(any()) } returns Unit

            val response = BaseHistoryFunction().getReportById(request, reportFile.reportId.toString(), context)

            assertThat(response.status).isEqualTo(HttpStatus.OK)
            val body = response.body as ReportView
            assertThat(body.fileType).isEqualTo("HL7")
            assertThat(body.total).isEqualTo(1)
            assertThat(body.displayName).isEqualTo("external-name")
            assertThat(body.content).isEqualTo("test")

            verify(exactly = 1) {
                mockDb.fetchReportFile(reportFile.reportId)
                mockDb.fetchItemLineagesForReport(reportFile.reportId, reportFile.itemCount)
                BlobAccess.downloadBlob(reportFile.bodyUrl)
            }
        }

        @Test
        fun `test get report for sent report id no body url`() {
            val request = MockHttpRequestMessage()
            request.httpHeaders["organization"] = "test1"

            val jwt = mapOf("organization" to listOf("DHSender_test1Admins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns claims

            val parentReportFile = ReportFile()
            parentReportFile.bodyUrl = "http://test"

            val reportFile = ReportFile()
            reportFile.reportId = UUID.randomUUID()
            reportFile.receivingOrg = "test1"
            reportFile.createdAt = OffsetDateTime.now()
            reportFile.itemCount = 1
            reportFile.bodyFormat = Report.Format.HL7.toString()
            reportFile.receivingOrgSvc = "default"
            reportFile.schemaName = "default"

            mockkObject(BlobAccess.Companion)
            every { BlobAccess.downloadBlob(parentReportFile.bodyUrl) } returns "test".toByteArray()

            val context = mockkClass(ExecutionContext::class)

            val mockDb = mockk<DatabaseAccess>()
            every { mockDb.fetchReportFile(any()) } returns reportFile
            every { mockDb.fetchParentReport(reportFile.reportId) } returns parentReportFile
            every { mockDb.fetchItemLineagesForReport(reportFile.reportId, reportFile.itemCount) } returns emptyList()
            mockkConstructor(WorkflowEngine::class)
            every { anyConstructed<WorkflowEngine>().db } returns mockDb
            every { anyConstructed<WorkflowEngine>().recordAction(any()) } returns Unit

            val response = BaseHistoryFunction().getReportById(request, reportFile.reportId.toString(), context)

            assertThat(response.status).isEqualTo(HttpStatus.OK)
            verify(exactly = 1) {
                mockDb.fetchReportFile(reportFile.reportId)
                mockDb.fetchItemLineagesForReport(reportFile.reportId, reportFile.itemCount)
                mockDb.fetchParentReport(reportFile.reportId)
                BlobAccess.downloadBlob(parentReportFile.bodyUrl)
            }
        }

        @Test
        fun `test get report id returns not found if there are no contents`() {
            val request = MockHttpRequestMessage()
            request.httpHeaders["organization"] = "test1"

            val jwt = mapOf("organization" to listOf("DHSender_test1Admins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.authenticate(any()) } returns claims

            val reportFile = ReportFile()
            reportFile.reportId = UUID.randomUUID()
            reportFile.receivingOrg = "test1"
            reportFile.bodyUrl = "http://bodyUrl"
            reportFile.createdAt = OffsetDateTime.now()
            reportFile.itemCount = 1
            reportFile.bodyFormat = Report.Format.HL7.toString()
            reportFile.receivingOrgSvc = "default"
            reportFile.schemaName = "default"

            mockkObject(BlobAccess.Companion)
            every { BlobAccess.downloadBlob(reportFile.bodyUrl) } returns "".toByteArray()

            val context = mockkClass(ExecutionContext::class)

            val mockDb = mockk<DatabaseAccess>()
            every { mockDb.fetchReportFile(any()) } returns reportFile
            mockkConstructor(WorkflowEngine::class)
            every { anyConstructed<WorkflowEngine>().db } returns mockDb
            every { anyConstructed<WorkflowEngine>().recordAction(any()) } returns Unit

            val response = BaseHistoryFunction().getReportById(request, reportFile.reportId.toString(), context)

            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
            verify(exactly = 1) {
                mockDb.fetchReportFile(reportFile.reportId)
                BlobAccess.downloadBlob(reportFile.bodyUrl)
            }
        }
    }
}