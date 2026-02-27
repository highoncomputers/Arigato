package com.arigato.app.core.parser

import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.ParameterValidation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParameterDetector @Inject constructor() {
    private val longFlagPattern = Regex("--([a-zA-Z][a-zA-Z0-9-]+)(?:=([\\S]+))?")
    private val shortFlagPattern = Regex("(?<![\\w-])-([a-zA-Z])(?:\\s+([\\S]+))?")
    private val positionalPattern = Regex("<([a-zA-Z][a-zA-Z0-9_-]*)>")
    private val urlPattern = Regex("https?://[\\S]+")
    private val ipPattern = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?:/\\d{1,2})?\\b")
    private val portPattern = Regex("\\b(\\d{1,5})(?:[-,]\\d{1,5})*\\b")

    private val knownFlagTypes = mapOf(
        "u" to ParameterType.URL,
        "url" to ParameterType.URL,
        "target" to ParameterType.TEXT,
        "host" to ParameterType.IP_ADDRESS,
        "p" to ParameterType.PORT,
        "port" to ParameterType.PORT,
        "w" to ParameterType.WORDLIST,
        "wordlist" to ParameterType.WORDLIST,
        "f" to ParameterType.FILE_PATH,
        "file" to ParameterType.FILE_PATH,
        "o" to ParameterType.FILE_PATH,
        "output" to ParameterType.FILE_PATH,
        "password" to ParameterType.PASSWORD,
        "pass" to ParameterType.PASSWORD,
        "P" to ParameterType.PASSWORD,
        "username" to ParameterType.TEXT,
        "user" to ParameterType.TEXT,
        "U" to ParameterType.TEXT,
        "timeout" to ParameterType.NUMBER,
        "threads" to ParameterType.NUMBER,
        "t" to ParameterType.NUMBER
    )

    private val knownFlags = mapOf(
        "-u" to Pair("url", "Target URL"),
        "--url" to Pair("url", "Target URL"),
        "-p" to Pair("port", "Port or port range"),
        "--port" to Pair("port", "Port or port range"),
        "-w" to Pair("wordlist", "Wordlist file path"),
        "--wordlist" to Pair("wordlist", "Wordlist file path"),
        "-o" to Pair("output", "Output file"),
        "--output" to Pair("output", "Output file"),
        "-t" to Pair("threads", "Number of threads"),
        "--threads" to Pair("threads", "Number of threads"),
        "-v" to Pair("verbose", "Verbose output"),
        "--verbose" to Pair("verbose", "Enable verbose output"),
        "-H" to Pair("header", "HTTP header"),
        "--header" to Pair("header", "HTTP header"),
        "-s" to Pair("ssl", "Use SSL/HTTPS"),
        "-d" to Pair("domain", "Target domain"),
        "--domain" to Pair("domain", "Target domain"),
        "-c" to Pair("cookie", "Cookie value"),
        "--cookie" to Pair("cookie", "Cookie value"),
        "-U" to Pair("username", "Username"),
        "--username" to Pair("username", "Username"),
        "-P" to Pair("password", "Password"),
        "--password" to Pair("password", "Password"),
        "-f" to Pair("file", "Input file"),
        "--file" to Pair("file", "Input file"),
        "-r" to Pair("recursive", "Recursive mode"),
        "--recursive" to Pair("recursive", "Enable recursive mode"),
        "-x" to Pair("extension", "File extension"),
        "--extension" to Pair("extension", "File extension"),
        "--timeout" to Pair("timeout", "Request timeout"),
        "--proxy" to Pair("proxy", "Proxy URL"),
        "--user-agent" to Pair("user_agent", "User-Agent string")
    )

    fun detectParameters(commands: List<String>): List<Parameter> {
        val detectedParams = mutableMapOf<String, Parameter>()

        for (command in commands) {
            val tokens = tokenize(command)
            if (tokens.isEmpty()) continue

            var i = 1
            while (i < tokens.size) {
                val token = tokens[i]
                when {
                    token.startsWith("--") -> {
                        val param = parseLongFlag(token, tokens.getOrNull(i + 1))
                        if (param != null) {
                            detectedParams[param.name] = param
                            if (param.flag != null && !tokens.getOrNull(i + 1).orEmpty().startsWith("-")) {
                                i++
                            }
                        }
                    }
                    token.startsWith("-") && token.length == 2 -> {
                        val param = parseShortFlag(token, tokens.getOrNull(i + 1))
                        if (param != null) {
                            detectedParams[param.name] = param
                            if (!tokens.getOrNull(i + 1).orEmpty().startsWith("-")) {
                                i++
                            }
                        }
                    }
                    token.startsWith("<") && token.endsWith(">") -> {
                        val param = parsePositional(token)
                        if (param != null) {
                            detectedParams[param.name] = param
                        }
                    }
                }
                i++
            }
        }

        return detectedParams.values.toList().sortedWith(
            compareBy({ !it.isRequired }, { it.isPositional }, { it.name })
        )
    }

    private fun tokenize(command: String): List<String> =
        command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun parseLongFlag(flag: String, nextToken: String?): Parameter? {
        val flagName = flag.removePrefix("--").split("=").first().trim()
        if (flagName.isBlank()) return null

        val known = knownFlags[flag]
        val paramName = known?.first ?: flagName.replace("-", "_")
        val description = known?.second ?: "Option: $flag"
        val hasValue = nextToken != null && !nextToken.startsWith("-") || flag.contains("=")
        val type = knownFlagTypes[paramName] ?: knownFlagTypes[flagName] ?: inferTypeFromName(flagName)
        val validation = buildValidation(type)

        return if (flag == "--help" || flag == "--version") null
        else Parameter(
            name = paramName,
            flag = flag,
            description = description,
            type = if (hasValue) type.name else ParameterType.FLAG.name,
            isRequired = false,
            validation = validation
        )
    }

    private fun parseShortFlag(flag: String, nextToken: String?): Parameter? {
        val flagChar = flag.removePrefix("-")
        val known = knownFlags[flag]
        val paramName = known?.first ?: flagChar
        val description = known?.second ?: "Flag $flag"
        val type = knownFlagTypes[flagChar] ?: ParameterType.TEXT
        val hasValue = nextToken != null && !nextToken.startsWith("-")

        return if (flagChar in listOf("h", "v")) null
        else Parameter(
            name = paramName,
            flag = flag,
            description = description,
            type = if (hasValue) type.name else ParameterType.FLAG.name,
            isRequired = false,
            validation = buildValidation(type)
        )
    }

    private fun parsePositional(token: String): Parameter? {
        val name = token.removeSurrounding("<", ">").lowercase().replace("-", "_")
        val type = inferTypeFromName(name)
        return Parameter(
            name = name,
            flag = null,
            description = "Target $name",
            type = type.name,
            isRequired = true,
            isPositional = true,
            validation = buildValidation(type)
        )
    }

    fun inferTypeFromName(name: String): ParameterType {
        val lower = name.lowercase()
        return when {
            lower.containsAny("url", "uri", "site", "website") -> ParameterType.URL
            lower.containsAny("ip", "host", "target", "server") -> ParameterType.IP_ADDRESS
            lower.containsAny("port") -> ParameterType.PORT
            lower.containsAny("file", "path", "wordlist", "list", "dict") -> ParameterType.FILE_PATH
            lower.containsAny("pass", "password", "pwd", "secret") -> ParameterType.PASSWORD
            lower.containsAny("thread", "timeout", "count", "retry", "delay", "num") -> ParameterType.NUMBER
            lower.containsAny("cidr", "range", "subnet", "network") -> ParameterType.CIDR
            else -> ParameterType.TEXT
        }
    }

    private fun buildValidation(type: ParameterType): ParameterValidation? = when (type) {
        ParameterType.IP_ADDRESS -> ParameterValidation(
            pattern = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?:/[0-9]{1,2})?$",
            hint = "e.g., 192.168.1.1 or 192.168.0.0/24"
        )
        ParameterType.URL -> ParameterValidation(
            pattern = "^https?://.*",
            hint = "e.g., https://example.com"
        )
        ParameterType.PORT -> ParameterValidation(
            pattern = "^[0-9]+([-,][0-9]+)*$",
            hint = "e.g., 80, 443, or 1-1000",
            minValue = 1.0,
            maxValue = 65535.0
        )
        ParameterType.NUMBER -> ParameterValidation(
            pattern = "^[0-9]+$",
            hint = "Enter a number",
            minValue = 1.0
        )
        ParameterType.CIDR -> ParameterValidation(
            pattern = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}/[0-9]{1,2}$",
            hint = "e.g., 192.168.0.0/24"
        )
        else -> null
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
