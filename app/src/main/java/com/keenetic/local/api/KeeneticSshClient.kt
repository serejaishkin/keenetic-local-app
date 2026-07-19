package com.keenetic.local.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

class KeeneticSshClient(
    private val host: String = "192.168.1.1",
    private val port: Int = 22,
    private val login: String,
    private val password: String
) {

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(host, port)
            ssh.authPassword(login, password)

            val session = ssh.startSession()
            val cmd = session.exec(command)
            val output = cmd.inputStream.bufferedReader().use { it.readText() }
            val error = cmd.errorStream.bufferedReader().use { it.readText() }

            cmd.join()
            session.close()
            ssh.disconnect()

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