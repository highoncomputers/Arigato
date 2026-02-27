package com.arigato.app.core.output

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedFindings(
    val openPorts: List<PortFinding> = emptyList(),
    val ipAddresses: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val cveIds: List<String> = emptyList(),
    val hashes: List<HashFinding> = emptyList(),
    val hostnames: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = openPorts.isEmpty() && ipAddresses.isEmpty() && urls.isEmpty() &&
                cveIds.isEmpty() && hashes.isEmpty() && hostnames.isEmpty()
}

data class PortFinding(val port: Int, val protocol: String, val state: String, val service: String)

data class HashFinding(val value: String, val type: HashType)

enum class HashType(val length: Int) {
    MD5(32), SHA1(40), SHA256(64), SHA512(128), UNKNOWN(0)
}

@Singleton
class OutputParser @Inject constructor() {
    private val portPattern = Regex("""(\d{1,5})/(tcp|udp)\s+(open|closed|filtered)\s+(\S*)""")
    private val ipPattern = Regex("""\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b""")
    private val urlPattern = Regex("""https?://[^\s'"<>]+""")
    private val cvePattern = Regex("""CVE-\d{4}-\d{4,7}""", RegexOption.IGNORE_CASE)
    private val hashPattern = Regex("""\b([0-9a-fA-F]{32}|[0-9a-fA-F]{40}|[0-9a-fA-F]{64}|[0-9a-fA-F]{128})\b""")
    private val hostnamePattern = Regex("""(?:Host:\s*|Nmap scan report for\s+)([a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z]{2,})+)""")

    fun parse(lines: List<String>): ParsedFindings {
        val text = lines.joinToString("\n")

        val ports = portPattern.findAll(text).map { match ->
            PortFinding(
                port = match.groupValues[1].toIntOrNull() ?: 0,
                protocol = match.groupValues[2],
                state = match.groupValues[3],
                service = match.groupValues[4].ifBlank { "unknown" }
            )
        }.filter { it.port > 0 }.distinctBy { it.port to it.protocol }.toList()

        val ips = ipPattern.findAll(text).map { it.value }
            .filter { !it.startsWith("127.") && it != "0.0.0.0" }
            .distinct().toList()

        val urls = urlPattern.findAll(text).map { it.value }.distinct().toList()

        val cves = cvePattern.findAll(text).map { it.value.uppercase() }.distinct().toList()

        val hashes = hashPattern.findAll(text).map { match ->
            val hex = match.value
            val type = when (hex.length) {
                32 -> HashType.MD5
                40 -> HashType.SHA1
                64 -> HashType.SHA256
                128 -> HashType.SHA512
                else -> HashType.UNKNOWN
            }
            HashFinding(hex.lowercase(), type)
        }.distinctBy { it.value }.toList()

        val hostnames = hostnamePattern.findAll(text).map { it.groupValues[1] }.distinct().toList()

        return ParsedFindings(
            openPorts = ports,
            ipAddresses = ips,
            urls = urls,
            cveIds = cves,
            hashes = hashes,
            hostnames = hostnames
        )
    }

    fun extractTargets(findings: ParsedFindings): Map<String, String> {
        val params = mutableMapOf<String, String>()
        findings.ipAddresses.firstOrNull()?.let { params["target"] = it; params["host"] = it }
        findings.urls.firstOrNull()?.let { params["url"] = it; params["target_url"] = it }
        findings.openPorts.firstOrNull { it.state == "open" }?.let {
            params["port"] = it.port.toString()
        }
        findings.hostnames.firstOrNull()?.let { params["hostname"] = it }
        return params
    }
}
