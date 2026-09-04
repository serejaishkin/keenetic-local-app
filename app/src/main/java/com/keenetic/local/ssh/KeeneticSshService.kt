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
import java.io.InputStream
import java.util.Properties

/**
 * Result data class for SSH command execution.
 */
data class SshExecutionResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
    val error: String? = null
)

/**
 * Service for secure SSH communication with KeeneticOS routers using the JSch library.
 * Serves as a secondary management method alongside the HTTP/HTTPS RCI REST API.
 */
class KeeneticSshService {

    /**
     * Executes an arbitrary CLI command on the Keenetic router via SSH exec channel.
     *
     * @param host Router IP address or hostname (e.g., 192.168.1.1)
     * @param port SSH port (standard default is 22)
     * @param username Login account (usually "admin" or an account with 'ssh'/'cli' privileges)
     * @param password Account password
     * @param command KeeneticOS command string (e.g., "system reboot" or "show version")
     * @param timeoutMs Connection and execution timeout in milliseconds
     * @return [SshExecutionResult] containing stdout, stderr, and exit status
     */
    suspend fun executeCommand(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        command: String,
        timeoutMs: Int = 10000
    ): SshExecutionResult = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelExec? = null

        try {
            AppLogger.logDebug("KeeneticSshService", "Initiating SSH connection to $username@$host:$port for command: $command")
            val jsch = JSch()

            session = jsch.getSession(username, host, port).apply {
                setPassword(password)
                val config = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                }
                setConfig(config)
                timeout = timeoutMs
                connect(timeoutMs)
            }

            channel = (session.openChannel("exec") as ChannelExec).apply {
                setCommand(if (command.endsWith("\n")) command else "$command\n")
                inputStream = null
                setErrStream(null)
            }

            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()
            channel.outputStream = stdoutStream
            channel.setErrStream(stderrStream)

            channel.connect(timeoutMs)

