package com.keenetic.local.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.keenetic.local.api.ConnectionPolicy
import com.keenetic.local.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

data class SshExecutionResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
    val error: String = ""
)

class KeeneticSshService {
    suspend fun executeCommand(host: String, port: Int = 22, username: String, password: String, command: String, timeoutMs: Int = 10000): SshExecutionResult = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            AppLogger.logDebug("KeeneticSshService", "Initiating SSH connection to $username@$host:$port for command: $command")
            val jsch = JSch()
            session = jsch.getSession(username, host, port).apply {
                setPassword(password)
                setConfig(Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                })
                timeout = timeoutMs
                connect(timeoutMs)
            }
            channel = (session.openChannel("exec") as ChannelExec).apply {
                setCommand(if (command.endsWith("\n")) command else "$command\n")
                setInputStream(null)
                setErrStream(null)
            }
            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()
            channel.outputStream = stdoutStream
            channel.setErrStream(stderrStream)
            channel.connect(timeoutMs)

            val startTime = System.currentTimeMillis()
            while (!channel.isClosed) {
                if (System.currentTimeMillis() - startTime > timeoutMs) break
                Thread.sleep(100)
            }
            val exitStatus = channel.exitStatus
            val stdout = stdoutStream.toString("UTF-8").trim()
            val stderr = stderrStream.toString("UTF-8").trim()
            val combinedOutput = if (stderr.isNotBlank()) "$stdout\n$stderr".trim() else stdout
            SshExecutionResult(success = exitStatus == 0 || exitStatus == -1, output = combinedOutput, exitCode = exitStatus)
        } catch (e: JSchException) {
            val errMsg = e.message ?: "JSch SSH connection error"
            AppLogger.logError("KeeneticSshService", e)
            SshExecutionResult(false, "", -1, errMsg)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unexpected error during SSH execution"
            AppLogger.logError("KeeneticSshService", e)
            SshExecutionResult(false, "", -1, errMsg)
        } finally {
            try { channel?.disconnect() } catch (_: Exception) {}
            try { session?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun rebootRouter(host: String, port: Int = 22, username: String, password: String, timeoutMs: Int = 8000): SshExecutionResult = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            val jsch = JSch()
            session = jsch.getSession(username, host, port).apply {
                setPassword(password)
                setConfig(Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                })
                timeout = timeoutMs
                connect(timeoutMs)
            }
            channel = (session.openChannel("exec") as ChannelExec).apply { setCommand("system reboot\n") }
            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            channel.outputStream = outStream
            channel.setErrStream(errStream)
            channel.connect(timeoutMs)
            var waited = 0
            while (!channel.isClosed && waited < 2500) { Thread.sleep(100); waited += 100 }
            val responseText = outStream.toString("UTF-8").trim()
            SshExecutionResult(true, if (responseText.isNotBlank()) "Команда 'system reboot' передана по SSH: $responseText" else "Команда 'system reboot' успешно отправлена по SSH. Интернет-центр выполняет перезапуск.", 0)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val isRebootDisconnection = msg.contains("Pipe closed", true) || msg.contains("session is down", true) || msg.contains("Connection reset", true) || msg.contains("Socket closed", true)
            if (isRebootDisconnection) SshExecutionResult(true, "Команда перезагрузки принята. Соединение SSH разорвано перезапускающимся роутером.", 0)
            else {
                AppLogger.logError("KeeneticSshService", e)
                SshExecutionResult(false, "", -1, "Ошибка SSH (${e.javaClass.simpleName}): ${e.message ?: "Не удалось подключиться к порту $port"}")
            }
        } finally {
            try { channel?.disconnect() } catch (_: Exception) {}
            try { session?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun pingViaSsh(host: String, port: Int = 22, username: String, password: String, target: String, count: Int = 4, timeoutMs: Int = 15000): SshExecutionResult =
        executeCommand(host, port, username, password, "ping $target -c $count -W 3", timeoutMs)

    suspend fun tracerouteViaSsh(host: String, port: Int = 22, username: String, password: String, target: String, timeoutMs: Int = 30000): SshExecutionResult =
        executeCommand(host, port, username, password, "traceroute $target -m 30", timeoutMs)

    suspend fun dnsLookupViaSsh(host: String, port: Int = 22, username: String, password: String, target: String, timeoutMs: Int = 10000): SshExecutionResult {
        val nsResult = executeCommand(host, port, username, password, "ping $target -c 1 -W 2", timeoutMs)
        val dnsResult = executeCommand(host, port, username, password, "show ip name-server", 5000)
        val combinedOutput = buildString {
            appendLine("=== DNS Resolution for: $target ===")
            appendLine()
            if (nsResult.output.isNotBlank()) appendLine(nsResult.output)
            appendLine()
            appendLine("=== Configured DNS Servers ===")
            appendLine(dnsResult.output.ifBlank { "(не удалось получить список DNS-серверов)" })
        }
        return SshExecutionResult(nsResult.success, combinedOutput.trim(), nsResult.exitCode)
    }

    suspend fun fetchPoliciesViaSsh(host: String, port: Int = 22, username: String, password: String): List<ConnectionPolicy> = withContext(Dispatchers.IO) {
        val result = mutableListOf(ConnectionPolicy("", "Основная (по умолчанию)", "Стандартная политика сегмента"))
        val execResult = executeCommand(host, port, username, password, "show ip policy")
        if (execResult.success && execResult.output.isNotBlank()) {
            val policyRegex = Regex("""(?:policy,\s*)?name\s*=\s*(\w+)(?:,\s*description\s*=\s*([^:\r\n]+))?""", RegexOption.IGNORE_CASE)
            var currentId: String? = null
            var currentDesc = ""
            for (rawLine in execResult.output.lines()) {
                val line = rawLine.trim()
                val match = policyRegex.find(line)
                if (match != null) {
                    val id = match.groupValues[1].trim()
                    val desc = match.groupValues.getOrNull(2)?.trim() ?: ""
                    if (id.isNotBlank() && !id.equals("Main", true) && !id.equals("default", true)) result.add(ConnectionPolicy(id, desc.ifBlank { id }, desc))
                    continue
                }
                if (line.startsWith("ip policy ", true)) {
                    currentId?.let { id -> if (!id.equals("Main", true) && !id.equals("default", true)) result.add(ConnectionPolicy(id, currentDesc.ifBlank { id }, currentDesc)) }
                    currentId = line.substringAfter("ip policy ").trim()
                    currentDesc = ""
                } else if (line.startsWith("description ", true) && currentId != null) {
                    currentDesc = line.substringAfter("description ").trim().trim('"')
                } else if (line == "!" && currentId != null) {
                    val id = currentId!!
                    if (!id.equals("Main", true) && !id.equals("default", true)) result.add(ConnectionPolicy(id, currentDesc.ifBlank { id }, currentDesc))
                    currentId = null
                    currentDesc = ""
                }
            }
            currentId?.let { id -> if (!id.equals("Main", true) && !id.equals("default", true)) result.add(ConnectionPolicy(id, currentDesc.ifBlank { id }, currentDesc)) }
        }
        result.distinctBy { it.id }
    }
}
