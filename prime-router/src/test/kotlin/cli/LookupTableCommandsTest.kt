package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.JacksonMapperUtilities
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockk
import org.apache.http.HttpStatus
import org.jooq.JSONB
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LookupTableCommandsTest {
    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.defaultMapper

    @Test
    fun `test rows to table`() {
        val colNames = listOf("a", "b")
        val data = mapOf(colNames[0] to "value1", colNames[1] to "value2")
        val output = LookupTableCommands.rowsToPrintableTable(listOf(data), colNames)
        assertThat(output.isNotEmpty()).isTrue()

        assertFailsWith<IllegalArgumentException>(
            block = {
                LookupTableCommands.rowsToPrintableTable(emptyList(), colNames)
            }
        )
    }

    @Test
    fun `test info to table`() {
        val data = listOf(LookupTableVersion())
        data[0].createdAt = OffsetDateTime.now()
        data[0].createdBy = "someone"
        data[0].isActive = false
        data[0].tableVersion = 1
        data[0].tableName = "name"
        data[0].tableSha256Checksum = "abc123"
        val output = LookupTableCommands.infoToPrintableTable(data)
        assertThat(output.isNotEmpty()).isTrue()

        assertFailsWith<IllegalArgumentException>(
            block = {
                LookupTableCommands.infoToPrintableTable(emptyList())
            }
        )
    }

    @Test
    fun `test extract row data from json`() {
        val colNames = listOf("a", "b")
        var data = LookupTableEndpointUtilities
            .extractTableRowFromJson(JSONB.jsonb("""{"a": "value1", "b": "value2"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", "value2"))

        data = LookupTableEndpointUtilities.extractTableRowFromJson(JSONB.jsonb("""{"a": "value1"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", ""))

        data = LookupTableEndpointUtilities.extractTableRowFromJson(JSONB.jsonb("{}"), colNames)
        assertThat(data).isEqualTo(listOf("", ""))
    }

    @Test
    fun `test set table row to json`() {
        var json = LookupTableEndpointUtilities.setTableRowToJson(mapOf("a" to "value1", "b" to "value2"))
        var row = mapper.readValue<Map<String, String>>(json.data())
        assertThat(row["a"]).isEqualTo("value1")
        assertThat(row["b"]).isEqualTo("value2")

        json = LookupTableEndpointUtilities.setTableRowToJson(emptyMap())
        row = mapper.readValue(json.data())
        assertThat(row.isEmpty()).isTrue()
    }

    @Test
    fun `get error from response test`() {
        val mockResult = mockk<Result.Success<FuelJson>>()
        val mockResultFailure = mockk<Result.Failure<FuelError>>()
        val mockGenericErrorMessage = "Some dummy message"

        // Not a failure
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResult)).isEqualTo("")

        // No response from the API
        every { mockResultFailure.error.response.body().isEmpty() } returns true
        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // The response from the API is just a string, not JSON
        every { mockResultFailure.error.response.body().isEmpty() } returns false
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // Response is JSON, but has no error field
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"dummy": "dummy"}"""
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // A response with an error
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"error": "some error"}"""
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo("some error")
    }

    @Test
    fun `check common errors from response test`() {
        val mockResult = mockk<Result.Success<FuelJson>>()
        val mockResultFailure = mockk<Result.Failure<FuelError>>()
        val mockResponse = mockk<Response>()

        // API Not found
        every { mockResponse.statusCode } returns HttpStatus.SC_NOT_FOUND
        every { mockResultFailure.error.response.body().isEmpty() } returns false
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // API Not found with unexpected JSON response
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"dummy": "dummy"}"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Table not found
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"error": "some error"}"""
        assertFailsWith<LookupTableEndpointUtilities.Companion.TableNotFoundException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Nome other error
        every { mockResponse.statusCode } returns HttpStatus.SC_BAD_REQUEST
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Good response, but no body returned
        every { mockResponse.statusCode } returns HttpStatus.SC_OK
        every { mockResult.get().content.isBlank() } returns true
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )
    }

    @Test
    fun `get table info from response test`() {
        val mockResult = mockk<Result<FuelJson, FuelError>>()
        val mockResponse = mockk<Response>()

        // Empty content
        every { mockResponse.statusCode } returns HttpStatus.SC_OK
        every { mockResult.get().content } returns ""

        // Not a JSON array
        every { mockResult.get().content } returns """{}"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.getTableInfoFromResponse(mockResult, mockResponse)
            }
        )

        // Good data
        every { mockResult.get().content } returns
            """{"tableName": "name", "tableVersion": 1, "isActive": true, 
                        "createdBy": "developer", "createdAt": "2018-12-30T06:00:00Z"}
            """
        val info = LookupTableEndpointUtilities.getTableInfoFromResponse(mockResult, mockResponse)
        assertThat(info.tableName).isEqualTo("name")
    }

    @Test
    fun `compare and annotate sender compendium with lookup table`() {
        val tableMap: Map<String?, Map<String, String>> = mapOf(
            "12345" to mapOf("Code" to "12345", "Descriptor" to "some descriptor", "Code System" to "SYSTEM1"),
            "54321" to mapOf("Code" to "54321", "Descriptor" to "some descriptor", "Code System" to "SYSTEM2")
        )
        val compendium = listOf(
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM1"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM2"),
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM2"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM1"),
            mapOf("test code" to "56789", "test description" to "test", "coding system" to "SYSTEM1")
        )
        val expectedOutput = listOf(
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "Y"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM2", "mapped?" to "Y"),
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM2", "mapped?" to "N"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "N"),
            mapOf("test code" to "56789", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "N")
        )
        val output = LookupTableCompareMappingCommand.compareMappings(compendium, tableMap)
        assertThat(output).isEqualTo(expectedOutput)
    }

    @Test
    fun `sync observation mapping table with update data`() {
        val conditionData = mapOf(
            "condition_name" to "Acute poliomyelitis (disorder)",
            "condition_code" to "398102009",
            "Condition Code System" to "SNOMEDCT",
            "Condition Code System Version" to "2023-03",
            "Value Source" to "RCTC",
            "Created At" to "10/24/23"
        )
        val noOIDData = listOf(mapOf("foo" to "bar"), mapOf("biz" to "buzz"))
        val polioData = mapOf(
            "Member OID" to "2.16.840.1.113762.1.4.1146.828",
            "Name" to "Poliovirus Infection (Organism or Substance in Lab Results)",
            "Descriptor" to "Human poliovirus 3 (organism)",
            "Code System" to "SNOMEDCT",
            "Version" to "2023-03",
            "Status" to "Active",
        )
        val tableData = mapOf(
            "2.16.840.1.113762.1.4.1146.828" to listOf(
                mapOf("Code" to "16362001") + polioData + conditionData,
                mapOf("Code" to "16362002") + polioData + conditionData
            ),
            "Some Random OID" to noOIDData,
            "NO_OID" to noOIDData
        )
        val labTestData = mapOf(
            "Member OID" to "2.16.840.1.113762.1.4.1146.828",
            "Name" to "Poliovirus Infection (Organism or Substance in Lab Results) 2",
            "Code System" to "SNOMEDCT",
            "Version" to "2023-03",
            "Status" to "Active"
        )
        val updateData = mapOf(
            "2.16.840.1.113762.1.4.1146.828" to listOf(
                mapOf("Code" to "16362002", "Descriptor" to "Human poliovirus over 9000 (organism)") + labTestData,
                mapOf("Code" to "16362003", "Descriptor" to "Human poliovirus 3 (organism)") + labTestData
            )
        )

        val output = LookupTableUpdateMappingCommand.syncMappings(tableData, updateData)
        val expectedOutput = updateData.flatMap { it.value.map { condition -> condition + conditionData } } +
            noOIDData + noOIDData // verify entries missing from updateData were not dropped
        assertThat(output).isEqualTo(expectedOutput)
    }

    @Test
    fun `fetch observation mapping update data`() {
        val responseMap = mapOf(
            "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.828/\$expand" to """
                {
                  "resourceType": "ValueSet",
                  "id": "2.16.840.1.113762.1.4.1146.828",
                  "meta": {
                    "versionId": "13",
                    "lastUpdated": "2022-01-18T01:03:06.000-05:00",
                    "profile": [ "http://hl7.org/fhir/StructureDefinition/shareablevalueset", "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/publishable-valueset-cqfm" ]
                  },
                  "extension": [ {
                    "url": "http://hl7.org/fhir/StructureDefinition/valueset-effectiveDate",
                    "valueDate": "2022-01-18"
                  } ],
                  "url": "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.828",
                  "identifier": [ {
                    "system": "urn:ietf:rfc:3986",
                    "value": "urn:oid:2.16.840.1.113762.1.4.1146.828"
                  } ],
                  "version": "20220118",
                  "name": "Poliovirus Infection (Organism or Substance in Lab Results)",
                  "title": "Poliovirus Infection (Organism or Substance in Lab Results)",
                  "status": "active",
                  "date": "2022-01-18T01:03:06-05:00",
                  "publisher": "CSTE Steward",
                  "expansion": {
                    "identifier": "urn:uuid:069f2f2e-b377-4017-99a5-78e14652e031",
                    "timestamp": "2023-11-22T10:15:22-05:00",
                    "total": 5,
                    "offset": 0,
                    "parameter": [ {
                      "name": "count",
                      "valueInteger": 1000
                    }, {
                      "name": "offset",
                      "valueInteger": 0
                    } ],
                    "contains": [ {
                      "system": "http://snomed.info/sct",
                      "version": "http://snomed.info/sct/731000124108/version/20230901",
                      "code": "16362001",
                      "display": "Human poliovirus 3 (organism)"
                    }, {
                      "system": "http://snomed.info/sct",
                      "version": "http://snomed.info/sct/731000124108/version/20230901",
                      "code": "22580008",
                      "display": "Human poliovirus 1 (organism)"
                    }, {
                      "system": "http://snomed.info/sct",
                      "version": "http://snomed.info/sct/731000124108/version/20230901",
                      "code": "44172002",
                      "display": "Human poliovirus (organism)"
                    }, {
                      "system": "http://snomed.info/sct",
                      "version": "http://snomed.info/sct/731000124108/version/20230901",
                      "code": "55174004",
                      "display": "Human poliovirus 2 (organism)"
                    }, {
                      "system": "http://snomed.info/sct",
                      "version": "http://snomed.info/sct/731000124108/version/20230901",
                      "code": "713616004",
                      "display": "Antigen of Human poliovirus (substance)"
                    } ]
                  }
                }
            """.trimIndent(),
            "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1112/\$expand" to """
                {
                  "resourceType": "ValueSet",
                  "id": "2.16.840.1.113762.1.4.1146.1112",
                  "meta": {
                    "versionId": "5",
                    "lastUpdated": "2019-12-27T01:00:20.000-05:00",
                    "profile": [ "http://hl7.org/fhir/StructureDefinition/shareablevalueset", "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/publishable-valueset-cqfm" ]
                  },
                  "extension": [ {
                    "url": "http://hl7.org/fhir/StructureDefinition/valueset-effectiveDate",
                    "valueDate": "2019-12-27"
                  } ],
                  "url": "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1112",
                  "identifier": [ {
                    "system": "urn:ietf:rfc:3986",
                    "value": "urn:oid:2.16.840.1.113762.1.4.1146.1112"
                  } ],
                  "version": "20191227",
                  "name": "Poliovirus infection (Test Panels for Poliovirus Antibody)",
                  "title": "Poliovirus infection (Test Panels for Poliovirus Antibody)",
                  "status": "active",
                  "date": "2019-12-27T01:00:20-05:00",
                  "publisher": "CSTE Steward",
                  "expansion": {
                    "identifier": "urn:uuid:804fcbb8-70e0-488e-9c4e-9eaef5888c75",
                    "timestamp": "2023-11-22T10:15:22-05:00",
                    "total": 2,
                    "offset": 0,
                    "parameter": [ {
                      "name": "count",
                      "valueInteger": 1000
                    }, {
                      "name": "offset",
                      "valueInteger": 0
                    } ],
                    "contains": [ {
                      "system": "http://loinc.org",
                      "version": "2.76",
                      "code": "41506-7",
                      "display": "Polio virus neutralizing Ab panel - Serum by Neutralization test"
                    }, {
                      "system": "http://loinc.org",
                      "version": "2.76",
                      "code": "68320-1",
                      "display": "Polio virus Ab panel [Titer] - Serum"
                    } ]
                  }
                }
            """.trimIndent()
        )
        val labTestData = mapOf(
            "Member OID" to "2.16.840.1.113762.1.4.1146.828",
            "Name" to "Poliovirus Infection (Organism or Substance in Lab Results)",
            "Code System" to "SNOMEDCT",
            "Version" to "20230901",
            "Status" to "Active"
        )
        val antiBodyData = mapOf(
            "Member OID" to "2.16.840.1.113762.1.4.1146.1112",
            "Name" to "Poliovirus infection (Test Panels for Poliovirus Antibody)",
            "Code System" to "LOINC",
            "Version" to "2.76",
            "Status" to "Active"
        )
        val expectedOutput = mapOf(
            "2.16.840.1.113762.1.4.1146.828" to listOf(
                mapOf("Code" to "16362001", "Descriptor" to "Human poliovirus 3 (organism)") + labTestData,
                mapOf("Code" to "22580008", "Descriptor" to "Human poliovirus 1 (organism)") + labTestData,
                mapOf("Code" to "44172002", "Descriptor" to "Human poliovirus (organism)") + labTestData,
                mapOf("Code" to "55174004", "Descriptor" to "Human poliovirus 2 (organism)") + labTestData,
                mapOf("Code" to "713616004", "Descriptor" to "Antigen of Human poliovirus (substance)") + labTestData
            ),
            "2.16.840.1.113762.1.4.1146.1112" to listOf(
                mapOf(
                    "Code" to "41506-7",
                    "Descriptor" to "Polio virus neutralizing Ab panel - Serum by Neutralization test"
                ) + antiBodyData,
                mapOf("Code" to "68320-1", "Descriptor" to "Polio virus Ab panel [Titer] - Serum") + antiBodyData
            )
        )
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(responseMap.getValue(request.url.toString())),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine)
        val updateData = LookupTableUpdateMappingCommand.fetchLatestTestData(
            listOf("2.16.840.1.113762.1.4.1146.828", "2.16.840.1.113762.1.4.1146.1112"),
            client
        )
        assertThat(updateData).isEqualTo(expectedOutput)
    }
}