            // Read output while channel is active
            val startTime = System.currentTimeMillis()
            while (!channel.isClosed) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    break
                }
                Thread.sleep(100)
            }

            val exitStatus = channel.exitStatus
            val stdout = stdoutStream.toString("UTF-8").trim()
            val stderr = stderrStream.toString("UTF-8").trim()
            val combinedOutput = if (stderr.isNotBlank()) "$stdout\n$stderr".trim() else stdout

            AppLogger.logInfo("KeeneticSshService", "SSH command '$command' completed with exit code $exitStatus")
            SshExecutionResult(
                success = exitStatus == 0 || exitStatus == -1, // -1 might happen if socket closed immediately on reboot
                output = combinedOutput,
                exitCode = exitStatus
            )
        } catch (e: JSchException) {
            val errMsg = e.message ?: "JSch SSH connection error"
            AppLogger.logError("KeeneticSshService", e)
            SshExecutionResult(
                success = false,
                output = "",
                exitCode = -1,
                error = errMsg
            )
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unexpected error during SSH execution"
            AppLogger.logError("KeeneticSshService", e)
            SshExecutionResult(
                success = false,
                output = "",
                exitCode = -1,
                error = errMsg
            )
        } finally {
            try {
                channel?.disconnect()
            } catch (_: Exception) {}
            try {
                session?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Sends the 'system reboot' command to the Keenetic router via SSH (JSch).
     *
     * Note: When Keenetic reboots, the SSH daemon terminates immediately, which often causes
     * an EOF or socket disconnection. This is treated as a successful trigger if the command
     * was dispatched successfully.
     *
     * @param host Router IP address
     * @param port SSH port (default 22)
     * @param username Keenetic admin username
     * @param password Keenetic admin password
     * @param timeoutMs Connection timeout in milliseconds
     * @return [SshExecutionResult] indicating whether the reboot was triggered
     */
    suspend fun rebootRouter(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        timeoutMs: Int = 8000
    ): SshExecutionResult = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelExec? = null

        try {
            AppLogger.logInfo("KeeneticSshService", "Sending SSH reboot command ('system reboot') to $username@$host:$port")
            val jsch = JSch()

            session = jsch.getSession(username, host, port).apply {
                setPassword(password)
                val config = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password,keyboard-interactive")
                }
                setConfig(config)
                timeout = timeoutMs
                connect(timeoutMs)
            }

            channel = (session.openChannel("exec") as ChannelExec).apply {
                // 'system reboot' is standard KeeneticOS CLI command
                setCommand("system reboot\n")
            }

            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            channel.outputStream = outStream
            channel.setErrStream(errStream)

            channel.connect(timeoutMs)

            // Wait briefly for the router to accept the command
            var waited = 0
            while (!channel.isClosed && waited < 2500) {
                Thread.sleep(100)
                waited += 100
            }

            val responseText = outStream.toString("UTF-8").trim()
            val message = if (responseText.isNotBlank()) {
                "Команда 'system reboot' передана по SSH: $responseText"
            } else {
                "Команда 'system reboot' успешно отправлена по SSH. Интернет-центр выполняет перезапуск."
            }

            SshExecutionResult(
                success = true,
                output = message,
                exitCode = 0
            )
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // When a router reboots, it abruptly cuts the TCP connection.
            // Strings like "Pipe closed", "session is down", "connection reset", "Socket closed"
            // indicate the router shutdown process began.
            val isRebootDisconnection = msg.contains("Pipe closed", ignoreCase = true) ||
                    msg.contains("session is down", ignoreCase = true) ||
                    msg.contains("Connection reset", ignoreCase = true) ||
                    msg.contains("Socket closed", ignoreCase = true)

            if (isRebootDisconnection) {
                AppLogger.logInfo("KeeneticSshService", "SSH connection closed as expected during router reboot: $msg")
                SshExecutionResult(
                    success = true,
                    output = "Команда перезагрузки принята. Соединение SSH разорвано перезапускающимся роутером.",
                    exitCode = 0
                )
            } else {
                AppLogger.logError("KeeneticSshService", e)
                SshExecutionResult(
                    success = false,
                    output = "",
                    exitCode = -1,
                    error = "Ошибка SSH (${e.javaClass.simpleName}): ${e.message ?: "Не удалось подключиться к порту $port"}"
                )
            }
        } finally {
            try {
                channel?.disconnect()
            } catch (_: Exception) {}
            try {
                session?.disconnect()
            } catch (_: Exception) {}
        }
    }

    /**
     * Executes a real ping from the Keenetic router via SSH CLI.
     * Sends "ping <host> -c <count>" and captures the output.
     */
    suspend fun pingViaSsh(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        target: String,
        count: Int = 4,
        timeoutMs: Int = 15000
    ): SshExecutionResult = executeCommand(
        host = host, port = port, username = username, password = password,
        command = "ping $target -c $count -W 3",
        timeoutMs = timeoutMs
    )

    /**
     * Executes a real traceroute from the Keenetic router via SSH CLI.
     * Sends "traceroute <target>" and captures the output.
     */
    suspend fun tracerouteViaSsh(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        target: String,
        timeoutMs: Int = 30000
    ): SshExecutionResult = executeCommand(
        host = host, port = port, username = username, password = password,
        command = "traceroute $target -m 30",
        timeoutMs = timeoutMs
    )

    /**
     * Executes a DNS lookup from the Keenetic router via SSH CLI.
     * Uses "show ip name-server" to see configured DNS, then "ping <host>" for resolution.
     */
    suspend fun dnsLookupViaSsh(
        host: String,
        port: Int = 22,
        username: String,
        password: String,
        target: String,
        timeoutMs: Int = 10000
    ): SshExecutionResult {
        // First try to resolve via the router's DNS proxy
        val nsResult = executeCommand(
            host = host, port = port, username = username, password = password,
            command = "ping $target -c 1 -W 2",
            timeoutMs = timeoutMs
        )
        // Also get configured DNS servers
        val dnsResult = executeCommand(
            host = host, port = port, username = username, password = password,
            command = "show ip name-server",
            timeoutMs = 5000
        )
        val combinedOutput = buildString {
            appendLine("=== DNS Resolution for: $target ===")
            appendLine()
            if (nsResult.output.isNotBlank()) {
                appendLine(nsResult.output)
            }
            appendLine()
            appendLine("=== Configured DNS Servers ===")
            if (dnsResult.output.isNotBlank()) {
                appendLine(dnsResult.output)
            } else {
                appendLine("(не удалось получить список DNS-серверов)")
            }
        }
        return SshExecutionResult(
            success = nsResult.success,
            output = combinedOutput.trim(),
            exitCode = nsResult.exitCode
        )
    }

    /**
     * Executes `show ip policy` via SSH CLI and parses configured KeeneticOS connection policies.
     */
    suspend fun fetchPoliciesViaSsh(
        host: String,
        port: Int = 22,
        username: String,
        password: String
    ): List<ConnectionPolicy> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ConnectionPolicy>()
        result.add(ConnectionPolicy("", "Основная (по умолчанию)", "Стандартная политика сегмента"))

        val execResult = executeCommand(
            host = host,
            port = port,
            username = username,
            password = password,
            command = "show ip policy"
        )

        if (execResult.success && execResult.output.isNotBlank()) {
            val lines = execResult.output.lines()
            val policyRegex = Regex("""(?:policy,\s*)?name\s*=\s*(\w+)(?:,\s*description\s*=\s*([^:\r\n]+))?""", RegexOption.IGNORE_CASE)
            
            var currentPolicyId: String? = null
            var currentPolicyDesc: String = ""

            for (rawLine in lines) {
                val line = rawLine.trim()
                
                // Format 1: name = Policy0, description = nfqws
                val match = policyRegex.find(line)
                if (match != null) {
                    val id = match.groupValues[1].trim()
                    val desc = match.groupValues.getOrNull(2)?.trim() ?: ""
                    val displayName = if (desc.isNotBlank()) desc else id
                    if (id.isNotBlank() && !id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                        result.add(ConnectionPolicy(id = id, name = displayName, description = desc))
                    }
                    continue
                }

                // Format 2: ip policy <name> blocks from running-config
                if (line.startsWith("ip policy ", ignoreCase = true)) {
                    // Flush previous policy if any
                    currentPolicyId?.let { id ->
                        if (!id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                            result.add(ConnectionPolicy(id = id, name = currentPolicyDesc.ifBlank { id }, description = currentPolicyDesc))
                        }
                    }
                    currentPolicyId = line.substringAfter("ip policy ", "").trim()
                    currentPolicyDesc = ""
                } else if (line.startsWith("description ", ignoreCase = true) && currentPolicyId != null) {
                    currentPolicyDesc = line.substringAfter("description ", "").trim().trim('"')
                } else if (line == "!" && currentPolicyId != null) {
                    val id = currentPolicyId!!
                    if (!id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                        result.add(ConnectionPolicy(id = id, name = currentPolicyDesc.ifBlank { id }, description = currentPolicyDesc))
                    }
                    currentPolicyId = null
                    currentPolicyDesc = ""
                }
            }

            currentPolicyId?.let { id ->
                if (!id.equals("Main", ignoreCase = true) && !id.equals("default", ignoreCase = true)) {
                    result.add(ConnectionPolicy(id = id, name = currentPolicyDesc.ifBlank { id }, description = currentPolicyDesc))
                }
            }
        }
        result.distinctBy { it.id }
    }
}
