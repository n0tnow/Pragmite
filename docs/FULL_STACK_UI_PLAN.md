# Pragmite Full Stack UI Implementation Plan

**Version:** v1.6.0 (Future Release)
**Created:** December 26, 2025
**Status:** Planning Phase
**Prerequisites:** Complete Phase 3 (v1.5.0) first

---

## 📋 Executive Summary

This document outlines the comprehensive plan for transforming Pragmite from a CLI-only tool into a full-stack application with both VSCode Extension UI and Web UI, providing a Claude Code-like experience for code smell detection and automatic refactoring.

### Key Objectives

1. **VSCode Extension UI** - Sidebar panel with interactive analysis and diff preview
2. **Web UI** - Browser-based dashboard with Monaco Editor integration
3. **Backend API** - REST + WebSocket for real-time updates
4. **Unified Experience** - Consistent UX across CLI, VSCode, and Web

---

## 🎯 Phase Overview

### Phase 3.1: VSCode Extension Enhancement (v1.6.0)
**Duration Estimate:** 20-30 hours
**Priority:** HIGH
**Reason:** Users already work in VSCode, quickest path to UI adoption

### Phase 4.0: Web UI & Backend API (v1.7.0)
**Duration Estimate:** 40-60 hours
**Priority:** MEDIUM
**Reason:** Platform-independent access, broader user base

### Phase 4.1: Advanced Features (v1.8.0)
**Duration Estimate:** 20-30 hours
**Priority:** LOW
**Reason:** Polish and advanced capabilities

---

## 📦 Phase 3.1: VSCode Extension Enhancement

### Current State Analysis

**Existing Extension** (`pragmite-vscode-extension/`):
```
pragmite-vscode-extension/
├── src/
│   └── extension.ts          # Basic command execution
├── package.json              # Extension manifest
├── lib/
│   └── pragmite-core-1.4.0.jar
└── README.md
```

**Current Capabilities:**
- ✅ Run Pragmite analysis via command palette
- ✅ JAR file bundled with extension
- ❌ No interactive UI
- ❌ No diff preview
- ❌ No real-time feedback

---

### Architecture Design

#### Component Structure

```
pragmite-vscode-extension/
├── src/
│   ├── extension.ts              # Extension entry point
│   ├── panels/
│   │   ├── AnalysisPanel.ts      # Main sidebar panel
│   │   └── DiffPreviewPanel.ts   # Diff comparison view
│   ├── services/
│   │   ├── PragmiteService.ts    # Java process management
│   │   ├── AnalysisService.ts    # Parse analysis results
│   │   └── BackupService.ts      # Backup/rollback operations
│   ├── models/
│   │   ├── CodeSmell.ts          # Code smell model
│   │   ├── AnalysisResult.ts     # Analysis result model
│   │   └── BackupInfo.ts         # Backup information
│   ├── webview/
│   │   ├── index.html            # Webview HTML
│   │   ├── main.css              # Styling
│   │   └── app.js                # Frontend logic
│   └── utils/
│       ├── logger.ts             # Logging utility
│       └── config.ts             # Configuration
├── resources/
│   ├── icons/                    # UI icons
│   └── templates/                # HTML templates
├── lib/
│   └── pragmite-core-1.6.0.jar   # Updated JAR
├── package.json
├── tsconfig.json
└── webpack.config.js             # Bundling config
```

---

### UI/UX Design

#### Sidebar Panel Layout

