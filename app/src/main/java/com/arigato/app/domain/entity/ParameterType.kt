package com.arigato.app.domain.entity

enum class ParameterType {
    TEXT,
    NUMBER,
    IP_ADDRESS,
    URL,
    FILE_PATH,
    SELECT,
    MULTI_SELECT,
    FLAG,
    BOOLEAN,
    PORT,
    PORT_RANGE,
    WORDLIST,
    PASSWORD,
    CIDR;

    companion object {
        fun fromString(value: String): ParameterType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
    }
}
