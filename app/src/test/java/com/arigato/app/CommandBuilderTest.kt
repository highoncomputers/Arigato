package com.arigato.app

import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.Tool
import com.arigato.app.utils.helpers.CommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CommandBuilderTest {
    private lateinit var commandBuilder: CommandBuilder

    @Before
    fun setup() {
        commandBuilder = CommandBuilder()
    }

    @Test
    fun `build creates command from positional parameter`() {
        val tool = Tool(
            id = "sherlock",
            name = "Sherlock",
            packageName = "sherlock",
            description = "Username search",
            parameters = listOf(
                Parameter("username", type = ParameterType.TEXT.name, isRequired = true, isPositional = true)
            ),
            commandTemplate = "sherlock {username}"
        )
        val command = commandBuilder.build(tool, mapOf("username" to "johndoe"))
        assertEquals("sherlock johndoe", command)
    }

    @Test
    fun `build creates command with flags`() {
        val tool = Tool(
            id = "nmap",
            name = "Nmap",
            packageName = "nmap",
            description = "Network scanner",
            parameters = listOf(
                Parameter("target", type = ParameterType.IP_ADDRESS.name, isRequired = true, isPositional = true),
                Parameter("port", flag = "-p", type = ParameterType.PORT.name, isRequired = false)
            ),
            commandTemplate = ""
        )
        val command = commandBuilder.build(tool, mapOf("target" to "192.168.1.1", "port" to "80,443"))
        assertEquals("nmap -p 80,443 192.168.1.1", command)
    }

    @Test
    fun `sanitize removes dangerous characters`() {
        val result = commandBuilder.sanitize("192.168.1.1; rm -rf /")
        assertFalse(result.contains(";"))
    }

    @Test
    fun `sanitize removes pipe characters`() {
        val result = commandBuilder.sanitize("192.168.1.1 | cat /etc/passwd")
        assertFalse(result.contains("|"))
    }

    @Test
    fun `build handles empty optional params gracefully`() {
        val tool = Tool(
            id = "nmap",
            name = "Nmap",
            packageName = "nmap",
            description = "Network scanner",
            parameters = listOf(
                Parameter("target", type = ParameterType.IP_ADDRESS.name, isRequired = true, isPositional = true),
                Parameter("port", flag = "-p", type = ParameterType.PORT.name, isRequired = false)
            ),
            commandTemplate = ""
        )
        val command = commandBuilder.build(tool, mapOf("target" to "192.168.1.1"))
        assertEquals("nmap 192.168.1.1", command)
    }
}
