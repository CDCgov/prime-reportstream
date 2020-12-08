package gov.cdc.prime.router.transport

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess
import java.util.Properties

class SftpTransport : Transport {

    override fun send(service: OrganizationService, header: DatabaseAccess.Header, contents: ByteArray): Boolean {

        val (user, pass) = lookupCredentials(service)

        val fileDir = service.transport.filePath.removeSuffix("/")

        // TODO - determine what the filename should be
        val path = "$fileDir/${service.fullName.replace('.', '-')}-${header.task.reportId}.csv"
        val host: String = service.transport.host
        val port: String = service.transport.port

        val jsch = JSch()
        val jschSession = jsch.getSession(user, host, port.toInt())
        val config = Properties()
        config.put("StrictHostKeyChecking", "no")
        config.put("PreferredAuthentications", "password")
        jschSession.setConfig(config)
        jschSession.setPassword(pass)

        jschSession.connect()
        val channelSftp = jschSession.openChannel("sftp") as ChannelSftp

        var success = false

        try {
            channelSftp.connect()
            channelSftp.put(contents.inputStream(), path, ChannelSftp.OVERWRITE)
            success = true
        } finally {
            channelSftp.disconnect()
        }

        return success
    }

    private fun lookupCredentials(service: OrganizationService): Pair<String, String> {

        val envVarLabel = service.fullName.replace(".", "__").replace('-', '_').toUpperCase()

        val user = System.getenv("${envVarLabel}__USER") ?: ""
        val pass = System.getenv("${envVarLabel}__PASS") ?: ""

        if (user.isNullOrBlank() || pass.isNullOrBlank())
            error("Unable to find SFTP credentials for ${service.fullName}")

        return Pair(user, pass)
    }
}