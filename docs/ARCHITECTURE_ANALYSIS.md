# Arigato Platform — Architectural Analysis & Advanced Enhancement Design

## 1. Architectural Analysis

### Layer Separation (Score: 9/10)

The codebase faithfully follows Clean Architecture:

```
domain/       ← zero Android dependencies, pure Kotlin
core/         ← business-logic engines (execution, intelligence, parsing)
data/         ← Room + DataStore implementations; repository pattern
ui/           ← Compose screens, ViewModels (MVVM)
di/           ← Hilt modules; single source of truth for wiring
plugins/      ← extension point (IToolPlugin / ToolRegistry)
utils/        ← stateless helpers only
```

Dependency direction is correct throughout: UI → domain ← data, with `core` sitting
beside domain. No circular references were found in the static analysis.

### Strength Assessment

| Area | Strength |
|---|---|
| Coroutine usage | `callbackFlow` + `SharedFlow` correctly bridges blocking I/O |
| Repository pattern | `IToolRepository` / `IExecutionRepository` fully abstract storage |
| Hilt wiring | Singleton scoping is appropriate for executors and caches |
| Serialization | `kotlinx.serialization` with custom `KSerializer` for enums |
| Output streaming | `ProcessManager.globalOutput` enables multi-subscriber fan-out |
| Plugin system | `IToolPlugin` + `ToolRegistry` provides clean extension point |
| Security | `CommandBuilder.sanitize()` strips shell metacharacters |
| DataStore | Replaces SharedPreferences correctly; reactive `Flow` access |

### Identified Bottlenecks

1. **Sequential output flushing** — `ExecutionRepositoryImpl.flushOutputBuffer()` acquires
   a `Mutex` and serializes JSON on every 50 lines. Under high-throughput tools (nmap, masscan),
   this creates write pressure on the main Room thread.

2. **`SuggestionEngine` — static relevance scores** — All suggestions use a hardcoded `0.75f`
   relevance score. There is no feedback loop; the engine cannot learn from actual usage.

3. **`ShellExecutor.isToolInstalled()` — `runtimeManager` reference** — The method calls
   `runtimeManager.getBinPaths()` but `runtimeManager` is not injected (will cause a
   `UninitializedPropertyAccessException` at runtime). This is a latent bug.

4. **`ProcessManager` — no concurrency limit** — Unlimited parallel executions can exhaust
   file descriptors and memory on constrained devices.

5. **`ExecutionRepositoryImpl` — in-memory output buffer** — `outputBuffers` is a plain
   `MutableMap` with `Mutex` protection but is never bounded. Long-running tools with verbose
   output (e.g., Metasploit) can exhaust heap.

6. **No audit trail** — Commands are stored in `ExecutionEntity` but there is no tamper-evident
   log. For a security tool platform this is a compliance gap.

7. **Workflow system is display-only** — `Workflow` / `WorkflowStep` entities exist and render
   on HomeScreen, but there is no orchestration engine to actually execute multi-step workflows.

8. **`SuggestionEngine.suggestNextTools` is category-only** — Suggestions ignore the actual
   parameter values used (e.g., if the user ran nmap on 192.168.1.0/24, suggest masscan on
   the same range automatically).

### Scalability Analysis

| Concern | Current State | Risk |
|---|---|---|
| Tool count (171+) | SQLite FTS-ready; no FTS index yet | Medium — search degrades at 500+ tools |
| Concurrent executions | Unbounded | High |
| Output storage | JSON blob in single column | High at >10 KB per run |
| Plugin loading | In-memory map, no dynamic loading | Low today, Medium at 50+ plugins |
| Navigation | Single back-stack | Low |

---

## 2. Suggested Advanced Features (Implemented as Modular Extensions)

### 2.1 Execution Queue with Priority & Concurrency Control
`core/execution/ExecutionQueue.kt`

A `PriorityQueue`-backed scheduler that limits concurrent tool execution to a configurable
maximum (default: 3). High-priority executions (e.g., manual user triggers) skip ahead of
background workflow steps.

### 2.2 Workflow Orchestration Engine
`core/intelligence/WorkflowOrchestrator.kt`
`domain/usecase/RunWorkflowUseCase.kt`