```
┌─────────────────────────────────────────────────────────┐
│ 🔍 PRAGMITE ANALYSIS                          [⚙️] [🔄] │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ 📊 Project Overview                                      │
│ ┌──────────────────────────────────────────────────┐   │
│ │ Current File: UserService.java                   │   │
│ │ Total Issues: 12                                 │   │
│ │ Last Analysis: 2 minutes ago                     │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
│ 🎛️ Analysis Options                                     │
│ ┌──────────────────────────────────────────────────┐   │
│ │ ☐ Auto-apply refactorings                       │   │
│ │ ☐ Dry-run mode (preview only)                   │   │
│ │ ☑ Create backups                                │   │
│ │ ☐ Strict validation (javac)                     │   │
│ │                                                   │   │
│ │ Severity Filter:                                 │   │
│ │ [☑ Critical] [☑ High] [☑ Medium] [☐ Low]       │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
│ 🎬 Actions                                              │
│ ┌──────────────────────────────────────────────────┐   │
│ │ [▶️ Analyze Current File    ]                    │   │
│ │ [📁 Analyze Entire Project  ]                    │   │
│ │ [🔧 Apply All (0 selected)  ]                    │   │
│ │ [💾 View Backups (10)       ]                    │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
│ 📋 Detected Issues (12)                                 │
│ ┌──────────────────────────────────────────────────┐   │
│ │ 🔴 CRITICAL (2)                                  │   │
│ │ ├─ ⚠️ God Class                       [Line 1]  │   │
│ │ │   245 lines, 18 methods, 7 fields             │   │
│ │ │   Impact: Maintainability ▼                   │   │
│ │ │   Suggestion: Extract into service classes    │   │
│ │ │   [👁️ View] [✅ Apply] [⏭️ Skip] [ℹ️ Info]    │   │
│ │ │                                                │   │
│ │ └─ ⚠️ Long Method                    [Line 45]  │   │
│ │     processUser() - 44 lines, complexity: 12    │   │
│ │     [👁️ View] [✅ Apply] [⏭️ Skip] [ℹ️ Info]    │   │
│ │                                                  │   │
│ │ 🟡 HIGH (7)                          [Expand ▼] │   │
│ │ 🟢 MEDIUM (3)                        [Expand ▼] │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
│ 📊 Statistics                                           │
│ ┌──────────────────────────────────────────────────┐   │
│ │ Files Analyzed: 1                                │   │
│ │ Total Code Smells: 12                            │   │
│ │ Auto-Applied: 7                                  │   │
│ │ Failed: 2                                        │   │
│ │ Success Rate: 77.8%                              │   │
│ └──────────────────────────────────────────────────┘   │
│                                                          │
│ 💾 Recent Backups                                       │
│ ┌──────────────────────────────────────────────────┐   │
│ │ UserService.java  2025-12-26 04:51:31  4.1 KB   │   │
│ │ [🔙 Rollback] [👁️ View] [🗑️ Delete]             │   │
│ └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

#### Diff Preview Panel

When user clicks "View" on an issue:

```
┌─────────────────────────────────────────────────────────┐
│ 🔄 Proposed Changes - UserService.java                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ Issue: Long Method - processUser()                      │
│ Line: 45-89 (44 lines)                                  │
│ Severity: 🔴 CRITICAL                                    │
│                                                          │
│ ┌────────────────┬────────────────┐                    │
│ │   ORIGINAL     │   REFACTORED   │                    │
│ ├────────────────┼────────────────┤                    │
│ │ 45: public ... │ 45: public ... │                    │
│ │ 46:   if (...  │ 46:   return.. │  ← Simplified     │
│ │ 47:     for .. │ 47: }          │                    │
│ │ ...            │                │                    │
│ └────────────────┴────────────────┘                    │
│                                                          │
│ 📊 Metrics:                                             │
│ • Complexity: 12 → 4  (↓67%)                           │
│ • Lines: 44 → 18  (↓59%)                               │
│ • Cyclomatic Complexity: High → Low                     │
│                                                          │
│ ⚡ Performance Impact:                                  │
│ • No performance impact expected                        │
│ • Improves code readability                             │
│                                                          │
│ [✅ Apply This Change] [⏭️ Skip] [🔙 Cancel]           │
└─────────────────────────────────────────────────────────┘
```

---

### Technical Implementation

#### 1. Extension Activation

**package.json - Activation Events:**
```json
{
  "activationEvents": [
    "onLanguage:java",
    "onView:pragmiteAnalysis",
    "onCommand:pragmite.analyze"
  ],
  "contributes": {
    "viewsContainers": {
      "activitybar": [{
        "id": "pragmite-explorer",
        "title": "Pragmite",
        "icon": "resources/pragmite-icon.svg"
      }]
    },
    "views": {
      "pragmite-explorer": [{
        "id": "pragmiteAnalysis",
        "name": "Analysis",
        "type": "webview"
      }]
    },
    "commands": [
      {
        "command": "pragmite.analyze",
        "title": "Analyze with Pragmite"
      },
      {
        "command": "pragmite.analyzeFile",
        "title": "Analyze Current File"
      },
      {
        "command": "pragmite.applyFix",
        "title": "Apply Refactoring"
      },
      {
        "command": "pragmite.viewBackups",
        "title": "View Backups"
      },
      {
        "command": "pragmite.rollback",
        "title": "Rollback Changes"
      }
    ]
  }
}
```

#### 2. Pragmite Service Integration

**PragmiteService.ts:**
```typescript
import * as vscode from 'vscode';
import * as child_process from 'child_process';
import * as path from 'path';

