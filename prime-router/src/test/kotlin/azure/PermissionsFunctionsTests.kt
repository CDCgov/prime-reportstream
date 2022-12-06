package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Permission
import gov.cdc.prime.router.permissions.PermissionsFunctions
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import java.time.OffsetDateTime
import kotlin.test.Test

class PermissionsFunctionsTests {
    private val orgName: String = "OrgNameTest"
    private val userName: String = "local@test.com"
    private val permissionId: Int = 1
    private val body = """{
            "name": "validate",
            "description":"Validates a file",
            "enabled":"true",
            "createdBy":"test.user"
        }"""

    private fun buildPermissionsFunction(
        mockDbAccess: DatabaseAccess? = null
    ): PermissionsFunctions {
        val dbAccess = mockDbAccess ?: mockk()
        return PermissionsFunctions(dbAccess)
    }

    private fun buildPermissions(): List<Permission> {
        val permission = Permission(
            1,
            "validate",
            "Validates a file.",
            true,
            "test.user",
            OffsetDateTime.now().minusWeeks(1)
        )
        return listOf(
            permission
        )
    }

    private fun buildPermission(id: Int): Permission {
        return Permission(
            id,
            "validate",
            "Validates a file.",
            true,
            "test.user",
            OffsetDateTime.now().minusWeeks(1)
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test getPermissions function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.getPermissions(any()) } returns resp

        val res = permissionsFunctions.getPermissions(req)
        assert(res.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test getPermissions function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage()
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.getAll(any()) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.getPermissions(internalServerErrorReq)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.getAll(any()) }.throws(Exception())

        internalErrorRes = permissionsFunctions.getPermissions(internalServerErrorReq)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test getPermissionsForOrganization function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.getPermissionsForOrganization(any(), any()) } returns resp

        val res = permissionsFunctions.getPermissionsForOrganization(req, orgName)
        assert(res.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test getPermissionsForOrganization function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage()
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.getPermissionsByOrgName(any(), orgName) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.getPermissionsForOrganization(internalServerErrorReq, orgName)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.getPermissionsByOrgName(any(), orgName) }.throws(Exception())

        internalErrorRes = permissionsFunctions.getPermissionsForOrganization(internalServerErrorReq, orgName)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test getAll function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequest = MockHttpRequestMessage()
        every { mockDbAccess.fetchAllPermissions(any()) } returns buildPermissions()
        val response = permissionsFunctions.getAll(mockRequest)
        assert(response.status.equals(HttpStatus.OK))

        // Exception in the database
        val mockRequestException = MockHttpRequestMessage()
        every {
            mockDbAccess.fetchAllPermissions(
                any()
            )
        }.throws(Exception("exception thrown"))
        val exceptionResponse = permissionsFunctions.getAll(
            mockRequestException
        )
        assert(exceptionResponse.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test getPermissionsByOrgName function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequestWithOrgName = MockHttpRequestMessage()
        every { mockDbAccess.fetchPermissionsByOrgName(any(), any()) } returns buildPermissions()
        val response = permissionsFunctions.getPermissionsByOrgName(mockRequestWithOrgName, orgName)

        val jsonResponse = JSONArray(response.body.toString())
        val permissionObject = jsonResponse.first() as JSONObject
        val permissionObjectId = permissionObject.get("name")
        assert(permissionObjectId.equals("validate"))
        assert(response.status.equals(HttpStatus.OK))

        // Exception in the database
        val mockRequestMissingOrgNameValue = MockHttpRequestMessage()
        mockRequestMissingOrgNameValue.parameters["orgName"] = ""
        every {
            mockDbAccess.fetchPermissionsByOrgName(
                any(),
                any()
            )
        }.throws(Exception("missing orgName"))
        val missingOrgNameValueResponse = permissionsFunctions.getPermissionsByOrgName(
            mockRequestMissingOrgNameValue,
            orgName
        )
        assert(missingOrgNameValueResponse.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test updatePermission function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        // Happy path default
        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.updatePermission(any(), any()) } returns resp

        val res = permissionsFunctions.updatePermission(req, permissionId)
        assert(res.status.equals(HttpStatus.OK))

        // Happy path PUT
        val reqPut = MockHttpRequestMessage(body, HttpMethod.PUT)
        val resPut = permissionsFunctions.updatePermission(reqPut, permissionId)
        assert(resPut.status.equals(HttpStatus.OK))

        // Happy path DELETE
        val reqDelete = MockHttpRequestMessage(body, HttpMethod.DELETE)
        val resDelete = permissionsFunctions.updatePermission(reqDelete, permissionId)
        assert(resDelete.status.equals(HttpStatus.OK))

        clearAllMocks()

        // Throws error if HttpMethod not valid
        val resUnsupportedMethod = permissionsFunctions.updatePermission(req, permissionId)
        assert(resUnsupportedMethod.status.equals(HttpStatus.BAD_REQUEST))

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = permissionsFunctions.updatePermission(unAuthReq, permissionId)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = permissionsFunctions.updatePermission(unAuthReq, permissionId)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test updatePermission function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage(body, HttpMethod.PUT)
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.updateOne(any(), permissionId) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.updatePermission(internalServerErrorReq, permissionId)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.updateOne(any(), permissionId) }.throws(Exception())