Drives multi-step workflows end-to-end:
- Executes each `WorkflowStep` in sequence
- Extracts IP/URL/hash outputs from step N and injects them as inputs to step N+1
- Emits real-time `WorkflowEvent` progress updates via `StateFlow`
- Supports skip of optional steps on failure

### 2.3 Structured Output Parser
`core/output/OutputParser.kt`

Parses raw stdout and extracts structured findings:
- Open ports (e.g., `80/tcp open http`)
- IP addresses
- URLs / hostnames
- CVE identifiers
- Hash values (MD5, SHA1, SHA256)
- Credentials (`user:pass` patterns)

Findings are surfaced in the UI via `ParsedOutputCard` and can be piped into subsequent
workflow steps automatically.

### 2.4 Execution Analytics & Usage Intelligence
`core/analytics/ExecutionAnalytics.kt`
`domain/entity/AnalyticsEvent.kt`

Records anonymised usage events locally (no network calls):
- Which tools are executed most frequently
- Average execution duration per tool
- Success / failure rates
- Most-used parameter combinations

Used by `SuggestionEngine` to produce data-driven relevance scores instead of static 0.75f.

### 2.5 Command Audit Logger
`core/security/CommandAuditLogger.kt`

Append-only audit log with:
- Timestamp, tool ID, command hash (SHA-256), exit code
- Written to internal app storage as a rotating log file (max 10 MB, 5 rotations)
- Cannot be cleared through the normal "Clear History" flow
- Exportable as JSON for compliance reporting

### 2.6 Interactive Workflow Execution UI
`ui/screens/WorkflowScreen.kt`
`ui/viewmodel/WorkflowViewModel.kt`
`ui/components/WorkflowStepCard.kt`
`ui/components/ParsedOutputCard.kt`

A dedicated screen for running workflows step-by-step with:
- Visual step progress indicator
- Per-step output accordion
- Auto-populated parameters from previous step's extracted findings
- Skip / Retry / Stop controls

---

## 3. Performance Optimization Strategy

### Output Buffer Bounding
Cap `outputBuffers[id]` at 500 lines. When capacity is reached, flush immediately and
discard oldest lines (ring-buffer semantics). This prevents OOM on verbose tools.

### Room — WAL Mode
Enable Write-Ahead Logging on `AppDatabase`:
```kotlin
Room.databaseBuilder(...)
    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    .build()
```

### FTS Index on ToolEntity
Add a `Room @Fts4` virtual table for `ToolEntity` to accelerate the search query
from `O(n)` LIKE scan to `O(log n)` FTS lookup.

### Coroutine Dispatcher Tuning
`ProcessManager` uses `Dispatchers.IO` (thread-pool of 64). Limit to a bounded pool
(`newFixedThreadPoolContext(8, "execution")`) to prevent over-subscription on ARM devices.

---

## 4. Security Hardening Plan

| Measure | Location | Implementation |
|---|---|---|
| Command injection prevention | `CommandBuilder` | Existing sanitize(); extend to reject Unicode bidirectional overrides |
| Audit log integrity | `CommandAuditLogger` | Append-only file; HMAC-SHA256 over each entry |
| Secure parameter storage | `UserPreferences` | Migrate sensitive fields (API keys) to `EncryptedSharedPreferences` |
| Root escalation guard | `ExecuteToolUseCase` | Verify `RootDetector.isDeviceRooted()` before executing REQUIRES_ROOT tools |
| Output data leak prevention | `OutputViewer` | Add redaction filter for private-key patterns before display |
| Intent validation | `ShellExecutor.launchInTermux` | Validate that the resolved component is exactly `com.termux` |

---

## 5. Future Roadmap

### Phase 2 — Intelligence (Q3)
- ML-based parameter auto-fill from execution history
- Anomaly detection on output patterns (flag unexpected errors)
- Cross-tool data pipeline (scan → exploit → report)

### Phase 3 — Collaboration (Q4)
- Export / import tool definitions and workflow results as JSON bundles
- Session sharing via encrypted QR codes (local network only)

### Phase 4 — Platform (Q1 next year)
- Dynamic plugin loading from signed APKs (Android `DexClassLoader`)
- Custom runtime bundle sideloading (Python packages, Go binaries)
- REST API mode — expose execution engine via localhost HTTP for scripted integration
