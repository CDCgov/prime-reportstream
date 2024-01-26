package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
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

    private fun getMockLookupTblGetCmd(
        url: String,
        status: HttpStatusCode,
        body: String,
    ): LookupTableGetCommand {
        return LookupTableGetCommand(
            ApiMockEngine(
                url,
                status,
                body
            ).client()
        )
    }


}