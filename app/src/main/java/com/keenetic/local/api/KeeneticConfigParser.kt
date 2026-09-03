package com.keenetic.local.api

/**
 * Data structures and parser for KeeneticOS router configuration files (running-config / startup-config).
 */
data class KeeneticParsedConfig(
    val model: String = "",
    val version: String = "",
    val lastChange: String = "",
    val md5Checksum: String = "",
    val username: String = "",
    val hostname: String = "",
    val domainName: String = "",
    val timezone: String = "",
    val knownHosts: List<ConfigKnownHost> = emptyList(),
    val policies: List<ConfigPolicy> = emptyList(),
    val hotspotAssignments: List<ConfigHotspotHost> = emptyList(),
    val interfaces: List<ConfigInterface> = emptyList(),
    val wifiNetworks: List<ConfigWifiNetwork> = emptyList(),
    val dhcpPools: List<ConfigDhcpPool> = emptyList(),
    val dnsUpstreams: List<ConfigDnsUpstream> = emptyList(),
    val routes: List<String> = emptyList(),
    val rawLinesCount: Int = 0
)

data class ConfigKnownHost(
    val name: String,
    val mac: String
)

data class ConfigHotspotHost(
    val mac: String,
    val permitted: Boolean = true,
    val policy: String = ""
)

data class ConfigPolicy(
    val id: String,
    val description: String = "",
    val permitInterfaces: List<String> = emptyList(),
    val noPermitInterfaces: List<String> = emptyList(),
    val isMultipath: Boolean = false
)

data class ConfigInterface(
    val name: String,
    val rename: String = "",
    val description: String = "",
    val ipAddress: String = "",
    val netmask: String = "",
    val securityLevel: String = "",
    val isUp: Boolean = true,
    val includedInterfaces: List<String> = emptyList(),
    val vlanId: Int? = null,
    val proxyUpstream: String = "",
    val proxyProtocol: String = ""
)

data class ConfigWifiNetwork(
    val master: String,
    val accessPoint: String,
    val ssid: String,
    val description: String = "",
    val security: String = "",
    val isUp: Boolean = true,
    val bandSteering: Boolean = false
)

data class ConfigDhcpPool(
    val name: String,
    val startIp: String,
    val endIp: String,
    val bindInterface: String,
    val leaseSeconds: Long = 0
)

data class ConfigDnsUpstream(
    val type: String, // "tls", "https", "plain"
    val upstream: String,
    val sni: String = "",
    val viaInterface: String = ""
)

object KeeneticConfigParser {

