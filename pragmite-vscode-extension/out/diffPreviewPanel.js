"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.DiffPreviewPanel = void 0;
const vscode = __importStar(require("vscode"));
const path = __importStar(require("path"));
const Diff = __importStar(require("diff"));
/**
 * Diff Preview Panel using Monaco Editor v1.6.3
 * Shows side-by-side comparison of before/after code with Monaco Editor integration
 */
class DiffPreviewPanel {
    static createOrShow(extensionPath, fileName, beforeCode, afterCode, refactoringType) {
        const column = vscode.ViewColumn.Beside;
        // If we already have a panel, show it
        if (DiffPreviewPanel.currentPanel) {
            DiffPreviewPanel.currentPanel._panel.reveal(column);
            DiffPreviewPanel.currentPanel.updateContent(fileName, beforeCode, afterCode, refactoringType);
            return;
        }
        // Otherwise, create a new panel
        const panel = vscode.window.createWebviewPanel('pragmiteDiffPreview', 'Pragmite Diff Preview', column, {
            enableScripts: true,
            retainContextWhenHidden: true,
            localResourceRoots: [
                vscode.Uri.file(path.join(extensionPath, 'node_modules', 'monaco-editor'))
            ]
        });
        DiffPreviewPanel.currentPanel = new DiffPreviewPanel(panel, extensionPath, fileName, beforeCode, afterCode, refactoringType);
    }
    constructor(panel, extensionPath, fileName, beforeCode, afterCode, refactoringType) {
        this._disposables = [];
        this._panel = panel;
        this._extensionPath = extensionPath;
        // Set the webview's initial html content
        this.updateContent(fileName, beforeCode, afterCode, refactoringType);
        // Listen for when the panel is disposed
        this._panel.onDidDispose(() => this.dispose(), null, this._disposables);
        // Handle messages from the webview
        this._panel.webview.onDidReceiveMessage(message => {
            switch (message.command) {
                case 'copyDiff':
                    this._copyDiffToClipboard(message.diff);
                    return;
                case 'acceptChanges':
                    this._acceptChanges(message.afterCode);
                    return;
                case 'rejectChanges':
                    this._rejectChanges();
                    return;
            }
        }, null, this._disposables);
    }
    async _acceptChanges(afterCode) {
        vscode.window.showInformationMessage('✅ Changes accepted! Apply to file?', 'Apply', 'Cancel')
            .then(selection => {
            if (selection === 'Apply') {
                // This will be implemented when integrating with AutoApplyPanel
                vscode.window.showInformationMessage('✅ Changes accepted and ready to apply');
            }
        });
    }
    async _rejectChanges() {
        vscode.window.showInformationMessage('❌ Changes rejected');
        this._panel.dispose();
    }
    updateContent(fileName, beforeCode, afterCode, refactoringType) {
        // Generate diff
        const diffResult = Diff.createTwoFilesPatch('before.java', 'after.java', beforeCode, afterCode, 'Before', 'After');
        // Calculate statistics
        const lines = diffResult.split('\n');
        const additions = lines.filter(line => line.startsWith('+')).length;
        const deletions = lines.filter(line => line.startsWith('-')).length;
        this._panel.title = `Diff: ${fileName}`;
        this._panel.webview.html = this._getHtmlForWebview(fileName, beforeCode, afterCode, diffResult, refactoringType, additions, deletions);
    }
    _copyDiffToClipboard(diff) {
        vscode.env.clipboard.writeText(diff);
        vscode.window.showInformationMessage('Diff copied to clipboard!');
    }
    _getHtmlForWebview(fileName, beforeCode, afterCode, unifiedDiff, refactoringType, additions, deletions) {
        // Get Monaco Editor resources
        const monacoBase = this._panel.webview.asWebviewUri(vscode.Uri.file(path.join(this._extensionPath, 'node_modules', 'monaco-editor', 'min')));
        // Escape for JavaScript embedding
        const escapeJs = (text) => {
            return text
                .replace(/\\/g, '\\\\')
                .replace(/`/g, '\\`')
                .replace(/\$/g, '\\$');
        };
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Diff Preview - Monaco Editor</title>
    <link rel="stylesheet" href="${monacoBase}/vs/editor/editor.main.css">
    <style>
        body {
            font-family: var(--vscode-font-family);
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            margin: 0;
            padding: 0;
            overflow: hidden;
        }

        .header {
            background-color: var(--vscode-editorGroupHeader-tabsBackground);
            padding: 12px 20px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }

        .header h1 {
            margin: 0 0 8px 0;
            font-size: 16px;
            font-weight: 600;
            color: var(--vscode-textLink-activeForeground);
        }

        .header-info {
            display: flex;
            gap: 20px;
            align-items: center;
            font-size: 12px;
            color: var(--vscode-descriptionForeground);
        }

        .stats {
            display: flex;
            gap: 12px;
        }

        .stat {
            padding: 4px 10px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 500;
        }

        .stat.additions {
            background-color: rgba(0, 255, 0, 0.12);
            color: #4ec9b0;
        }

        .stat.deletions {
            background-color: rgba(255, 0, 0, 0.12);
            color: #f48771;
        }

        #monaco-container {
            height: calc(100vh - 130px);
            width: 100%;
        }

        .actions {
            padding: 12px 20px;
            background-color: var(--vscode-editorGroupHeader-tabsBackground);
            border-top: 1px solid var(--vscode-panel-border);
            display: flex;
            gap: 10px;
        }

        button {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            padding: 6px 14px;
            cursor: pointer;
            border-radius: 3px;
            font-size: 12px;
            font-weight: 500;
        }

        button:hover {
            background-color: var(--vscode-button-hoverBackground);
        }

        button.secondary {
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }

        button.secondary:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📝 ${fileName}</h1>
        <div class="header-info">
            <span><strong>Refactoring:</strong> ${refactoringType}</span>
            <div class="stats">
                <span class="stat additions">+${additions} additions</span>
                <span class="stat deletions">-${deletions} deletions</span>
            </div>
        </div>
    </div>

    <div id="monaco-container"></div>

    <div class="actions">
        <button id="copyDiff" class="secondary">📋 Copy Diff</button>
        <button id="acceptChanges">✅ Accept Changes</button>
        <button id="rejectChanges" class="secondary">❌ Reject Changes</button>
    </div>

    <script src="${monacoBase}/vs/loader.js"></script>
    <script>
        const vscode = acquireVsCodeApi();

        require.config({ paths: { vs: '${monacoBase}/vs' } });

        require(['vs/editor/editor.main'], function() {
            const beforeCode = \`${escapeJs(beforeCode)}\`;
            const afterCode = \`${escapeJs(afterCode)}\`;

            // Create Monaco diff editor
            const diffEditor = monaco.editor.createDiffEditor(
                document.getElementById('monaco-container'),
                {
                    enableSplitViewResizing: true,
                    renderSideBySide: true,
                    readOnly: true,
                    automaticLayout: true,
                    fontSize: 13,
                    minimap: { enabled: true },
                    scrollBeyondLastLine: false,
                    renderWhitespace: 'selection',
                    diffWordWrap: 'on',
                    theme: document.body.classList.contains('vscode-dark') ? 'vs-dark' : 'vs'
                }
            );

            // Set diff models
            const originalModel = monaco.editor.createModel(beforeCode, 'java');
            const modifiedModel = monaco.editor.createModel(afterCode, 'java');

            diffEditor.setModel({
                original: originalModel,
                modified: modifiedModel
            });

            // Button handlers
            document.getElementById('copyDiff').addEventListener('click', () => {
                vscode.postMessage({
                    command: 'copyDiff',
                    diff: \`--- Before\\n+++ After\\n\` + afterCode
                });
            });

            document.getElementById('acceptChanges').addEventListener('click', () => {
                vscode.postMessage({
                    command: 'acceptChanges',
                    afterCode: afterCode
                });
            });

            document.getElementById('rejectChanges').addEventListener('click', () => {
                vscode.postMessage({
                    command: 'rejectChanges'
                });
            });

            // Cleanup on dispose
            window.addEventListener('beforeunload', () => {
                diffEditor.dispose();
                originalModel.dispose();
                modifiedModel.dispose();
            });
        });
    </script>
</body>
</html>`;
    }
    dispose() {
        DiffPreviewPanel.currentPanel = undefined;
        // Clean up our resources
        this._panel.dispose();
        while (this._disposables.length) {
            const x = this._disposables.pop();
            if (x) {
                x.dispose();
            }
        }
    }
}
exports.DiffPreviewPanel = DiffPreviewPanel;
//# sourceMappingURL=diffPreviewPanel.js.map