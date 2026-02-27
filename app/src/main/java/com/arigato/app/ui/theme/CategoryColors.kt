package com.arigato.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.arigato.app.domain.entity.ToolCategory

@Composable
fun categoryColorFor(category: String): Color {
    return when (ToolCategory.fromString(category)) {
        ToolCategory.OSINT -> CategoryOsint
        ToolCategory.NETWORK_SCANNING -> CategoryNetwork
        ToolCategory.WEB_SECURITY -> CategoryWeb
        ToolCategory.PASSWORD_CRACKING -> CategoryPassword
        ToolCategory.WIRELESS -> CategoryWireless
        ToolCategory.EXPLOITATION -> CategoryExploitation
        ToolCategory.FORENSICS -> CategoryForensics
        ToolCategory.MOBILE_SECURITY -> CategoryMobile
        ToolCategory.REVERSE_ENGINEERING -> CategoryReverse
        ToolCategory.CRYPTOGRAPHY -> CategoryCrypto
        ToolCategory.SOCIAL_ENGINEERING -> CategorySocial
        ToolCategory.OTHER -> CategoryOther
    }
}
