package com.arigato.app

import com.arigato.app.core.generator.InputValidator
import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.ParameterValidation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InputValidatorTest {
    private lateinit var validator: InputValidator

    @Before
    fun setup() {
        validator = InputValidator()
    }

    @Test
    fun `validate accepts valid IP address`() {
        val param = Parameter("target", type = ParameterType.IP_ADDRESS.name, isRequired = true)
        val result = validator.validate(param, "192.168.1.1")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate rejects invalid IP address`() {
        val param = Parameter("target", type = ParameterType.IP_ADDRESS.name, isRequired = true)
        val result = validator.validate(param, "999.999.999.999")
        assertFalse(result.isValid)
    }

    @Test
    fun `validate accepts valid URL`() {
        val param = Parameter("url", type = ParameterType.URL.name, isRequired = true)
        val result = validator.validate(param, "https://example.com")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate rejects URL without scheme`() {
        val param = Parameter("url", type = ParameterType.URL.name, isRequired = true)
        val result = validator.validate(param, "example.com")
        assertFalse(result.isValid)
    }

    @Test
    fun `validate accepts valid CIDR`() {
        val param = Parameter("network", type = ParameterType.CIDR.name, isRequired = true)
        val result = validator.validate(param, "192.168.0.0/24")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate rejects command injection`() {
        val param = Parameter("target", type = ParameterType.TEXT.name, isRequired = true)
        val result = validator.validate(param, "192.168.1.1; rm -rf /")
        assertFalse(result.isValid)
    }

    @Test
    fun `validate rejects command injection with pipes`() {
        val param = Parameter("url", type = ParameterType.URL.name, isRequired = true)
        val result = validator.validate(param, "https://example.com | cat /etc/passwd")
        assertFalse(result.isValid)
    }

    @Test
    fun `validate required field fails on empty value`() {
        val param = Parameter("target", type = ParameterType.TEXT.name, isRequired = true)
        val result = validator.validate(param, "")
        assertFalse(result.isValid)
    }

    @Test
    fun `validate optional field passes on empty value`() {
        val param = Parameter("output", type = ParameterType.FILE_PATH.name, isRequired = false)
        val result = validator.validate(param, "")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate accepts valid port`() {
        val param = Parameter("port", type = ParameterType.PORT.name, isRequired = false)
        val result = validator.validate(param, "80")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate accepts port range`() {
        val param = Parameter("port", type = ParameterType.PORT.name, isRequired = false)
        val result = validator.validate(param, "1-1000")
        assertTrue(result.isValid)
    }
}
