package com.arigato.app.core.intelligence

import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import com.arigato.app.domain.entity.ToolSuggestion
import com.arigato.app.domain.entity.Workflow
import com.arigato.app.domain.entity.WorkflowStep
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionEngine @Inject constructor() {
    private val workflowDefinitions: List<Workflow> = buildWorkflows()

    fun suggestNextTools(currentTool: Tool, allTools: List<Tool>): List<ToolSuggestion> {
        val suggestions = mutableListOf<ToolSuggestion>()
        val currentCategory = currentTool.toolCategory

        val categoryChains = mapOf(
            ToolCategory.OSINT to listOf(ToolCategory.NETWORK_SCANNING, ToolCategory.WEB_SECURITY),
            ToolCategory.NETWORK_SCANNING to listOf(ToolCategory.EXPLOITATION, ToolCategory.WEB_SECURITY),
            ToolCategory.WEB_SECURITY to listOf(ToolCategory.EXPLOITATION, ToolCategory.PASSWORD_CRACKING),
            ToolCategory.EXPLOITATION to listOf(ToolCategory.FORENSICS, ToolCategory.REVERSE_ENGINEERING),
            ToolCategory.WIRELESS to listOf(ToolCategory.PASSWORD_CRACKING, ToolCategory.NETWORK_SCANNING)
        )

        categoryChains[currentCategory]?.forEach { nextCategory ->
            allTools.filter { it.toolCategory == nextCategory }
                .take(3)
                .forEach { tool ->
                    suggestions.add(
                        ToolSuggestion(
                            tool = tool,
                            relevance = 0.75f,
                            reason = "Commonly used after ${currentTool.name}"
                        )
                    )
                }
        }

        return suggestions.take(5)
    }

    fun suggestByKeyword(keyword: String, allTools: List<Tool>): List<ToolSuggestion> {
        val lower = keyword.lowercase()
        return allTools
            .filter { tool ->
                tool.name.lowercase().contains(lower) ||
                        tool.description.lowercase().contains(lower) ||
                        tool.tags.any { it.contains(lower) }
            }
            .map { tool ->
                ToolSuggestion(
                    tool = tool,
                    relevance = if (tool.name.lowercase().contains(lower)) 1.0f else 0.6f,
                    reason = "Matches keyword: $keyword"
                )
            }
            .sortedByDescending { it.relevance }
            .take(10)
    }

    fun getWorkflowsForCategory(category: ToolCategory): List<Workflow> =
        workflowDefinitions.filter { it.category == category.name }

    fun getAllWorkflows(): List<Workflow> = workflowDefinitions

    private fun buildWorkflows(): List<Workflow> = listOf(
        Workflow(
            id = "web_pentest",
            name = "Web Application Pentest",
            description = "Comprehensive web application security assessment workflow",
            category = ToolCategory.WEB_SECURITY.name,
            steps = listOf(
                WorkflowStep("nmap", "Nmap", mapOf("port" to "80,443,8080,8443"), "Discover open web ports"),
                WorkflowStep("nikto", "Nikto", emptyMap(), "Scan for common web vulnerabilities"),
                WorkflowStep("gobuster", "Gobuster", mapOf("mode" to "dir"), "Directory enumeration"),
                WorkflowStep("sqlmap", "SQLMap", emptyMap(), "Test for SQL injection"),
                WorkflowStep("burpsuite", "Burp Suite", emptyMap(), "Manual testing with proxy", isOptional = true)
            ),
            tags = listOf("web", "pentest", "owasp")
        ),
        Workflow(
            id = "network_recon",
            name = "Network Reconnaissance",
            description = "Systematic network discovery and enumeration",
            category = ToolCategory.NETWORK_SCANNING.name,
            steps = listOf(
                WorkflowStep("nmap", "Nmap", mapOf("scanType" to "quick"), "Host discovery"),
                WorkflowStep("masscan", "Masscan", emptyMap(), "Fast port scanning"),
                WorkflowStep("nmap", "Nmap", mapOf("scanType" to "aggressive"), "Service version detection"),
                WorkflowStep("nikto", "Nikto", emptyMap(), "Web service analysis", isOptional = true)
            ),
            tags = listOf("network", "recon")
        ),
        Workflow(
            id = "osint_investigation",
            name = "OSINT Investigation",
            description = "Open source intelligence gathering workflow",
            category = ToolCategory.OSINT.name,
            steps = listOf(
                WorkflowStep("sherlock", "Sherlock", emptyMap(), "Username search across platforms"),
                WorkflowStep("ghunt", "GHunt", emptyMap(), "Google account investigation", isOptional = true),
                WorkflowStep("holehe", "Holehe", emptyMap(), "Email account check"),
                WorkflowStep("maltego", "Maltego", emptyMap(), "Visual link analysis", isOptional = true)
            ),
            tags = listOf("osint", "recon", "investigation")
        ),
        Workflow(
            id = "wifi_audit",
            name = "WiFi Security Audit",
            description = "Wireless network security assessment",
            category = ToolCategory.WIRELESS.name,
            steps = listOf(
                WorkflowStep("airmon-ng", "Airmon-ng", emptyMap(), "Enable monitor mode"),
                WorkflowStep("airodump-ng", "Airodump-ng", emptyMap(), "Capture network traffic"),
                WorkflowStep("aircrack-ng", "Aircrack-ng", emptyMap(), "Test WPA/WEP security"),
                WorkflowStep("hashcat", "Hashcat", emptyMap(), "Advanced password cracking", isOptional = true)
            ),
            tags = listOf("wireless", "wifi", "pentest")
        ),
        Workflow(
            id = "mobile_security",
            name = "Mobile App Security Assessment",
            description = "Android/iOS application security testing",
            category = ToolCategory.MOBILE_SECURITY.name,
            steps = listOf(
                WorkflowStep("apktool", "APKTool", emptyMap(), "Decompile APK"),
                WorkflowStep("dex2jar", "Dex2Jar", emptyMap(), "Convert DEX to JAR"),
                WorkflowStep("frida", "Frida", emptyMap(), "Dynamic instrumentation"),
                WorkflowStep("objection", "Objection", emptyMap(), "Runtime mobile exploration")
            ),
            tags = listOf("mobile", "android", "apk")
        )
    )
}
