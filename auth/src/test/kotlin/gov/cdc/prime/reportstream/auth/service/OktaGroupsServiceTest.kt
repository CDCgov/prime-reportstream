package gov.cdc.prime.reportstream.auth.service

import gov.cdc.prime.reportstream.auth.client.OktaGroupsClient
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWT
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OktaGroupsServiceTest {

    inner class Fixture {
        val appId = "appId"
        val groups = listOf("group1", "group2")
        val oktaGroupsJWT = OktaGroupsJWT(appId, groups)
        val jwt = "jwt"

        val oktaGroupsClient: OktaGroupsClient = mockk()
        val oktaGroupsJWTWriter: OktaGroupsJWTWriter = mockk()

        val service = OktaGroupsService(oktaGroupsClient, oktaGroupsJWTWriter)
    }

    @Test
    fun `write JWT given the groups`() {
        val f = Fixture()

        coEvery { f.oktaGroupsClient.getApplicationGroups(f.appId) }
            .returns(f.groups)
        every { f.oktaGroupsJWTWriter.write(f.oktaGroupsJWT) }
            .returns(f.jwt)

        assertEquals(
            runBlocking { f.service.generateOktaGroupsJWT(f.appId) },
            f.jwt
        )
    }
}