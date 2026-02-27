# Arigato - Android Security Tools Platform

A production-ready Android application that provides a GUI-based platform for executing 100+ security tools from the i-Haklab repository. Users can configure and execute tools through intuitive forms instead of typing commands manually.

## Features

- **Dynamic Form Generation**: Automatically generates parameter input forms from tool definitions
- **100+ Security Tools**: Pre-configured definitions for tools across all categories
- **Command Builder**: Safely constructs commands from user inputs with injection prevention
- **Live Output Viewer**: Terminal-like output display with ANSI color support
- **Tool Categories**: Organized by category (OSINT, Network, Web, Password, Wireless, Exploitation, etc.)
- **Execution History**: Track all tool executions with timestamps and status
- **Favorites**: Quick access to frequently used tools
- **Workflow Suggestions**: Pre-built security testing workflows
- **Intelligence Layer**: Context-aware tool suggestions
- **Termux Integration**: Execute tools via Termux or directly

## Architecture

Clean Architecture + MVVM with the following layers:

```
app/src/main/java/com/arigato/app/
├── core/
│   ├── execution/       # Process management, shell execution
│   ├── generator/       # Form generation, input validation
│   ├── intelligence/    # Tool suggestions, workflows
│   └── parser/          # README parser, parameter detection
├── data/
│   ├── local/           # Room database, DataStore
│   └── repository/      # Repository implementations
├── di/                  # Hilt dependency injection
├── domain/
│   ├── entity/          # Domain models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business logic
├── plugins/             # Plugin system for custom tools
├── ui/
│   ├── components/      # Reusable Compose components
│   ├── navigation/      # Navigation graph
│   ├── screens/         # App screens
│   ├── theme/           # Material3 theme
│   └── viewmodel/       # ViewModels
└── utils/               # Extensions, helpers
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Async**: Coroutines + Flow
- **Navigation**: Navigation Compose
- **Serialization**: Kotlinx Serialization

## Prerequisites

- Android 7.0+ (API 24+)
- [Termux](https://f-droid.org/packages/com.termux/) installed (for tool execution)
- Security tools installed within Termux

## Security Tools Categories

| Category | Example Tools |
|----------|--------------|
| OSINT | Sherlock, GHunt, Holehe, Maltego |
| Network Scanning | Nmap, Masscan, Nikto |
| Web Security | SQLMap, Burp Suite, Gobuster, DIRB, WFuzz |
| Password Cracking | Hashcat, John the Ripper, Hydra |
| Wireless | Aircrack-ng, Bettercap, Wifite |
| Exploitation | Metasploit, ExploitDB |
| Forensics | Binwalk, Autopsy, Volatility |
| Mobile Security | Frida, Objection, APKTool, Dex2Jar |
| Reverse Engineering | Ghidra, Radare2, GDB |

## Adding Tool Definitions

Create a JSON file in `app/src/main/assets/tools/` following the schema:

```json
{
  "id": "tool-id",
  "name": "Tool Name",
  "packageName": "executable-name",
  "description": "Tool description",
  "category": "WEB_SECURITY",
  "parameters": [
    {
      "name": "paramName",
      "flag": "-f",
      "type": "TEXT",
      "description": "Parameter description",
      "isRequired": true
    }
  ],
  "commandTemplate": "tool-name {paramName}",
  "examples": [
    {"command": "tool-name -f value", "description": "Example"}
  ],
  "requiresRoot": false,
  "tags": ["web", "scanner"]
}
```

## Legal Disclaimer

Arigato is intended for authorized security testing and educational purposes only. Always obtain explicit written permission before testing any system you do not own. Unauthorized access to computer systems is illegal.

## Build

```bash
./gradlew assembleDebug
```