        internalErrorRes = permissionsFunctions.updatePermission(internalServerErrorReq, permissionId)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test createPermission function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        // Happy path
        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.createPermission(any()) } returns resp

        val res = permissionsFunctions.createPermission(req)
        assert(res.status.equals(HttpStatus.OK))

        clearAllMocks()

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = permissionsFunctions.createPermission(unAuthReq)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = permissionsFunctions.createPermission(unAuthReq)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test createPermission function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage(body, HttpMethod.PUT)
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.addOne(any(), userName) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.createPermission(internalServerErrorReq)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.addOne(any(), userName) }.throws(Exception())

        internalErrorRes = permissionsFunctions.createPermission(internalServerErrorReq)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test assignPermissionToOrganization function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        // Happy path
        val req = MockHttpRequestMessage(body, HttpMethod.POST)
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.assignPermissionToOrganization(any(), any(), any()) } returns resp

        val res = permissionsFunctions.assignPermissionToOrganization(req, orgName, permissionId)
        assert(res.status.equals(HttpStatus.OK))

        clearAllMocks()

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = permissionsFunctions.assignPermissionToOrganization(unAuthReq, orgName, permissionId)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = permissionsFunctions.assignPermissionToOrganization(unAuthReq, orgName, permissionId)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test assignPermissionToOrganization function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage(body, HttpMethod.POST)
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.addPermissionOrganization(any(), orgName, permissionId) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.assignPermissionToOrganization(
            internalServerErrorReq,
            orgName,
            permissionId
        )

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.addPermissionOrganization(any(), orgName, permissionId) }.throws(Exception())

        internalErrorRes = permissionsFunctions.assignPermissionToOrganization(
            internalServerErrorReq,
            orgName,
            permissionId
        )
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test deletePermissionFromOrganization function`() {
        val permissionsFunctions = spyk(PermissionsFunctions())

        // Happy path
        val req = MockHttpRequestMessage(body, HttpMethod.DELETE)
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { permissionsFunctions.deletePermissionFromOrganization(any(), any(), any()) } returns resp

        val res = permissionsFunctions.deletePermissionFromOrganization(req, orgName, permissionId)
        assert(res.status.equals(HttpStatus.OK))

        clearAllMocks()

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = permissionsFunctions.deletePermissionFromOrganization(unAuthReq, orgName, permissionId)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = permissionsFunctions.deletePermissionFromOrganization(unAuthReq, orgName, permissionId)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test deletePermissionFromOrganization function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage(body, HttpMethod.DELETE)
        val permissionsFunctions = spyk(buildPermissionsFunction())

        every { permissionsFunctions.deletePermissionOrganization(any(), orgName, permissionId) }.throws(
            Exception("something went wrong somewhere")
        )

        var internalErrorRes = permissionsFunctions.deletePermissionFromOrganization(
            internalServerErrorReq,
            orgName,
            permissionId
        )

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { permissionsFunctions.deletePermissionOrganization(any(), orgName, permissionId) }.throws(Exception())

        internalErrorRes = permissionsFunctions.deletePermissionFromOrganization(
            internalServerErrorReq,
            orgName,
            permissionId
        )
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `test updateOne function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequestWithId = MockHttpRequestMessage(body)
        every { mockDbAccess.fetchPermissionById(any(), any()) } returns buildPermission(permissionId)
        every { mockDbAccess.updatePermissionById(any(), any(), any()) } returns Unit
        val response = permissionsFunctions.updateOne(mockRequestWithId, permissionId)

        assert(response.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test updateOne function no id found`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Permission id not valid or null
        val mockRequestWithInvalidId = MockHttpRequestMessage(body)
        every { mockDbAccess.fetchPermissionById(any(), any()) } returns null

        val response = permissionsFunctions.updateOne(mockRequestWithInvalidId, permissionId)

        val responseBody = JSONObject(response.body.toString())
        val messageObject = responseBody.get("message")
        assert(messageObject.equals("No id found."))
        assert(response.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test addPermissionOrganization function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequest = MockHttpRequestMessage(body, HttpMethod.POST)
        every { mockDbAccess.insertPermissionOrganization(any(), any(), any()) } returns 1
        val response = permissionsFunctions.addPermissionOrganization(mockRequest, orgName, permissionId)

        assert(response.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test deletePermissionOrganization function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequestWithId = MockHttpRequestMessage()
        every { mockDbAccess.deletePermissionOrganization(any(), any(), any()) } returns Unit
        val response = permissionsFunctions.deletePermissionOrganization(mockRequestWithId, orgName, permissionId)

        assert(response.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test addOne function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val request = """{"name":"measure-tracker","description":"Validates a file"}"""
        val mockRequest = MockHttpRequestMessage(request, HttpMethod.POST)
        every { mockDbAccess.insertPermission(any(), any()) } returns 1
        val response = permissionsFunctions.addOne(mockRequest, userName)

        assert(response.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test addOne function missing required field`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // request not valid
        val request = """{"description":"Validates a file"}"""
        val mockRequest = MockHttpRequestMessage(request, HttpMethod.POST)
        every { mockDbAccess.insertPermission(any(), any()) } returns 1

        val errorRes = permissionsFunctions.addOne(mockRequest, userName)
        assert(errorRes.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test deleteOne function`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Happy path
        val mockRequestWithId = MockHttpRequestMessage()
        every { mockDbAccess.fetchPermissionById(any(), any()) } returns buildPermission(permissionId)
        every { mockDbAccess.deletePermissionById(any(), any()) } returns Unit
        val response = permissionsFunctions.deleteOne(mockRequestWithId, permissionId)

        assert(response.status.equals(HttpStatus.OK))
    }

    @Test
    fun `test deleteOne function no id found`() {
        val mockDbAccess = mockk<DatabaseAccess>()
        val permissionsFunctions = spyk(buildPermissionsFunction(mockDbAccess))

        // Permission id not valid or null
        val mockRequestWithInvalidId = MockHttpRequestMessage()
        every { mockDbAccess.fetchPermissionById(any(), any()) } returns null

        val response = permissionsFunctions.deleteOne(mockRequestWithInvalidId, permissionId)

        val responseBody = JSONObject(response.body.toString())
        val messageObject = responseBody.get("message")
        assert(messageObject.equals("No id found."))
        assert(response.status.equals(HttpStatus.BAD_REQUEST))
    }
}