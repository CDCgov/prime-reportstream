package gov.cdc.prime.router.transport

import com.jcraft.jsch.*
import sftputils.*
import software.sham.sftp.*
import gov.cdc.prime.router.Receiver

import java.util.*

class SftpTransport {
    
    fun send( transport: Receiver.Transport, contents: ByteArray, fileName: String ){

        val session = initSshClient(transport.host, transport.port)
        val fileDir = "/sftpout"
        val path = "${fileDir}/${fileName}.csv"

        val channel: ChannelSftp = session.openChannel("stfp") as ChannelSftp

        try{ 
            channel.connect()
            SftpUtils.mkdirp(channel, fileDir)
            channel.put( contents.inputStream(),  path )
        }
        finally{
            channel.disconnect()
            session.disconnect()
        }
    }

    private fun initSshClient(
        host: String = "localhost",
        port: String = "22",
        user: String = "tester",
        password: String = "testing"
    ) : Session {
        val jsch = JSch()
        val session = jsch.getSession(user, host, port.toInt() )
        val config = Properties()
        config.setProperty("StrictHostKeyChecking", "no")
        session.setConfig(config)
        session.setPassword(password)
        session.connect()
        return session
    }
}