    /**
     * Parses a raw Keenetic configuration file text into structured [KeeneticParsedConfig].
     */
    fun parse(text: String): KeeneticParsedConfig {
        var model = ""
        var version = ""
        var lastChange = ""
        var md5Checksum = ""
        var username = ""
        var hostname = ""
        var domainName = ""
        var timezone = ""

        val knownHosts = mutableListOf<ConfigKnownHost>()
        val policies = mutableListOf<ConfigPolicy>()
        val hotspotHosts = mutableListOf<ConfigHotspotHost>()
        val interfaces = mutableListOf<ConfigInterface>()
        val wifiNetworks = mutableListOf<ConfigWifiNetwork>()
        val dhcpPools = mutableListOf<ConfigDhcpPool>()
        val dnsUpstreams = mutableListOf<ConfigDnsUpstream>()
        val routes = mutableListOf<String>()

        val lines = text.lines()
        var currentSection = ""
        var currentSectionArg = ""
        val sectionLines = mutableListOf<String>()

        fun flushSection() {
            if (currentSection.isBlank()) return
            when (currentSection.lowercase()) {
                "interface" -> parseInterfaceBlock(currentSectionArg, sectionLines, interfaces, wifiNetworks)
                "ip policy" -> parsePolicyBlock(currentSectionArg, sectionLines, policies)
                "ip dhcp pool" -> parseDhcpPoolBlock(currentSectionArg, sectionLines, dhcpPools)
                "dns-proxy" -> parseDnsProxyBlock(sectionLines, dnsUpstreams)
            }
            currentSection = ""
            currentSectionArg = ""
            sectionLines.clear()
        }

        var inHotspotBlock = false

        for (rawLine in lines) {
            val line = rawLine.trim()

            // Header metadata comments
            if (line.startsWith("! $$$ Model:", ignoreCase = true)) {
                model = line.substringAfter(":").trim()
                continue
            }
            if (line.startsWith("! $$$ Version:", ignoreCase = true)) {
                version = line.substringAfter(":").trim()
                continue
            }
            if (line.startsWith("! $$$ Last change:", ignoreCase = true)) {
                lastChange = line.substringAfter(":").trim()
                continue
            }
            if (line.startsWith("! $$$ Md5 checksum:", ignoreCase = true)) {
                md5Checksum = line.substringAfter(":").trim()
                continue
            }
            if (line.startsWith("! $$$ Username:", ignoreCase = true)) {
                username = line.substringAfter(":").trim()
                continue
            }

            // Section terminator
            if (line == "!") {
                if (inHotspotBlock) {
                    inHotspotBlock = false
                }
                flushSection()
                continue
            }

            // Standalone commands outside block sections
            if (currentSection.isBlank()) {
                if (line.startsWith("hostname ", ignoreCase = true)) {
                    hostname = line.substringAfter("hostname ").trim()
                    continue
                }
                if (line.startsWith("domainname ", ignoreCase = true)) {
                    domainName = line.substringAfter("domainname ").trim()
                    continue
                }
                if (line.startsWith("clock timezone ", ignoreCase = true)) {
                    timezone = line.substringAfter("clock timezone ").trim()
                    continue
                }
                if (line.startsWith("known host ", ignoreCase = true)) {
                    val parts = line.substringAfter("known host ").trim().split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        knownHosts.add(ConfigKnownHost(name = parts[0], mac = parts[1].lowercase()))
                    }
                    continue
                }
                if (line.startsWith("ip route ", ignoreCase = true)) {
                    routes.add(line.substringAfter("ip route ").trim())
                    continue
                }
                if (line.equals("ip hotspot", ignoreCase = true)) {
                    inHotspotBlock = true
                    continue
                }
                if (inHotspotBlock) {
                    if (line.startsWith("host ", ignoreCase = true)) {
                        val parts = line.substringAfter("host ").trim().split("\\s+".toRegex())
                        if (parts.isNotEmpty()) {
                            val mac = parts[0].lowercase()
                            val isPermit = parts.contains("permit")
                            val policyIndex = parts.indexOf("policy")
                            val policy = if (policyIndex >= 0 && policyIndex + 1 < parts.size) parts[policyIndex + 1] else ""
                            
                            val existingIndex = hotspotHosts.indexOfFirst { it.mac.equals(mac, ignoreCase = true) }
                            if (existingIndex >= 0) {
                                val prev = hotspotHosts[existingIndex]
                                hotspotHosts[existingIndex] = prev.copy(
                                    permitted = isPermit || prev.permitted,
                                    policy = if (policy.isNotBlank()) policy else prev.policy
                                )
                            } else {
                                hotspotHosts.add(ConfigHotspotHost(mac = mac, permitted = isPermit, policy = policy))
                            }
                        }
                    }
                    continue
                }

                // Check for block starts
                if (line.startsWith("interface ", ignoreCase = true)) {
                    currentSection = "interface"
                    currentSectionArg = line.substringAfter("interface ").trim()
                    continue
                }
                if (line.startsWith("ip policy ", ignoreCase = true)) {
                    currentSection = "ip policy"
                    currentSectionArg = line.substringAfter("ip policy ").trim()
                    continue
                }
                if (line.startsWith("ip dhcp pool ", ignoreCase = true)) {
                    currentSection = "ip dhcp pool"
                    currentSectionArg = line.substringAfter("ip dhcp pool ").trim()
                    continue
                }
                if (line.equals("dns-proxy", ignoreCase = true)) {
                    currentSection = "dns-proxy"
                    currentSectionArg = ""
                    continue
                }
            } else {
                // Inside an active block
                sectionLines.add(line)
            }
        }
        flushSection()