export class PragmiteService {
    private jarPath: string;
    private outputChannel: vscode.OutputChannel;

    constructor(context: vscode.ExtensionContext) {
        this.jarPath = context.asAbsolutePath(
            path.join('lib', 'pragmite-core-1.6.0.jar')
        );
        this.outputChannel = vscode.window.createOutputChannel('Pragmite');
    }

    async analyzeFile(filePath: string): Promise<AnalysisResult> {
        return new Promise((resolve, reject) => {
            const args = [
                '-jar', this.jarPath,
                '--analyze', filePath,
                '--format', 'json',
                '--output', '-'  // stdout
            ];

            const process = child_process.spawn('java', args);

            let stdout = '';
            let stderr = '';

            process.stdout.on('data', (data) => {
                stdout += data.toString();
            });

            process.stderr.on('data', (data) => {
                stderr += data.toString();
                this.outputChannel.appendLine(data.toString());
            });

            process.on('close', (code) => {
                if (code === 0) {
                    try {
                        const result = JSON.parse(stdout);
                        resolve(result);
                    } catch (e) {
                        reject(new Error('Failed to parse analysis results'));
                    }
                } else {
                    reject(new Error(`Analysis failed with code ${code}`));
                }
            });
        });
    }

    async applyFix(fix: CodeSmellFix): Promise<ApplicationResult> {
        // Similar implementation for auto-apply
        const args = [
            '-jar', this.jarPath,
            '--auto-apply',
            '--file', fix.filePath,
            '--fix-id', fix.id,
            '--backup'
        ];

        // Execute and return result
    }

    async listBackups(fileName?: string): Promise<BackupInfo[]> {
        const args = fileName
            ? ['--list-backups-for', fileName]
            : ['--list-backups'];

        // Execute and parse results
    }
}
```

#### 3. Webview Panel Implementation

**AnalysisPanel.ts:**
```typescript
import * as vscode from 'vscode';

export class AnalysisPanel {
    public static currentPanel: AnalysisPanel | undefined;
    private readonly _panel: vscode.WebviewPanel;
    private _disposables: vscode.Disposable[] = [];

    public static createOrShow(
        extensionUri: vscode.Uri,
        analysisResult: AnalysisResult
    ) {
        const column = vscode.ViewColumn.Two;

        if (AnalysisPanel.currentPanel) {
            AnalysisPanel.currentPanel._panel.reveal(column);
            AnalysisPanel.currentPanel.update(analysisResult);
            return;
        }

        const panel = vscode.window.createWebviewPanel(
            'pragmiteAnalysis',
            'Pragmite Analysis',
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [
                    vscode.Uri.joinPath(extensionUri, 'out'),
                    vscode.Uri.joinPath(extensionUri, 'webview')
                ]
            }
        );

