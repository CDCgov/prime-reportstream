package gov.cdc.prime.router.permissions

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Permission
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.HttpUtilities.Companion.errorJson
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

/*
 * Permissions API used for features that are associated with an organization
 */

class PermissionsFunctions(
    private val dbAccess: DatabaseAccess = DatabaseAccess()
) : Logging {
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    @FunctionName("getPermissions")
    @StorageAccount("AzureWebJobsStorage")
    fun getPermissions(
        @HttpTrigger(
            name = "getPermissions",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "permissions"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            getAll(request)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    @FunctionName("getPermissionsForOrganization")
    @StorageAccount("AzureWebJobsStorage")
    fun getPermissionsForOrganization(
        @HttpTrigger(
            name = "getPermissionsForOrganization",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "organization/{orgName}/permissions"
        ) request: HttpRequestMessage<String?>,
        @BindingName("orgName") orgName: String
    ): HttpResponseMessage {
        return try {
            getPermissionsByOrgName(request, orgName)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    @FunctionName("updatePermission")
    @StorageAccount("AzureWebJobsStorage")
    fun updatePermission(
        @HttpTrigger(
            name = "updatePermission",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "permission/{id}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("id") id: Int
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            when (request.httpMethod) {
                HttpMethod.PUT ->
                    updateOne(request, id)
                HttpMethod.DELETE ->
                    deleteOne(request, id)
                else ->
                    return HttpUtilities.badRequestResponse(request, errorJson("unsupported method"))
            }
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    @FunctionName("createPermission")
    @StorageAccount("AzureWebJobsStorage")
    fun createPermission(
        @HttpTrigger(
            name = "createPermission",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "permission"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            addOne(request, claims.userName)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    @FunctionName("assignPermissionToOrganization")
    @StorageAccount("AzureWebJobsStorage")
    fun assignPermissionToOrganization(
        @HttpTrigger(
            name = "assignPermissionToOrganization",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "organization/{orgName}/permission/{id}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("orgName") orgName: String,
        @BindingName("id") permissionId: Int
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            addPermissionOrganization(request, orgName, permissionId)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    @FunctionName("deletePermissionFromOrganization")
    @StorageAccount("AzureWebJobsStorage")
    fun deletePermissionFromOrganization(
        @HttpTrigger(
            name = "deletePermissionFromOrganization",
            methods = [HttpMethod.DELETE],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "organization/{orgName}/permission/{id}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("orgName") orgName: String,
        @BindingName("id") permissionId: Int
    ): HttpResponseMessage {
        return try {
            val claims = AuthenticatedClaims.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            // admin permissions only
            if (!claims.isPrimeAdmin) {
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }

            deletePermissionOrganization(request, orgName, permissionId)
        } catch (ex: Exception) {
            if (ex.message != null) {
                logger.error(ex.message!!, ex)
            } else {
                logger.error(ex)
            }
            HttpUtilities.internalErrorResponse(request)
        }
    }

    internal fun getAll(
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        // return all permissions or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                response = dbAccess.fetchAllPermissions()
                HttpStatus.OK
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun getPermissionsByOrgName(
        request: HttpRequestMessage<String?>,
        orgName: String
    ): HttpResponseMessage {
        // return organization permissions or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                response = dbAccess.fetchPermissionsByOrgName(orgName)
                HttpStatus.OK
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun updateOne(
        request: HttpRequestMessage<String?>,
        id: Int
    ): HttpResponseMessage {
        // return a message or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                // Check that the permission id is valid (or null)
                val permission = dbAccess.fetchPermissionById(id)
                if (permission == null) {
                    errorMessage = "No id found."
                    HttpStatus.BAD_REQUEST
                } else {
                    val body = mapper.readValue(request.body!!.toString(), Permission::class.java)

                    response = dbAccess.updatePermissionById(id, body)
                    HttpStatus.OK
                }
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }
        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun addOne(
        request: HttpRequestMessage<String?>,
        userName: String
    ): HttpResponseMessage {
        // return an id or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                val body = mapper.readValue(request.body!!.toString(), Permission::class.java)

                response = dbAccess.insertPermission(userName, body)
                HttpStatus.OK
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }
        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun addPermissionOrganization(
        request: HttpRequestMessage<String?>,
        orgName: String,
        permissionId: Int
    ): HttpResponseMessage {
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                response = dbAccess.insertPermissionOrganization(orgName, permissionId)
                HttpStatus.OK
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun deletePermissionOrganization(
        request: HttpRequestMessage<String?>,
        orgName: String,
        permissionId: Int
    ): HttpResponseMessage {
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                response = dbAccess.deletePermissionOrganization(orgName, permissionId)
                HttpStatus.OK
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }

        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    internal fun deleteOne(
        request: HttpRequestMessage<String?>,
        id: Int
    ): HttpResponseMessage {
        // return a message or an error
        var response: Any = ""
        var errorMessage: String? = ""

        val httpStatus: HttpStatus =
            try {
                // Check that the permission id is valid (or null)
                val permission = dbAccess.fetchPermissionById(id)
                if (permission == null) {
                    errorMessage = "No id found."
                    HttpStatus.BAD_REQUEST
                } else {
                    response = dbAccess.deletePermissionById(id)
                    HttpStatus.OK
                }
            } catch (e: Exception) {
                errorMessage = e.message
                logger.error { errorMessage }
                HttpStatus.BAD_REQUEST
            }
        return responseBuilder(httpStatus, errorMessage, response, request)
    }

    private fun responseBuilder(
        httpStatus: HttpStatus,
        errorMessage: String?,
        response: Any,
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        val responseMessage = if (httpStatus == HttpStatus.BAD_REQUEST) {
            mapOf(
                "error" to true,
                "status" to httpStatus.value(),
                "message" to errorMessage
            )
        } else {
            response
        }

        return request.createResponseBuilder(httpStatus)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                JacksonMapperUtilities.allowUnknownsMapper
                    .writeValueAsString(responseMessage)
            )
            .build()
    }
}