        return KeeneticParsedConfig(
            model = model,
            version = version,
            lastChange = lastChange,
            md5Checksum = md5Checksum,
            username = username,
            hostname = hostname,
            domainName = domainName,
            timezone = timezone,
            knownHosts = knownHosts,
            policies = policies,
            hotspotAssignments = hotspotHosts,
            interfaces = interfaces,
            wifiNetworks = wifiNetworks,
            dhcpPools = dhcpPools,
            dnsUpstreams = dnsUpstreams,
            routes = routes,
            rawLinesCount = lines.size
        )
    }

    private fun parseInterfaceBlock(
        name: String,
        lines: List<String>,
        interfaces: MutableList<ConfigInterface>,
        wifiNetworks: MutableList<ConfigWifiNetwork>
    ) {
        var rename = ""
        var description = ""
        var ipAddress = ""
        var netmask = ""
        var securityLevel = ""
        var isUp = true
        val includes = mutableListOf<String>()
        var proxyProtocol = ""
        var proxyUpstream = ""
        var ssid = ""
        var encryption = ""

        for (line in lines) {
            if (line.startsWith("rename ", ignoreCase = true)) rename = line.substringAfter("rename ").trim()
            if (line.startsWith("description ", ignoreCase = true)) description = line.substringAfter("description ").trim().trim('"')
            if (line.startsWith("ip address ", ignoreCase = true)) {
                val parts = line.substringAfter("ip address ").trim().split("\\s+".toRegex())
                if (parts.isNotEmpty()) ipAddress = parts[0]
                if (parts.size >= 2) netmask = parts[1]
            }
            if (line.startsWith("security-level ", ignoreCase = true)) securityLevel = line.substringAfter("security-level ").trim()
            if (line.equals("down", ignoreCase = true)) isUp = false
            if (line.equals("up", ignoreCase = true)) isUp = true
            if (line.startsWith("include ", ignoreCase = true)) includes.add(line.substringAfter("include ").trim())
            if (line.startsWith("proxy protocol ", ignoreCase = true)) proxyProtocol = line.substringAfter("proxy protocol ").trim()
            if (line.startsWith("proxy upstream ", ignoreCase = true)) proxyUpstream = line.substringAfter("proxy upstream ").trim()
            if (line.startsWith("ssid ", ignoreCase = true)) ssid = line.substringAfter("ssid ").trim()
            if (line.startsWith("encryption ", ignoreCase = true)) encryption = line.substringAfter("encryption ").trim()
        }

        // Wi-Fi Access Point interface
        if (name.contains("AccessPoint", ignoreCase = true) && ssid.isNotBlank()) {
            val master = name.substringBefore("/")
            val ap = name.substringAfter("/")
            wifiNetworks.add(
                ConfigWifiNetwork(
                    master = master,
                    accessPoint = ap,
                    ssid = ssid,
                    description = description.ifBlank { rename },
                    security = encryption.ifBlank { "WPA2" },
                    isUp = isUp
                )
            )
        }

        interfaces.add(
            ConfigInterface(
                name = name,
                rename = rename,
                description = description,
                ipAddress = ipAddress,
                netmask = netmask,
                securityLevel = securityLevel,
                isUp = isUp,
                includedInterfaces = includes,
                proxyProtocol = proxyProtocol,
                proxyUpstream = proxyUpstream
            )
        )
    }

    private fun parsePolicyBlock(
        id: String,
        lines: List<String>,
        policies: MutableList<ConfigPolicy>
    ) {
        var description = ""
        val permits = mutableListOf<String>()
        val noPermits = mutableListOf<String>()
        var isMultipath = false

        for (line in lines) {
            if (line.startsWith("description ", ignoreCase = true)) {
                description = line.substringAfter("description ").trim().trim('"')
            }
            if (line.startsWith("permit global ", ignoreCase = true)) {
                permits.add(line.substringAfter("permit global ").trim())
            }
            if (line.startsWith("no permit global ", ignoreCase = true)) {
                noPermits.add(line.substringAfter("no permit global ").trim())
            }
            if (line.equals("multipath", ignoreCase = true)) {
                isMultipath = true
            }
        }

        policies.add(
            ConfigPolicy(
                id = id,
                description = description,
                permitInterfaces = permits,
                noPermitInterfaces = noPermits,
                isMultipath = isMultipath
            )
        )
    }

    private fun parseDhcpPoolBlock(
        name: String,
        lines: List<String>,
        pools: MutableList<ConfigDhcpPool>
    ) {
        var startIp = ""
        var endIp = ""
        var bind = ""
        var lease: Long = 0

        for (line in lines) {
            if (line.startsWith("range ", ignoreCase = true)) {
                val parts = line.substringAfter("range ").trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    startIp = parts[0]
                    endIp = parts[1]
                }
            }
            if (line.startsWith("bind ", ignoreCase = true)) {
                bind = line.substringAfter("bind ").trim()
            }
            if (line.startsWith("lease ", ignoreCase = true)) {
                lease = line.substringAfter("lease ").trim().toLongOrNull() ?: 0
            }
        }

        pools.add(
            ConfigDhcpPool(
                name = name,
                startIp = startIp,
                endIp = endIp,
                bindInterface = bind,
                leaseSeconds = lease
            )
        )
    }

    private fun parseDnsProxyBlock(
        lines: List<String>,
        upstreams: MutableList<ConfigDnsUpstream>
    ) {
        for (line in lines) {
            if (line.startsWith("tls upstream ", ignoreCase = true)) {
                val rest = line.substringAfter("tls upstream ").trim()
                val target = rest.substringBefore(" ")
                val sni = if (rest.contains("sni ")) rest.substringAfter("sni ").substringBefore(" ") else ""
                val via = if (rest.contains("on ")) rest.substringAfter("on ").trim() else ""
                upstreams.add(ConfigDnsUpstream(type = "tls", upstream = target, sni = sni, viaInterface = via))
            } else if (line.startsWith("https upstream ", ignoreCase = true)) {
                val rest = line.substringAfter("https upstream ").trim()
                val target = rest.substringBefore(" ")
                val via = if (rest.contains("on ")) rest.substringAfter("on ").trim() else ""
                upstreams.add(ConfigDnsUpstream(type = "https", upstream = target, viaInterface = via))
            }
        }
    }
}
