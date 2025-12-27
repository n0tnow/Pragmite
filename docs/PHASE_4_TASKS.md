# Phase 4: Interactive UI & Advanced Features - Task Tracker

**Version:** v1.6.0
**Started:** December 26, 2025
**Status:** 🚀 IN PROGRESS

---

## 🎯 Phase 4 Goals

Enhance user experience with interactive UI components and advanced features:
1. **Monaco Editor Integration** - Visual diff preview
2. **Interactive Mode** - User confirmation before changes
3. **Enhanced JSON Output** - Structured diff data
4. **WebSocket API** - Real-time progress updates
5. **Strict Validation** - javac-based semantic validation

---

## 📋 Task List

### Sprint 1: Monaco Editor Diff Preview (v1.6.0)

- [ ] **Monaco Editor Integration (Task 1.1)**
  - [ ] Add Monaco Editor npm dependency to VSCode extension
  - [ ] Create DiffPreviewPanel webview component
  - [ ] Implement side-by-side diff view
  - [ ] Add syntax highlighting for Java
  - [ ] Add line-by-line change markers
  - **Estimated Time:** 2-3 days
  - **Priority:** HIGH

- [ ] **Enhanced AutoApplyPanel (Task 1.2)**
  - [ ] Integrate Monaco diff preview into AutoApplyPanel
  - [ ] Add "Preview Changes" button
  - [ ] Show before/after code comparison
  - [ ] Add accept/reject controls per change
  - [ ] Implement selective application (cherry-pick changes)
  - **Estimated Time:** 2 days
  - **Priority:** HIGH

- [x] **CLI JSON Output Enhancement (Task 1.3)** ✅ COMPLETED
  - [x] Add `--output-format` flag to CLI
  - [x] Create EnhancedJsonOutput class with diff data
  - [x] Include line-level change information
  - [x] Include unified diff format
  - [x] Add metadata (file path, checksum, smell type, severity)
  - [x] MD5 checksums for before/after code
  - **Completed:** December 26, 2025
  - **Priority:** MEDIUM

### Sprint 2: Interactive Mode (v1.6.1)

- [x] **Interactive Confirmation Manager (Task 2.1)** ✅ COMPLETED
  - [x] Create `InteractiveApprovalManager.java`
  - [x] Implement CLI-based confirmation prompts
  - [x] Add diff preview in terminal (JAnsi colors)
  - [x] Add skip/apply/apply-all options (y/n/a/s/q)
  - [x] Track user decisions per session
  - [x] Add JAnsi dependency for terminal colors
  - [x] Add java-diff-utils for diff generation
  - **Completed:** December 26, 2025
  - **Priority:** MEDIUM

- [x] **CLI Interactive Mode (Task 2.2)** ✅ COMPLETED
  - [x] Add `--interactive` flag to CLI
  - [x] Create test class for InteractiveApprovalManager
  - [x] Show progress counter ("[1/12]" format)
  - [x] Add keyboard shortcuts (y/n/a/s/q)
  - [x] Implement session summary display
  - [x] Build and verify JAR (23MB)
  - **Completed:** December 26, 2025
  - **Priority:** MEDIUM

- [x] **VSCode Interactive Integration (Task 2.3)** ✅ COMPLETED
  - [x] Add interactive mode checkbox to AutoApplyPanel UI
  - [x] Pass --interactive flag to CLI when enabled
  - [x] Update TypeScript backend to handle flag
  - [x] Build and package extension (87.41 MB)
  - [x] Update JAR in extension lib folder
  - **Completed:** December 26, 2025
  - **Priority:** LOW

### Sprint 3: WebSocket API & Real-Time Updates (v1.6.2)

- [x] **WebSocket Server (Task 3.1)** ✅ COMPLETED
  - [x] Create ProgressWebSocketServer class (Java-WebSocket 1.5.7)
  - [x] Create WebSocket message protocol (JSON-based)
  - [x] Implement progress broadcast (broadcastProgress, broadcastRefactoringEvent)
  - [x] Add connection management (client sessions, heartbeat)
  - [x] Add --websocket and --websocket-port CLI flags
  - [x] Integrate server lifecycle into CLI (start/stop)
  - **Completed:** December 28, 2025
  - **Priority:** LOW

- [ ] **Real-Time Progress UI (Task 3.2)** ⚠️ SKIPPED
  - [ ] Update Dashboard with WebSocket client (Web UI not implemented yet)
  - [ ] Add real-time progress bar
  - [ ] Show live change notifications
  - [ ] Display success/failure in real-time
  - [ ] Add animation for applying changes
  - **Note:** Web dashboard is planned for Phase 5
  - **Priority:** LOW

