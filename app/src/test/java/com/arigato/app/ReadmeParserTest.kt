package com.arigato.app

import com.arigato.app.core.parser.ParameterDetector
import com.arigato.app.core.parser.RawReadmeData
import com.arigato.app.core.parser.ReadmeParser
import com.arigato.app.domain.entity.ToolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadmeParserTest {
    private lateinit var parser: ReadmeParser
    private lateinit var detector: ParameterDetector

    @Before
    fun setup() {
        detector = ParameterDetector()
        parser = ReadmeParser(detector)
    }

    @Test
    fun `parse extracts tool name from title`() {
        val markdown = """
            # Nmap
            ## ¿Qué es?
            Network scanner tool.
        """.trimIndent()

        val tool = parser.parse(RawReadmeData("nmap", markdown))
        assertEquals("Nmap", tool.name)
    }

    @Test
    fun `parse extracts command examples from code blocks`() {
        val markdown = """
            # Sherlock
            ## ¿Cómo se usa?
            Basic usage:
            ```
            sherlock username
            sherlock -u https://target.com
            ```
        """.trimIndent()

        val tool = parser.parse(RawReadmeData("sherlock", markdown))
        assertTrue(tool.examples.isNotEmpty())
    }

    @Test
    fun `parse infers OSINT category`() {
        val markdown = """
            # GHunt
            ## ¿Qué es?
            An offensive Google framework for OSINT and username investigation.
        """.trimIndent()

        val tool = parser.parse(RawReadmeData("ghunt", markdown))
        assertEquals(ToolCategory.OSINT, tool.toolCategory)
    }

    @Test
    fun `parse infers network scanning category`() {
        val markdown = """
            # Nmap
            ## ¿Qué es?
            A network port scanner and host discovery tool.
        """.trimIndent()

        val tool = parser.parse(RawReadmeData("nmap", markdown))
        assertEquals(ToolCategory.NETWORK_SCANNING, tool.toolCategory)
    }

    @Test
    fun `parse detects root requirement`() {
        val markdown = """
            # Aircrack-ng
            ## ¿Qué es?
            WiFi cracking tool that requires root and monitor mode.
        """.trimIndent()

        val tool = parser.parse(RawReadmeData("aircrack-ng", markdown))
        assertTrue(tool.requiresRoot)
    }

    @Test
    fun `parse generates valid tool id`() {
        val markdown = "# TestTool\n## ¿Qué es?\nA test tool."
        val tool = parser.parse(RawReadmeData("testtool", markdown))
        assertEquals("testtool", tool.id)
        assertEquals("testtool", tool.packageName)
    }
}
