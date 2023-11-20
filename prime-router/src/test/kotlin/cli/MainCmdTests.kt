package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.PrintMessage
import com.wolpl.clikttestkit.test
import gov.cdc.prime.router.cli.tests.TestReportStream
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainCmdTests {
    private fun getMockLookupTblListCmd(
        url: String,
        status: HttpStatusCode,
        body: String,
    ): LookupTableListCommand {
        return LookupTableListCommand(
            ApiMockEngine(
                url,
                status,
                body
            ).client()
        )
    }

    @Test
    fun `test prime CLI - commands help`() {
        val assertionErrors = mutableListOf<AssertionError>()
        val commands = listOf(
            ProcessData(),
            ListSchemas(),
            LivdTableUpdate(),
            GenerateDocs(),
            CredentialsCli(),
            CompareCsvFiles(),
            TestReportStream(),
            LoginCommand(),
            LogoutCommand(),
            OrganizationSettings(),
            SenderSettings(),
            ReceiverSettings(),
            MultipleSettings(),
            LookupTableCommands(),
            LookupTableListCommand(),
            LookupTableGetCommand(),
            LookupTableCreateCommand(),
            LookupTableActivateCommand(),
            LookupTableDiffCommand(),
            LookupTableLoadAllCommand(),
            LookupTableCompareMappingCommand(),
            ConvertFileCommands(),
            SenderFilesCommand(),
            ProcessFhirCommands(),
            FhirPathCommand(),
            ConvertValuesetsYamlToCSV(),
            ProcessHl7Commands()
        )

        commands.forEach {
            runBlocking {
                val exception = assertFailsWith<PrintHelpMessage>(
                    block = {
                        it.test(
                            argv = listOf("--help"),
                            expectedExitCode = 0,
                            environmentVariables = mapOf(
                                Pair("POSTGRES_USER", "prime"),
                                Pair("POSTGRES_PASSWORD", "changeIT!"),
                                Pair("POSTGRES_URL", "jdbc:postgresql://localhost:5432/prime_data_hub")
                            )
                        ) {
                        }
                    }
                )
                try {
                    assertNotNull(exception, "Expect PrintHelpMessage thrown for: prime ${it.commandName} --help")
                } catch (ae: AssertionError) {
                    assertionErrors.add(ae)
                }
            }
        }
        // use a home made error collector
        // assert all commands successfully display help text when prime <command name> --help issued
        assertTrue(
            assertionErrors.isEmpty(),
            "Expect all --help will trigger a PrintHelpMessage thrown, " +
                "but there are ${assertionErrors.size} failed to do so, " +
                "errors dump: $assertionErrors"
        )
    }

    @Test
    fun `test prime CLI - lookuptables list connection error`() {
        val lkUpList = LookupTableListCommand()

        runBlocking {
            val exception = assertFailsWith<PrintMessage>(
                block = {
                    lkUpList.test(
                        expectedExitCode = 0,
                        environmentVariables = mapOf(
                            Pair("POSTGRES_USER", "prime"),
                            Pair("POSTGRES_PASSWORD", "changeIT!"),
                            Pair("POSTGRES_URL", "jdbc:postgresql://localhost:5432/prime_data_hub")
                        )
                    ) {
                    }
                }
            )
            assertNotNull(exception.message)
            assertTrue(
                exception.message!!.contains(
                "Error fetching the list of tables: Connection refused"
                ),
                "Expect 'Error fetching the list of tables: Connection refused', " +
                        "but got ${exception.message}"
            )
        }
    }

    @Test
    fun `test prime CLI - lookuptables list`() {
        val tables = """[{
        "lookupTableVersionId" : 6,
        "tableName" : "ethnicity",
        "tableVersion" : 1,
        "isActive" : true,
        "createdBy" : "local@test.com",
        "createdAt" : "2023-11-13T15:38:50.495Z",
        "tableSha256Checksum" : "67a9db3bb62a79b4a9d22126f58eebb15dd99a2a2a81bdf4ff740fa884fd5635"
        }, {
            "lookupTableVersionId" : 9,
            "tableName" : "fhirpath_filter_shorthand",
            "tableVersion" : 1,
            "isActive" : true,
            "createdBy" : "local@test.com",
            "createdAt" : "2023-11-13T15:38:50.598Z",
            "tableSha256Checksum" : "4295f38f1e9bdb233d5086bdae3cf92024815883db3f0a96066580c4ba74fcde"
        }]"""
        val mockLookupTblList = getMockLookupTblListCmd(
            url = "/api/lookuptables/list",
            HttpStatusCode.OK,
            body = tables
        )

        runBlocking {
            mockLookupTblList.test(
                expectedExitCode = 0,
                environmentVariables = mapOf(
                    Pair("POSTGRES_USER", "prime"),
                    Pair("POSTGRES_PASSWORD", "changeIT!"),
                    Pair("POSTGRES_URL", "jdbc:postgresql://localhost:5432/prime_data_hub")
                )
            ) {
                ignoreOutputs()
            }
        }
    }

    @Test
    fun `test prime CLI - RouterCli top cmd`() {
        val cmd = RouterCli()
        runBlocking {
            val exception = assertFailsWith<PrintHelpMessage>(
                block = {
                    cmd.test(
                        argv = listOf("--help"),
                        expectedExitCode = 0,
                        environmentVariables = mapOf(
                            Pair("POSTGRES_USER", "prime"),
                            Pair("POSTGRES_PASSWORD", "changeIT!"),
                            Pair("POSTGRES_URL", "jdbc:postgresql://localhost:5432/prime_data_hub")
                        )
                    ) {
                    }
                }
            )

            assertNotNull(exception, "Expect PrintHelpMessage thrown for: prime --help")
        }
    }
}