package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.test.Test

class SftpTransportTests {

    @Test
    fun `test createDefaultSSHClient`() {
        var sshClient = SftpTransport.createDefaultSSHClient(true)
        assertThat(sshClient).isNotNull()
        assertThat(sshClient.transport.config.keyAlgorithms.size).isEqualTo(10)
        sshClient = SftpTransport.createDefaultSSHClient(false)
        assertThat(sshClient).isNotNull()
        assertThat(sshClient.transport.config.keyAlgorithms.size).isEqualTo(14)
    }
}