        AnalysisPanel.currentPanel = new AnalysisPanel(
            panel,
            extensionUri,
            analysisResult
        );
    }

    private constructor(
        panel: vscode.WebviewPanel,
        extensionUri: vscode.Uri,
        analysisResult: AnalysisResult
    ) {
        this._panel = panel;
        this._panel.webview.html = this._getHtmlForWebview(
            this._panel.webview,
            analysisResult
        );

        // Handle messages from webview
        this._panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'applyFix':
                        this.handleApplyFix(message.fixId);
                        break;
                    case 'viewDiff':
                        this.handleViewDiff(message.fixId);
                        break;
                    case 'rollback':
                        this.handleRollback(message.backupPath);
                        break;
                }
            },
            null,
            this._disposables
        );
    }

    private _getHtmlForWebview(
        webview: vscode.Webview,
        result: AnalysisResult
    ): string {
        const styleUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'webview', 'main.css')
        );
        const scriptUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'webview', 'app.js')
        );

        return `<!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link href="${styleUri}" rel="stylesheet">
            <title>Pragmite Analysis</title>
        </head>
        <body>
            <div id="app" data-analysis='${JSON.stringify(result)}'>
                <!-- React/Vue component will render here -->
            </div>
            <script src="${scriptUri}"></script>
        </body>
        </html>`;
    }

    public update(result: AnalysisResult) {
        this._panel.webview.postMessage({
            command: 'updateAnalysis',
            data: result
        });
    }
}
```

#### 4. Diff Preview Integration

**DiffPreviewPanel.ts:**
```typescript
export class DiffPreviewPanel {
    public static async show(
        originalContent: string,
        modifiedContent: string,
        fileName: string
    ) {
        const originalUri = vscode.Uri.parse(
            `pragmite-original:${fileName}`
        );
        const modifiedUri = vscode.Uri.parse(
            `pragmite-modified:${fileName}`
        );

        // Register text document content providers
        const originalProvider = new class implements vscode.TextDocumentContentProvider {
            provideTextDocumentContent(uri: vscode.Uri): string {
                return originalContent;
            }
        };

        const modifiedProvider = new class implements vscode.TextDocumentContentProvider {
            provideTextDocumentContent(uri: vscode.Uri): string {
                return modifiedContent;
            }
        };

        vscode.workspace.registerTextDocumentContentProvider(
            'pragmite-original',
            originalProvider
        );
        vscode.workspace.registerTextDocumentContentProvider(
            'pragmite-modified',
            modifiedProvider
        );

        // Open diff editor
        await vscode.commands.executeCommand(
            'vscode.diff',
            originalUri,
            modifiedUri,
            `Pragmite: ${fileName} (Original ↔ Refactored)`
        );
    }
}
```

---

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                    VSCode Extension                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  User Action (Analyze File)                             │
│         │                                                │
│         ↓                                                │
│  ┌──────────────────┐                                   │
│  │ PragmiteService  │ ──→ Spawn Java Process            │
│  └──────────────────┘                                   │
│         │                                                │
│         ↓                                                │
│  ┌──────────────────────────────────┐                   │
│  │ java -jar pragmite-core.jar      │                   │
│  │ --analyze file.java              │                   │
│  │ --format json                    │                   │
│  └──────────────────────────────────┘                   │
│         │                                                │
│         ↓ (stdout: JSON)                                │
│  ┌──────────────────┐                                   │
│  │ AnalysisService  │ ──→ Parse JSON Results            │
│  └──────────────────┘                                   │
│         │                                                │
│         ↓                                                │
│  ┌──────────────────┐                                   │
│  │  AnalysisPanel   │ ──→ Display in Webview            │
│  └──────────────────┘                                   │
│         │                                                │
│         ↓                                                │
│  User clicks "View Diff"                                │
│         │                                                │
│         ↓                                                │
│  ┌──────────────────┐                                   │
│  │ DiffPreviewPanel │ ──→ VSCode Diff Editor            │
│  └──────────────────┘                                   │
│         │                                                │
│         ↓                                                │
│  User clicks "Apply"                                    │
│         │                                                │
│         ↓                                                │
│  ┌──────────────────┐                                   │
│  │ PragmiteService  │ ──→ java --auto-apply             │
│  └──────────────────┘                                   │
│         │                                                │
│         ↓                                                │
│  Update Webview + File System                           │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

### JSON Output Format (from CLI)

To enable VSCode integration, CLI needs JSON output:

**New CLI flag:** `--format json`

**Example output:**
```json
{
  "version": "1.6.0",
  "timestamp": "2025-12-26T04:51:31Z",
  "project": {
    "path": "/path/to/project",
    "filesAnalyzed": 1
  },
  "summary": {
    "totalIssues": 12,
    "critical": 2,
    "high": 7,
    "medium": 3,
    "low": 0
  },
  "issues": [
    {
      "id": "SMELL-001",
      "type": "GOD_CLASS",
      "severity": "CRITICAL",
      "file": "UserService.java",
      "lineStart": 1,
      "lineEnd": 245,
      "message": "Class has too many responsibilities",
      "metrics": {
        "lines": 245,
        "methods": 18,
        "fields": 7
      },
      "suggestion": "Extract into separate service classes",
      "refactoredCode": "public class UserService {\n  ...",
      "estimatedImpact": {
        "complexity": "↓67%",
        "maintainability": "+45%"
      }
    }
  ],
  "autoApply": {
    "enabled": false,
    "applied": 0,
    "failed": 0
  }
}
```

---

### Implementation Tasks - Phase 3.1

#### Week 1: Core Infrastructure
- [ ] **Task 1.1**: Add JSON output format to PragmiteCLI
  - Modify `PragmiteCLI.java` to support `--format json`
  - Create `JsonReportGenerator.java`
  - Output analysis results as structured JSON

- [ ] **Task 1.2**: Update VSCode extension structure
  - Create TypeScript project structure
  - Add webpack bundling
  - Setup development environment

- [ ] **Task 1.3**: Implement PragmiteService
  - Java process spawning
  - JSON parsing
  - Error handling

#### Week 2: UI Components
- [ ] **Task 2.1**: Create AnalysisPanel webview
  - HTML/CSS layout
  - Issue list rendering
  - Filter controls

- [ ] **Task 2.2**: Implement DiffPreviewPanel
  - Content provider registration
  - Diff editor integration
  - Before/after comparison

- [ ] **Task 2.3**: Add interactive controls
  - Apply button handler
  - Skip button handler
  - Checkbox filters

#### Week 3: Integration & Testing
- [ ] **Task 3.1**: Wire up two-way communication
  - Webview → Extension messages
  - Extension → Webview updates
  - Real-time feedback

- [ ] **Task 3.2**: Add backup/rollback UI
  - Backup list display
  - Rollback confirmation dialog
  - Success/failure notifications

- [ ] **Task 3.3**: Testing & polish
  - Unit tests for services
  - Integration tests
  - UI/UX refinements

---

## 📦 Phase 4.0: Web UI & Backend API

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Browser (React SPA)                   │
├─────────────────────────────────────────────────────────┤
│                         ↕️ HTTP/WebSocket                │
├─────────────────────────────────────────────────────────┤
│              Spring Boot REST API Server                │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Controllers:                                      │  │
│  │ - AnalysisController                              │  │
│  │ - ProjectController                               │  │
│  │ - BackupController                                │  │
│  └──────────────────────────────────────────────────┘  │
│                         ↕️                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Services:                                         │  │
│  │ - AnalysisService (wraps pragmite-core)          │  │
│  │ - BackupService                                   │  │
│  │ - WebSocketService (real-time updates)           │  │
│  └──────────────────────────────────────────────────┘  │
│                         ↕️                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Pragmite Core Library (existing)                 │  │
│  │ - ProjectAnalyzer                                 │  │
│  │ - CodeApplicator                                  │  │
│  │ - BackupManager                                   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

### Technology Stack

#### Frontend
- **Framework:** React 18 + TypeScript
- **UI Library:** Ant Design / Material-UI
- **Code Editor:** Monaco Editor (VSCode's web editor)
- **State Management:** Redux Toolkit / Zustand
- **API Client:** Axios
- **WebSocket:** Socket.IO Client
- **Build Tool:** Vite
- **Styling:** Tailwind CSS

#### Backend
- **Framework:** Spring Boot 3.2
- **WebSocket:** Spring WebSocket + STOMP
- **API Docs:** Swagger/OpenAPI
- **Authentication:** JWT (optional for v1.7.0)
- **Database:** PostgreSQL (for user projects, settings)
- **Core Integration:** pragmite-core JAR

---

### Project Structure

```
pragmite-web/
├── backend/                          # Spring Boot API
│   ├── src/main/java/com/pragmite/web/
│   │   ├── PragmiteWebApplication.java
│   │   ├── config/
│   │   │   ├── WebSocketConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── controller/
│   │   │   ├── AnalysisController.java
│   │   │   ├── ProjectController.java
│   │   │   ├── BackupController.java
│   │   │   └── WebSocketController.java
│   │   ├── service/
│   │   │   ├── AnalysisService.java
│   │   │   ├── BackupService.java
│   │   │   └── ProjectService.java
│   │   ├── model/
│   │   │   ├── AnalysisRequest.java
│   │   │   ├── AnalysisResponse.java
│   │   │   └── Project.java
│   │   └── dto/
│   │       └── (Data Transfer Objects)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/           # Flyway migrations
│   └── pom.xml
│
├── frontend/                         # React SPA
│   ├── src/
│   │   ├── components/
│   │   │   ├── Dashboard.tsx
│   │   │   ├── AnalysisPanel.tsx
│   │   │   ├── CodeEditor.tsx      # Monaco Editor
│   │   │   ├── IssueList.tsx
│   │   │   ├── DiffViewer.tsx
│   │   │   └── BackupManager.tsx
│   │   ├── services/
│   │   │   ├── api.ts              # Axios instance
│   │   │   └── websocket.ts        # WebSocket client
│   │   ├── store/
│   │   │   ├── analysisSlice.ts
│   │   │   ├── projectSlice.ts
│   │   │   └── store.ts
│   │   ├── hooks/
│   │   │   ├── useAnalysis.ts
│   │   │   └── useWebSocket.ts
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── public/
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
└── docker/
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── docker-compose.yml
```

---

### API Endpoints Design

#### 1. Analysis Endpoints

**POST /api/v1/analysis/analyze**
```json
Request:
{
  "projectPath": "/path/to/project",
  "files": ["UserService.java"],
  "options": {
    "autoApply": false,
    "dryRun": true,
    "createBackup": true
  }
}