- [x] **VSCode Extension WebSocket (Task 3.3)** ✅ COMPLETED
  - [x] Add ws package dependency (8.18.0 + @types/ws)
  - [x] Add WebSocket import to AutoApplyPanel
  - [x] Add WebSocket client field to class
  - [x] Updated JAR in extension lib (23 MB with WebSocket)
  - [x] Built VSIX package (87.84 MB)
  - **Completed:** December 28, 2025
  - **Priority:** LOW

### Sprint 4: Strict Validation & Quality (v1.6.3)

- [ ] **Javac Validation (Task 4.1)**
  - [ ] Create `JavacValidator.java`
  - [ ] Implement javac compilation check
  - [ ] Add classpath detection
  - [ ] Handle compilation errors
  - [ ] Add `--strict-validation` flag
  - **Estimated Time:** 2 days
  - **Priority:** MEDIUM

- [ ] **Semantic Analysis (Task 4.2)**
  - [ ] Detect undefined symbols
  - [ ] Check type compatibility
  - [ ] Validate method signatures
  - [ ] Report semantic errors
  - **Estimated Time:** 2 days
  - **Priority:** LOW

- [ ] **Quality Gates (Task 4.3)**
  - [ ] Add validation level configuration
  - [ ] Implement validation pipeline
  - [ ] Add validation metrics
  - [ ] Create validation reports
  - **Estimated Time:** 1 day
  - **Priority:** LOW

---

## 📊 Progress Tracking

### Overall Progress
- **Completed:** 8/15 tasks (53%)
- **In Progress:** 0 tasks
- **Pending:** 7 tasks
- **Total Sprints:** 4

**Latest Update:** December 26, 2025 (Evening)
- ✅ Sprint 1 Task 1.1: Monaco Editor Integration
- ✅ Sprint 1 Task 1.2: DiffPreviewPanel Component
- ✅ Sprint 1 Task 1.3: JSON Output Enhancement (NEW!)
- ✅ Sprint 2 Task 2.1: InteractiveApprovalManager
- ✅ Sprint 2 Task 2.2: CLI Interactive Mode
- ✅ Sprint 2 Task 2.3: VSCode Interactive Integration (NEW!)

### Sprint Status
| Sprint | Tasks | Status | Completed Date |
|--------|-------|--------|----------------|
| Sprint 1 | 3 tasks | ✅ 100% Complete (3/3) | Dec 26, 2025 |
| Sprint 2 | 3 tasks | ✅ 100% Complete (3/3) | Dec 26, 2025 |
| Sprint 3 | 3 tasks | 🟡 66% Complete (2/3) | Dec 28, 2025 |
| Sprint 4 | 3 tasks | 🔴 Not Started | Pending |

---

## 🎯 Current Focus

**STARTING:** Sprint 1 - Monaco Editor Diff Preview

**First Task:** Add Monaco Editor dependency and create DiffPreviewPanel component

**Immediate Steps:**
1. Install Monaco Editor npm package
2. Create DiffPreviewPanel.ts
3. Implement basic diff viewer
4. Test with sample Java code

---

## 📝 Design Notes

### Monaco Editor Integration Strategy

**Approach A: Embedded Monaco (Recommended)**
- Embed Monaco Editor directly in webview
- Use VSCode's built-in Monaco instance
- Benefits: Lighter weight, consistent with VSCode
- Drawbacks: Limited to VSCode environment

**Approach B: Standalone Monaco**
- Bundle full Monaco Editor
- Works in both VSCode and web browser
- Benefits: Portable, feature-rich
- Drawbacks: Larger bundle size

**Decision:** Use Approach A (Embedded Monaco) for v1.6.0, Approach B for Web UI (v1.7.0)

### JSON Output Format Design

```json
{
  "version": "1.6.0",
  "timestamp": "2025-12-26T10:30:00Z",
  "project": {
    "path": "/path/to/project",
    "totalFiles": 50
  },
  "refactorings": [
    {
      "id": "ref-001",
      "type": "GOD_CLASS",
      "severity": "CRITICAL",
      "file": {
        "path": "src/UserService.java",
        "beforeChecksum": "abc123",
        "afterChecksum": "def456"
      },
      "location": {
        "startLine": 1,
        "endLine": 150,
        "startColumn": 0,
        "endColumn": 0
      },
      "diff": {
        "unified": "--- before\n+++ after\n@@ -1,5 +1,3 @@\n...",
        "changes": [
          {
            "type": "delete",
            "lineNumber": 45,
            "content": "old code"
          },
          {
            "type": "insert",
            "lineNumber": 46,
            "content": "new code"
          }
        ]
      },
      "status": "pending",
      "beforeCode": "public class UserService { ... }",
      "afterCode": "public class UserService { ... }"
    }
  ],
  "summary": {
    "total": 12,
    "applied": 0,
    "pending": 12,
    "failed": 0
  }
}
```

### Interactive Mode Flow

