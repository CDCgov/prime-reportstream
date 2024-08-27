package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.common.Environment

class DownloadReportTest : CoolTest() {
    /**
     * The name of the call
     */
    override val name: String
        get() = "downloadmessagecheck"

    /**
     * Description of the call
     */
    override val description: String
        get() = "Tests edge cases for message download"

    /**
     * Type of test
     */
    override val status: TestStatus
        get() = TODO("Not yet implemented")

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        TODO("Not yet implemented")
    }
}