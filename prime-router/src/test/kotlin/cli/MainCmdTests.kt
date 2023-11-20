package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.PrintMessage
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import com.wolpl.clikttestkit.test
import gov.cdc.prime.router.cli.tests.TestReportStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainCmdTests {

    @Test
    fun `test prime CLI - list happy`() {
        val listSchema = ListSchemas()
        val text = tapSystemOut {
            runBlocking {
                listSchema.test(
                    expectedExitCode = 0,
                    environmentVariables = mapOf(
                        Pair("POSTGRES_USER", "prime"),
                        Pair("POSTGRES_PASSWORD", "changeIT!"),
                        Pair("POSTGRES_URL", "jdbc:postgresql://localhost:5432/prime_data_hub")
                    )
                ) {
                }
            }
        }
        assertTrue(text.contains("Current Hub Schema Library"))
        assertTrue(text.contains("Current Clients (Senders to the Hub)"))
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
}