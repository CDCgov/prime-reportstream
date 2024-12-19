package gov.cdc.prime.reportstream.auth.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.okta.sdk.resource.api.ApplicationGroupsApi
import com.okta.sdk.resource.model.ApplicationGroupAssignment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class OktaGroupsClientTest {

    class Fixture {
        val appId = "appId"

        // truncated response from staging
        val apiResponse = """
            [
                {
                    "id":"00gek8f3iuksaVp1e1d7",
                    "_embedded":{
                        "group":{
                            "id":"00gek8f3iuksaVp1e1d7",
                            "objectClass":[
                                "okta:user_group"
                            ],
                            "type":"OKTA_GROUP",
                            "profile":{
                                "name":"ArnejTestGroup",
                                "description":"Using this group to play around with scopes"
                            }
                        }
                    }
                },
                {
                    "id":"00g9fxoz8jR9JEbhp1d7",
                    "priority":2,
                    "_embedded":{
                        "group":{
                            "id":"00g9fxoz8jR9JEbhp1d7",
                            "objectClass":[
                                "okta:user_group"
                            ],
                            "type":"OKTA_GROUP",
                            "profile":{
                                "name":"DHflexion",
                                "description":"Flexion org receiver group"
                            }
                        }
                    }
                }
            ]
        """.trimIndent()

        val mapper = jacksonObjectMapper()
        var parsedResponse: List<ApplicationGroupAssignment> = mapper
            .readValue(
                apiResponse,
                mapper.typeFactory.constructCollectionType(List::class.java, ApplicationGroupAssignment::class.java)
            )

        val applicationGroupsApi: ApplicationGroupsApi = mockk()
        val client = OktaGroupsClient(applicationGroupsApi)
    }

    @Test
    fun `fetch groups returns group names`() {
        val f = Fixture()

        every {
            f.applicationGroupsApi.listApplicationGroupAssignments(
                f.appId,
                null,
                null,
                null,
                "group"
            )
        }.returns(f.parsedResponse)

        assertEquals(
            runBlocking { f.client.getApplicationGroups(f.appId) },
            listOf("ArnejTestGroup", "DHflexion")
        )
    }
}