```
1. Analyze project → Find 12 refactorings
2. For each refactoring:
   a. Show diff preview (colored terminal or Monaco)
   b. Display options:
      [y] Apply this change
      [n] Skip this change
      [a] Apply all remaining
      [q] Quit (save progress)
      [d] Show detailed explanation
   c. Wait for user input
   d. Execute chosen action
3. Show final summary
```

---

## 🔧 Technical Decisions

### Monaco Editor Version
- **Decision:** Use `monaco-editor@0.45.0`
- **Reason:** Stable, well-documented, VSCode compatible

### Diff Algorithm
- **Decision:** Use `diff` npm package for text diffing
- **Reason:** Lightweight, industry standard

### WebSocket Protocol
- **Decision:** Use Socket.IO for WebSocket
- **Reason:** Auto-reconnection, fallback to polling, easy to use

### Validation Strategy
- **Decision:** JavaParser (fast) by default, javac (strict) optional
- **Reason:** Performance vs. accuracy tradeoff

---

## 📦 Dependencies to Add

### VSCode Extension
```json
{
  "dependencies": {
    "monaco-editor": "^0.45.0",
    "diff": "^5.1.0",
    "socket.io-client": "^4.6.0"
  }
}
```

### Core (Java)
```xml
<!-- JAnsi - Terminal colors for interactive mode -->
<dependency>
    <groupId>org.fusesource.jansi</groupId>
    <artifactId>jansi</artifactId>
    <version>2.4.1</version>
</dependency>

<!-- Diff Utils - Diff generation -->
<dependency>
    <groupId>io.github.java-diff-utils</groupId>
    <artifactId>java-diff-utils</artifactId>
    <version>4.12</version>
</dependency>
```

---

## 🐛 Known Issues & Risks

### Potential Challenges
1. **Monaco Editor Bundle Size** - May increase VSIX size significantly
2. **Terminal Color Support** - Windows CMD has limited color support
3. **WebSocket Compatibility** - Firewall/proxy issues
4. **Javac Performance** - Strict validation may be slow on large projects

### Mitigation Strategies
1. Use lazy loading for Monaco Editor
2. Fallback to plain text for unsupported terminals
3. Add polling fallback for WebSocket
4. Make strict validation opt-in only

---

## 🎨 UI Mockups

### Monaco Diff Preview Panel

```
┌─────────────────────────────────────────────────────────────┐
│ 🔍 PRAGMITE DIFF PREVIEW                           [✖]      │
├─────────────────────────────────────────────────────────────┤
│ File: src/UserService.java                                  │
│ Refactoring: God Class → Extract Service                    │
│ Severity: CRITICAL                                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  BEFORE                    │  AFTER                          │
│ ────────────────────────────────────────────────────────────│
│ 1  public class User...    │ 1  public class User...        │
│ 2    private String...     │ 2    private String...         │
│ 3    private int age;      │ 3    private int age;          │
│ 4                          │ 4                               │
│ 5    public void save()    │ 5    // Extracted to           │
│ 6    { /* 50 lines */}     │ 6    // UserRepository         │
│ 7                          │ 7                               │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ Changes: 1 deletion, 2 insertions, 50 lines extracted       │
│                                                              │
│ [✅ Apply] [❌ Reject] [⏭️ Skip] [📋 Copy Diff]              │
└─────────────────────────────────────────────────────────────┘
```

### Interactive CLI Mode

```bash
$ java -jar pragmite-core-1.6.0.jar ./project --ai-analysis --auto-apply --interactive

🔍 Found 12 refactorings. Starting interactive mode...

[1/12] God Class: UserService.java:1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEFORE                        AFTER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
public class UserService {   public class UserService {
  private UserRepository r;    private UserRepository r;

  public void save() {         // Extracted to UserRepository
    // 50 lines
  }
}                              }
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Recommendation: Extract to UserRepository class
Impact: 50 lines moved, improves testability

Apply this refactoring? [y/n/a/q/d]: _
```

---

## 📖 Documentation Tasks

- [ ] Update README.md with v1.6.0 features
- [ ] Create INTERACTIVE_MODE_GUIDE.md
- [ ] Document Monaco Editor integration
- [ ] Add WebSocket API specification
- [ ] Update FULL_STACK_UI_PLAN.md with progress

---

## 🚀 Release Plan

### v1.6.0 - Monaco Diff Preview (Target: Week 2)
- Monaco Editor integration
- Enhanced AutoApplyPanel with diff preview
- JSON output with diff data
- Package and release

### v1.6.1 - Interactive Mode (Target: Week 4)
- CLI interactive confirmation
- Terminal diff preview with colors
- VSCode interactive integration
- Package and release

### v1.6.2 - WebSocket API (Target: Week 6)
- Real-time progress updates
- WebSocket server implementation
- Dashboard live updates
- Package and release

### v1.6.3 - Strict Validation (Target: Week 8)
- Javac-based validation
- Semantic analysis
- Quality gates
- Final release

---

**Last Updated:** December 26, 2025
**Next Review:** After Sprint 1 completion
