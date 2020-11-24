package gov.cdc.prime.router.transport

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import gov.cdc.prime.router.OrganizationService
        import gov.cdc.prime.router.azure.ReportQueue
import java.util.*

class SftpTransport : Transport{

    override fun send(service: OrganizationService, header: ReportQueue.Header, contents: ByteArray) : Boolean {

        val fileDir = "./upload" // TODO: get a file directory from the transport

        val path = "${fileDir}/${service.fullName.replace( '.', '-')}-${header.id}.csv"
        val host: String = service.transport.host 
        val port: String = service.transport.port
        val user: String = "foo"            // todo: replace with user/password from keystore for the service
        val password: String = "pass"

        val jsch = JSch()
        val jschSession = jsch.getSession(user, host, port.toInt() )
        val config = Properties(); 
        config.put("StrictHostKeyChecking", "no")
        config.put("PreferredAuthentications", "password");
        jschSession.setConfig(config)
        jschSession.setPassword(password)
        
        jschSession.connect()
        val channelSftp = jschSession.openChannel( "sftp" ) as ChannelSftp

        var success = false; 

        try{ 
            channelSftp.connect()
            channelSftp.put(contents.inputStream(), path, ChannelSftp.OVERWRITE )
            success = true
        }
        finally {
            channelSftp.disconnect()
        }

        return success;
    }

}