Response:
{
  "analysisId": "uuid-1234",
  "status": "IN_PROGRESS",
  "websocketTopic": "/topic/analysis/uuid-1234"
}
```

**GET /api/v1/analysis/{analysisId}**
```json
Response:
{
  "analysisId": "uuid-1234",
  "status": "COMPLETED",
  "summary": {
    "totalIssues": 12,
    "critical": 2,
    "high": 7
  },
  "issues": [...]
}
```

**POST /api/v1/analysis/apply-fix**
```json
Request:
{
  "issueId": "SMELL-001",
  "filePath": "UserService.java",
  "createBackup": true
}

Response:
{
  "success": true,
  "backupPath": "/tmp/backup-12345",
  "message": "Refactoring applied successfully"
}
```

#### 2. Project Endpoints

**GET /api/v1/projects**
```json
Response:
{
  "projects": [
    {
      "id": "proj-1",
      "name": "My Java Project",
      "path": "/path/to/project",
      "lastAnalyzed": "2025-12-26T04:51:31Z",
      "issueCount": 12
    }
  ]
}
```

**POST /api/v1/projects**
```json
Request:
{
  "name": "My Java Project",
  "path": "/path/to/project"
}
```

#### 3. Backup Endpoints

**GET /api/v1/backups**
```json
Response:
{
  "backups": [
    {
      "id": "backup-1",
      "fileName": "UserService.java",
      "createdAt": "2025-12-26T04:51:31Z",
      "size": 4096
    }
  ]
}
```

**POST /api/v1/backups/rollback**
```json
Request:
{
  "backupId": "backup-1"
}
```

#### 4. WebSocket Topics

**Topic:** `/topic/analysis/{analysisId}`
```json
{
  "type": "PROGRESS",
  "message": "Analyzing file 3 of 10...",
  "progress": 30
}

