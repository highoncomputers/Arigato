package com.arigato.app.domain.entity

enum class ToolCategory(val displayName: String, val icon: String) {
    OSINT("OSINT", "search"),
    NETWORK_SCANNING("Network Scanning", "wifi"),
    WEB_SECURITY("Web Security", "language"),
    PASSWORD_CRACKING("Password Cracking", "lock"),
    WIRELESS("Wireless", "router"),
    EXPLOITATION("Exploitation", "bug_report"),
    FORENSICS("Forensics", "folder_open"),
    MOBILE_SECURITY("Mobile Security", "phone_android"),
    REVERSE_ENGINEERING("Reverse Engineering", "code"),
    CRYPTOGRAPHY("Cryptography", "enhanced_encryption"),
    SOCIAL_ENGINEERING("Social Engineering", "person"),
    OTHER("Other", "build");

    companion object {
        fun fromString(value: String): ToolCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: OTHER
    }
}
