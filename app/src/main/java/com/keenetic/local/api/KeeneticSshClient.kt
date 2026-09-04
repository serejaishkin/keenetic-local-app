package com.keenetic.local.api

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

class KeeneticSshClient(
    private val host: String = "192.168.1.1",
    private val port: Int = 22,
    private val login: String,
    private val password: String
) {

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val jsch = JSch()
            val session: Session = jsch.getSession(login, host, port)
            session.setPassword(password)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(15000)

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.inputStream = null

            val outputStream = ByteArrayOutputStream()
            channel.outputStream = outputStream

            val errStream = ByteArrayOutputStream()
            channel.errStream = errStream

            channel.connect(10000)

            val timeout = 30000L
            val start = System.currentTimeMillis()
            while (!channel.isClosed && System.currentTimeMillis() - start < timeout) {
                Thread.sleep(100)
            }

            val output = outputStream.toString("UTF-8")
            val error = errStream.toString("UTF-8")

            channel.disconnect()
            session.disconnect()

            if (error.isNotBlank() && output.isBlank()) {
                throw Exception("SSH Error: $error")
            }
            if (output.isBlank() && error.isNotBlank()) error.trim() else output.trim()
        }
    }

    suspend fun getSystem(): Result<String> = execute("show system")
    suspend fun getClients(): Result<String> = execute("show ip hotspot")
    suspend fun getInterfaces(): Result<String> = execute("show interface")
    suspend fun getLogs(): Result<String> = execute("show log tail 30")
    suspend fun ping(host: String, count: Int = 4): Result<String> = execute("ping $host -c $count")
    suspend fun traceroute(host: String): Result<String> = execute("traceroute $host")
    suspend fun reboot(): Result<String> = execute("system reboot")
    suspend fun getWiFiStatus(): Result<String> = execute("show interface WifiMaster0")
    suspend fun getWiFiClients(): Result<String> = execute("show interface WifiMaster0/AccessPoint0/assoc")
    suspend fun getWiFiGuestStatus(): Result<String> = execute("show interface WifiMaster1")
    suspend fun setWiFiGuest(up: Boolean): Result<String> =
        execute("interface WifiMaster1 ${if (up) "up" else "down"}")
    suspend fun getCpuLoad(): Result<String> = execute("show system cpuload")
    suspend fun getMemory(): Result<String> = execute("show system memory")
}