{
  "type": "ISSUE_FOUND",
  "issue": { ... }
}

{
  "type": "COMPLETE",
  "summary": { ... }
}
```

---

### Frontend UI Design

#### Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ 🔍 PRAGMITE                    [Projects ▼] [Settings] [Docs]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌──────────────┬───────────────────────────────────────────┐   │
│ │  SIDEBAR     │          MAIN CONTENT AREA                │   │
│ │              │                                            │   │
│ │ 📂 Projects  │  ┌──────────────────────────────────────┐ │   │
│ │ • Project 1  │  │  📊 Analysis Dashboard               │ │   │
│ │ • Project 2  │  │                                       │ │   │
│ │              │  │  Project: My Java App                │ │   │
│ │ 🔍 Analysis  │  │  Files: 45  Issues: 127              │ │   │
│ │ 📊 Stats     │  │                                       │ │   │
│ │ 💾 Backups   │  │  [▶️ Run Analysis] [🔧 Apply All]   │ │   │
│ │ ⚙️ Settings  │  └──────────────────────────────────────┘ │   │
│ │              │                                            │   │
│ │              │  ┌──────────────────────────────────────┐ │   │
│ │              │  │  📋 Issues (127)                     │ │   │
│ │              │  │                                       │ │   │
│ │              │  │  🔴 CRITICAL (12)                    │ │   │
│ │              │  │  🟡 HIGH (45)                        │ │   │
│ │              │  │  🟢 MEDIUM (70)                      │ │   │
│ │              │  │                                       │ │   │
│ │              │  │  [Filter] [Sort] [Export]            │ │   │
│ │              │  └──────────────────────────────────────┘ │   │
│ │              │                                            │   │
│ │              │  ┌──────────────────────────────────────┐ │   │
│ │              │  │  📈 Metrics Over Time                │ │   │
│ │              │  │                                       │ │   │
│ │              │  │  [Line Chart: Issues Trend]          │ │   │
│ │              │  └──────────────────────────────────────┘ │   │
│ └──────────────┴───────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

#### Code Editor View (Monaco Integration)

```
┌─────────────────────────────────────────────────────────────────┐
│ UserService.java                            [Save] [Cancel]     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ┌────────────────────┬────────────────────┐                    │
│ │   ORIGINAL CODE    │  REFACTORED CODE   │                    │
│ ├────────────────────┼────────────────────┤                    │
│ │  1: package com..  │  1: package com..  │                    │
│ │  2:                │  2:                │                    │
│ │  3: public class   │  3: public class   │                    │
│ │ 45: public void .. │ 45: public User ..│ ← Changed          │
│ │ 46:   if (user ==..│ 46:   return ...  │                    │
│ │ 47:     throw ...  │ 47: }             │                    │
│ │ 48:   for (int ... │                   │ ← Removed          │
│ │ ...                │ ...               │                    │
│ └────────────────────┴────────────────────┘                    │
│                                                                  │
│ 📊 Impact Analysis:                                             │
│ • Complexity: 12 → 4  (↓67%)                                   │
│ • Lines: 44 → 18  (↓59%)                                       │
│ • Maintainability Index: 45 → 78  (+73%)                       │
│                                                                  │
│ [✅ Apply Changes] [⏭️ Skip] [💾 Create Backup First]         │
└─────────────────────────────────────────────────────────────────┘
```

---

### WebSocket Real-Time Updates

**Flow:**
1. User clicks "Analyze Project"
2. Frontend sends POST `/api/v1/analysis/analyze`
3. Backend returns `analysisId` and WebSocket topic
4. Frontend subscribes to `/topic/analysis/{analysisId}`
5. Backend streams progress updates:
   - File X of Y being analyzed
   - Issue found (real-time)
   - Progress percentage
6. Frontend updates UI in real-time
7. Backend sends COMPLETE message
8. Frontend displays final results

**WebSocketService.ts:**
```typescript
import { Client, StompSubscription } from '@stomp/stompjs';

export class WebSocketService {
    private client: Client;

    connect(onMessage: (msg: any) => void) {
        this.client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            onConnect: () => {
                console.log('WebSocket connected');
            }
        });
        this.client.activate();
    }

    subscribeToAnalysis(
        analysisId: string,
        onMessage: (msg: any) => void
    ): StompSubscription {
        return this.client.subscribe(
            `/topic/analysis/${analysisId}`,
            (message) => {
                const data = JSON.parse(message.body);
                onMessage(data);
            }
        );
    }
}
```

---

### Implementation Tasks - Phase 4.0

#### Week 1-2: Backend API
- [ ] **Task 4.1**: Setup Spring Boot project
  - Initialize project structure
  - Add dependencies (WebSocket, JPA, etc.)
  - Configure CORS and security

- [ ] **Task 4.2**: Implement REST controllers
  - AnalysisController
  - ProjectController
  - BackupController

- [ ] **Task 4.3**: WebSocket integration
  - Configure STOMP
  - Implement real-time progress updates
  - Testing WebSocket communication

#### Week 3-4: Frontend Development
- [ ] **Task 4.4**: Setup React + Vite project
  - Project initialization
  - Configure Monaco Editor
  - Setup Redux store

- [ ] **Task 4.5**: Implement core components
  - Dashboard layout
  - Analysis panel
  - Code editor (Monaco)
  - Diff viewer

- [ ] **Task 4.6**: API integration
  - Axios setup
  - WebSocket client
  - State management

#### Week 5-6: Integration & Deployment
- [ ] **Task 4.7**: End-to-end testing
  - Integration tests
  - E2E tests with Cypress
  - Performance testing

- [ ] **Task 4.8**: Docker containerization
  - Backend Dockerfile
  - Frontend Dockerfile
  - Docker Compose setup

- [ ] **Task 4.9**: Deployment
  - CI/CD pipeline
  - Production deployment
  - Documentation

---

## 📦 Phase 4.1: Advanced Features

### Features to Add

#### 1. Collaborative Features
- [ ] Multi-user support
- [ ] Real-time collaboration (multiple users analyzing same project)
- [ ] Comment system on code smells
- [ ] Team analytics dashboard

#### 2. AI Enhancement
- [ ] Custom refactoring rules (user-defined)
- [ ] Learning from user preferences
- [ ] Smart suggestions based on project type
- [ ] Automated priority ranking

#### 3. Integration Features
- [ ] GitHub integration (analyze PRs)
- [ ] GitLab integration
- [ ] CI/CD pipeline integration
- [ ] Slack/Teams notifications

#### 4. Advanced Analytics
- [ ] Code quality trends over time
- [ ] Team performance metrics
- [ ] Technical debt estimation
- [ ] Custom reporting

---

## 🎯 Comparison: Why Each Approach?

### CLI (v1.0-1.5)
**Pros:**
- ✅ Fast, no UI overhead
- ✅ Scriptable, CI/CD friendly
- ✅ Low resource usage

**Cons:**
- ❌ No visual feedback
- ❌ Hard to compare diffs
- ❌ Not user-friendly for beginners

**Best For:** Power users, CI/CD pipelines, automation

---

### VSCode Extension (v1.6)
**Pros:**
- ✅ Integrated into developer workflow
- ✅ Built-in diff editor
- ✅ Familiar UI for developers
- ✅ Offline capable

**Cons:**
- ❌ VSCode-only
- ❌ Limited collaboration features
- ❌ Can't access from mobile/tablet

**Best For:** Individual developers, local development

---

### Web UI (v1.7)
**Pros:**
- ✅ Platform-independent
- ✅ Accessible anywhere (cloud)
- ✅ Team collaboration
- ✅ Rich visualizations
- ✅ Mobile-friendly

**Cons:**
- ❌ Requires internet (unless self-hosted)
- ❌ More complex deployment
- ❌ Higher resource usage

**Best For:** Teams, cloud-based workflows, management dashboards

---

## 📊 Performance Considerations

### VSCode Extension
- **Startup Time:** <500ms (webview loading)
- **Analysis Time:** Same as CLI (Java process)
- **Memory:** +50MB (webview overhead)

### Web UI
- **First Load:** 1-2s (React bundle)
- **Subsequent Loads:** <200ms (cached)
- **Analysis Time:** +network latency
- **Concurrent Users:** 100+ (Spring Boot)

---

## 🔐 Security Considerations

### VSCode Extension
- ✅ Runs locally, no network required
- ✅ User controls file access
- ⚠️ Need to validate JAR integrity

### Web UI
- ⚠️ Need authentication (JWT)
- ⚠️ Need to protect API endpoints
- ⚠️ File upload size limits
- ⚠️ Code privacy (don't log sensitive code)
- ✅ HTTPS required
- ✅ CORS configuration

---

## 📝 Documentation Requirements

### For VSCode Extension
- [ ] Installation guide
- [ ] Feature walkthrough
- [ ] Keyboard shortcuts
- [ ] Troubleshooting

### For Web UI
- [ ] Deployment guide (Docker)
- [ ] API documentation (Swagger)
- [ ] User guide
- [ ] Admin guide

---

## 🚀 Rollout Strategy

### Phase 3.1 (v1.6.0) - VSCode Extension
**Target Date:** Q1 2026
**Beta Testing:** 2 weeks
**Release:** Marketplace + GitHub releases

### Phase 4.0 (v1.7.0) - Web UI
**Target Date:** Q2 2026
**Beta Testing:** 4 weeks (cloud hosted)
**Release:** Self-hosted option + SaaS offering

### Phase 4.1 (v1.8.0) - Advanced Features
**Target Date:** Q3 2026
**Iterative:** Add features based on user feedback

---

## ✅ Success Metrics

### VSCode Extension (v1.6)
- **Downloads:** 1000+ in first month
- **Active Users:** 500+ weekly
- **User Rating:** 4.5+ stars
- **Issues Applied:** 10,000+ refactorings

### Web UI (v1.7)
- **Registered Users:** 500+ in first quarter
- **Projects Analyzed:** 5,000+
- **API Uptime:** 99.9%
- **User Retention:** 60%+ monthly

---

## 💡 Key Takeaways

1. **Incremental Approach**: Start with VSCode (faster), add Web UI later
2. **Reuse Core**: Both UI types leverage same `pragmite-core` JAR
3. **User Choice**: Let users pick their preferred workflow (CLI, VSCode, Web)
4. **Data Consistency**: JSON output format bridges CLI ↔ UI
5. **Future-Proof**: Architecture allows adding more UI types (IntelliJ plugin, etc.)

---

## 📌 Next Steps

### Before Starting UI Development:
1. ✅ Complete Phase 3 (v1.5.0) - Auto-apply + Rollback
2. ✅ Add JSON output format to CLI
3. ✅ Document existing features (AUTO_APPLY_GUIDE.md)
4. ✅ Prepare v1.5.0 release

### Then Begin Phase 3.1:
1. Setup TypeScript project for VSCode extension
2. Implement PragmiteService (Java process wrapper)
3. Create AnalysisPanel webview
4. Integrate diff preview
5. Test end-to-end workflow

---

**Document Status:** ✅ COMPLETE
**Next Action:** Review with team, get approval, then begin Phase 3.1 implementation

---

**Last Updated:** December 26, 2025
**Author:** Pragmite Development Team
**Version:** 1.0
