package com.arigato.app.core.parser

import com.arigato.app.domain.entity.CommandExample
import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.ParameterValidation
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import javax.inject.Inject
import javax.inject.Singleton

data class RawReadmeData(
    val toolId: String,
    val rawMarkdown: String
)

@Singleton
class ReadmeParser @Inject constructor(
    private val parameterDetector: ParameterDetector
) {
    private val codeBlockPattern = Regex("```[\\w]*\\n([\\s\\S]*?)```")
    private val inlineCodePattern = Regex("`([^`]+)`")
    private val headerPattern = Regex("^#{1,6}\\s+(.+)$", RegexOption.MULTILINE)
    private val sectionPattern = Regex("^##\\s+(.+)$", RegexOption.MULTILINE)

    fun parse(data: RawReadmeData): Tool {
        val lines = data.rawMarkdown.lines()
        val name = extractTitle(lines)
        val description = extractSection(data.rawMarkdown, "qué es", "es útil")
            ?: extractSection(data.rawMarkdown, "what is", "use")
            ?: extractFirstParagraph(data.rawMarkdown)
        val useCases = extractUseCases(data.rawMarkdown)
        val commandExamples = extractCommandExamples(data.rawMarkdown)
        val parameters = parameterDetector.detectParameters(commandExamples.map { it.command })
        val category = inferCategory(name, description ?: "", data.rawMarkdown)
        val requiresRoot = detectRootRequirement(data.rawMarkdown)
        val commandTemplate = buildCommandTemplate(data.toolId, parameters)

        return Tool(
            id = data.toolId,
            name = name.ifBlank { data.toolId },
            packageName = data.toolId,
            description = description?.trim() ?: "Security tool: ${data.toolId}",
            useCases = useCases,
            category = category.name,
            parameters = parameters,
            commandTemplate = commandTemplate,
            examples = commandExamples,
            requiresRoot = requiresRoot,
            tags = inferTags(name, description ?: "", category)
        )
    }

    private fun extractTitle(lines: List<String>): String {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ")) {
                return trimmed.removePrefix("# ").trim()
            }
        }
        return ""
    }

    private fun extractSection(markdown: String, startKeyword: String, endKeyword: String): String? {
        val lowerMd = markdown.lowercase()
        val startIdx = lowerMd.indexOf(startKeyword)
        if (startIdx == -1) return null

        val sectionStart = lowerMd.indexOf('\n', startIdx)
        if (sectionStart == -1) return null

        val nextSection = sectionPattern.find(markdown, sectionStart + 1)
        val sectionEnd = nextSection?.range?.first ?: markdown.length

        return markdown.substring(sectionStart + 1, sectionEnd)
            .lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("#") }
            .joinToString(" ")
            .replace(Regex("`[^`]+`")) { it.value.removeSurrounding("`") }
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun extractFirstParagraph(markdown: String): String? {
        val lines = markdown.lines()
        val sb = StringBuilder()
        var started = false
        for (line in lines) {
            if (line.startsWith("#")) {
                if (started) break
                continue
            }
            if (line.isBlank()) {
                if (started) break
            } else {
                started = true
                sb.append(line).append(" ")
            }
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun extractUseCases(markdown: String): List<String> {
        val section = extractSection(markdown, "es útil", "cómo se usa")
            ?: extractSection(markdown, "use", "how")
            ?: return emptyList()

        return section.lines()
            .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("•").trim() }
            .filter { it.isNotBlank() && it.length > 5 }
            .take(8)
    }

    fun extractCommandExamples(markdown: String): List<CommandExample> {
        val examples = mutableListOf<CommandExample>()
        var lastDescription: String? = null

        val lines = markdown.lines()
        var inCodeBlock = false
        val codeLines = mutableListOf<String>()

        for (i in lines.indices) {
            val line = lines[i]
            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeLines.clear()
                    lastDescription = findPrecedingDescription(lines, i)
                } else {
                    inCodeBlock = false
                    val code = codeLines.joinToString("\n").trim()
                    if (code.isNotBlank() && looksLikeCommand(code)) {
                        code.lines().forEach { cmdLine ->
                            val cmd = cmdLine.trim()
                            if (cmd.isNotBlank() && looksLikeCommand(cmd)) {
                                examples.add(CommandExample(cmd, lastDescription))
                            }
                        }
                    }
                    codeLines.clear()
                }
            } else if (inCodeBlock) {
                codeLines.add(line)
            }
        }

        inlineCodePattern.findAll(markdown).forEach { match ->
            val code = match.groupValues[1].trim()
            if (looksLikeCommand(code) && code.length > 5) {
                examples.add(CommandExample(code, null))
            }
        }

        return examples.distinctBy { it.command }.take(10)
    }

    private fun findPrecedingDescription(lines: List<String>, codeBlockIdx: Int): String? {
        for (i in (codeBlockIdx - 1) downTo maxOf(0, codeBlockIdx - 3)) {
            val line = lines[i].trim()
            if (line.isNotBlank() && !line.startsWith("#") && !line.startsWith("```")) {
                return line.removePrefix("-").removePrefix("*").trim()
            }
        }
        return null
    }

    private fun looksLikeCommand(text: String): Boolean {
        val knownTools = listOf(
            "nmap", "sqlmap", "hydra", "aircrack", "metasploit", "msfconsole",
            "nikto", "gobuster", "dirb", "wfuzz", "sherlock", "maltego",
            "burpsuite", "wireshark", "john", "hashcat", "binwalk", "radare2",
            "ghidra", "frida", "objection", "apktool", "dex2jar", "volatility",
            "autopsy", "recon-ng", "bettercap", "wifite", "airmon-ng", "airodump",
            "masscan", "zap", "openvas", "nessus", "exploitdb", "searchsploit"
        )
        val lowerText = text.lowercase()
        return knownTools.any { lowerText.startsWith(it) } ||
                (text.contains("-") && text.contains(" ")) ||
                text.matches(Regex("[a-zA-Z][a-zA-Z0-9_-]+\\s+.*"))
    }

    private fun inferCategory(name: String, description: String, markdown: String): ToolCategory {
        val combined = (name + " " + description + " " + markdown).lowercase()
        return when {
            combined.containsAny("osint", "username", "social media", "sherlock", "maltego", "recon", "footprint") -> ToolCategory.OSINT
            combined.containsAny("wifi", "wireless", "wpa", "wep", "aircrack", "wifite", "deauth") -> ToolCategory.WIRELESS
            combined.containsAny("web", "http", "sql injection", "xss", "sqlmap", "nikto", "gobuster", "dirb") -> ToolCategory.WEB_SECURITY
            combined.containsAny("password", "hash", "crack", "brute", "hydra", "hashcat", "john") -> ToolCategory.PASSWORD_CRACKING
            combined.containsAny("network", "scan", "port", "nmap", "masscan", "ping", "traceroute") -> ToolCategory.NETWORK_SCANNING
            combined.containsAny("exploit", "payload", "metasploit", "msfconsole", "shellcode", "buffer overflow") -> ToolCategory.EXPLOITATION
            combined.containsAny("forensic", "memory", "disk", "volatility", "autopsy", "recovery") -> ToolCategory.FORENSICS
            combined.containsAny("android", "apk", "dex", "smali", "frida", "objection", "mobile") -> ToolCategory.MOBILE_SECURITY
            combined.containsAny("reverse", "disassembl", "decompil", "ghidra", "radare", "ida", "gdb") -> ToolCategory.REVERSE_ENGINEERING
            combined.containsAny("crypto", "encrypt", "decrypt", "cipher", "hash") -> ToolCategory.CRYPTOGRAPHY
            combined.containsAny("phishing", "social engineer", "setoolkit") -> ToolCategory.SOCIAL_ENGINEERING
            else -> ToolCategory.OTHER
        }
    }

    private fun detectRootRequirement(markdown: String): Boolean {
        val lower = markdown.lowercase()
        return lower.containsAny("root", "sudo", "superuser", "monitor mode", "raw socket", "privileged")
    }

    private fun buildCommandTemplate(toolId: String, parameters: List<Parameter>): String {
        val parts = mutableListOf(toolId)
        parameters.filter { it.isRequired }.forEach { param ->
            if (param.flag != null) {
                parts.add("${param.flag} {${param.name}}")
            } else if (param.isPositional) {
                parts.add("{${param.name}}")
            }
        }
        return parts.joinToString(" ")
    }

    private fun inferTags(name: String, description: String, category: ToolCategory): List<String> {
        val tags = mutableSetOf(category.name.lowercase().replace("_", "-"))
        val combined = (name + " " + description).lowercase()
        val tagKeywords = mapOf(
            "python" to "python",
            "network" to "network",
            "web" to "web",
            "wireless" to "wireless",
            "gui" to "gui",
            "cli" to "cli",
            "passive" to "passive-recon",
            "active" to "active-recon"
        )
        tagKeywords.forEach { (keyword, tag) ->
            if (combined.contains(keyword)) tags.add(tag)
        }
        return tags.toList()
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
