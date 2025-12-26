import * as vscode from 'vscode';
import * as Diff from 'diff';

/**
 * Diff Preview Panel using Monaco Editor
 * Shows side-by-side comparison of before/after code
 */
export class DiffPreviewPanel {
    public static currentPanel: DiffPreviewPanel | undefined;
    private readonly _panel: vscode.WebviewPanel;
    private _disposables: vscode.Disposable[] = [];

    public static createOrShow(
        extensionPath: string,
        fileName: string,
        beforeCode: string,
        afterCode: string,
        refactoringType: string
    ) {
        const column = vscode.ViewColumn.Beside;

        // If we already have a panel, show it
        if (DiffPreviewPanel.currentPanel) {
            DiffPreviewPanel.currentPanel._panel.reveal(column);
            DiffPreviewPanel.currentPanel.updateContent(fileName, beforeCode, afterCode, refactoringType);
            return;
        }

        // Otherwise, create a new panel
        const panel = vscode.window.createWebviewPanel(
            'pragmiteDiffPreview',
            'Pragmite Diff Preview',
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );

        DiffPreviewPanel.currentPanel = new DiffPreviewPanel(
            panel,
            fileName,
            beforeCode,
            afterCode,
            refactoringType
        );
    }

    private constructor(
        panel: vscode.WebviewPanel,
        fileName: string,
        beforeCode: string,
        afterCode: string,
        refactoringType: string
    ) {
        this._panel = panel;

        // Set the webview's initial html content
        this.updateContent(fileName, beforeCode, afterCode, refactoringType);

        // Listen for when the panel is disposed
        this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

        // Handle messages from the webview
        this._panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'copyDiff':
                        this._copyDiffToClipboard(message.diff);
                        return;
                }
            },
            null,
            this._disposables
        );
    }

    private updateContent(
        fileName: string,
        beforeCode: string,
        afterCode: string,
        refactoringType: string
    ) {
        // Generate diff
        const diffResult = Diff.createTwoFilesPatch(
            'before.java',
            'after.java',
            beforeCode,
            afterCode,
            'Before',
            'After'
        );

        // Calculate statistics
        const lines = diffResult.split('\n');
        const additions = lines.filter(line => line.startsWith('+')).length;
        const deletions = lines.filter(line => line.startsWith('-')).length;

        this._panel.title = `Diff: ${fileName}`;
        this._panel.webview.html = this._getHtmlForWebview(
            fileName,
            beforeCode,
            afterCode,
            diffResult,
            refactoringType,
            additions,
            deletions
        );
    }

    private _copyDiffToClipboard(diff: string) {
        vscode.env.clipboard.writeText(diff);
        vscode.window.showInformationMessage('Diff copied to clipboard!');
    }

    private _getHtmlForWebview(
        fileName: string,
        beforeCode: string,
        afterCode: string,
        unifiedDiff: string,
        refactoringType: string,
        additions: number,
        deletions: number
    ): string {
        // Escape HTML special characters
        const escapeHtml = (text: string) => {
            return text
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        };

        const beforeLines = beforeCode.split('\n');
        const afterLines = afterCode.split('\n');

        // Generate line-by-line comparison
        const diff = Diff.diffLines(beforeCode, afterCode);

        let beforeHtml = '';
        let afterHtml = '';
        let beforeLineNum = 1;
        let afterLineNum = 1;

        diff.forEach(part => {
            const lines = part.value.split('\n').filter(line => line.length > 0 || part.value.endsWith('\n'));

            if (part.added) {
                // Added lines (only in after)
                lines.forEach(line => {
                    afterHtml += `<div class="line added"><span class="line-num">${afterLineNum++}</span><span class="line-content">+ ${escapeHtml(line)}</span></div>`;
                });
            } else if (part.removed) {
                // Removed lines (only in before)
                lines.forEach(line => {
                    beforeHtml += `<div class="line removed"><span class="line-num">${beforeLineNum++}</span><span class="line-content">- ${escapeHtml(line)}</span></div>`;
                });
            } else {
                // Unchanged lines (in both)
                lines.forEach(line => {
                    beforeHtml += `<div class="line"><span class="line-num">${beforeLineNum++}</span><span class="line-content">  ${escapeHtml(line)}</span></div>`;
                    afterHtml += `<div class="line"><span class="line-num">${afterLineNum++}</span><span class="line-content">  ${escapeHtml(line)}</span></div>`;
                });
            }
        });

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Diff Preview</title>
    <style>
        body {
            font-family: var(--vscode-editor-font-family);
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            margin: 0;
            padding: 0;
            overflow: hidden;
        }

        .header {
            background-color: var(--vscode-editorGroupHeader-tabsBackground);
            padding: 15px 20px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }

        .header h1 {
            margin: 0 0 10px 0;
            font-size: 18px;
            color: var(--vscode-textLink-activeForeground);
        }

        .header-info {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: var(--vscode-descriptionForeground);
        }

        .stats {
            display: flex;
            gap: 15px;
            margin-top: 10px;
        }

        .stat {
            padding: 5px 10px;
            border-radius: 3px;
            font-size: 12px;
        }

        .stat.additions {
            background-color: rgba(0, 255, 0, 0.1);
            color: var(--vscode-terminal-ansiGreen);
        }

        .stat.deletions {
            background-color: rgba(255, 0, 0, 0.1);
            color: var(--vscode-terminal-ansiRed);
        }

        .diff-container {
            display: flex;
            height: calc(100vh - 150px);
        }

        .diff-pane {
            flex: 1;
            overflow-y: auto;
            border-right: 1px solid var(--vscode-panel-border);
            background-color: var(--vscode-editor-background);
        }

        .diff-pane:last-child {
            border-right: none;
        }

        .diff-pane-header {
            position: sticky;
            top: 0;
            background-color: var(--vscode-editorGroupHeader-tabsBackground);
            padding: 8px 12px;
            border-bottom: 1px solid var(--vscode-panel-border);
            font-weight: bold;
            z-index: 10;
        }

        .line {
            display: flex;
            font-family: var(--vscode-editor-font-family);
            font-size: var(--vscode-editor-font-size, 14px);
            line-height: 1.6;
            white-space: pre;
        }

        .line-num {
            display: inline-block;
            width: 50px;
            padding: 0 10px;
            text-align: right;
            color: var(--vscode-editorLineNumber-foreground);
            background-color: var(--vscode-editorGutter-background);
            user-select: none;
            flex-shrink: 0;
        }

        .line-content {
            padding: 0 10px;
            flex: 1;
            overflow-x: auto;
        }

        .line.added {
            background-color: rgba(0, 255, 0, 0.15);
        }

        .line.added .line-content {
            color: var(--vscode-terminal-ansiGreen);
        }

        .line.removed {
            background-color: rgba(255, 0, 0, 0.15);
        }

        .line.removed .line-content {
            color: var(--vscode-terminal-ansiRed);
        }

        .line:hover {
            background-color: var(--vscode-list-hoverBackground);
        }

        .actions {
            padding: 15px 20px;
            background-color: var(--vscode-editorGroupHeader-tabsBackground);
            border-top: 1px solid var(--vscode-panel-border);
            display: flex;
            gap: 10px;
        }

        button {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            padding: 8px 16px;
            cursor: pointer;
            border-radius: 3px;
            font-size: 13px;
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

        .unified-diff {
            display: none;
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background-color: var(--vscode-editor-background);
            border: 1px solid var(--vscode-panel-border);
            padding: 20px;
            max-width: 80%;
            max-height: 80%;
            overflow: auto;
            z-index: 1000;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
        }

        .unified-diff.visible {
            display: block;
        }

        .overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            z-index: 999;
        }

        .overlay.visible {
            display: block;
        }

        .unified-diff pre {
            margin: 0;
            font-family: var(--vscode-editor-font-family);
            font-size: 13px;
            white-space: pre-wrap;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📝 ${escapeHtml(fileName)}</h1>
        <div class="header-info">
            <span><strong>Refactoring:</strong> ${escapeHtml(refactoringType)}</span>
        </div>
        <div class="stats">
            <span class="stat additions">+${additions} additions</span>
            <span class="stat deletions">-${deletions} deletions</span>
        </div>
    </div>

    <div class="diff-container">
        <div class="diff-pane">
            <div class="diff-pane-header">BEFORE</div>
            <div class="code-lines">
                ${beforeHtml}
            </div>
        </div>
        <div class="diff-pane">
            <div class="diff-pane-header">AFTER</div>
            <div class="code-lines">
                ${afterHtml}
            </div>
        </div>
    </div>

    <div class="actions">
        <button id="copyDiff" class="secondary">📋 Copy Unified Diff</button>
        <button id="showUnified" class="secondary">📄 Show Unified Diff</button>
    </div>

    <div class="overlay" id="overlay"></div>
    <div class="unified-diff" id="unifiedDiff">
        <h3>Unified Diff Format</h3>
        <pre>${escapeHtml(unifiedDiff)}</pre>
        <button onclick="closeUnified()">Close</button>
    </div>

    <script>
        const vscode = acquireVsCodeApi();

        document.getElementById('copyDiff').addEventListener('click', () => {
            const diff = \`${unifiedDiff.replace(/`/g, '\\`')}\`;
            vscode.postMessage({
                command: 'copyDiff',
                diff: diff
            });
        });

        document.getElementById('showUnified').addEventListener('click', () => {
            document.getElementById('overlay').classList.add('visible');
            document.getElementById('unifiedDiff').classList.add('visible');
        });

        document.getElementById('overlay').addEventListener('click', closeUnified);

        function closeUnified() {
            document.getElementById('overlay').classList.remove('visible');
            document.getElementById('unifiedDiff').classList.remove('visible');
        }

        // Sync scrolling between panes
        const panes = document.querySelectorAll('.diff-pane');
        let isScrolling = false;

        panes.forEach(pane => {
            pane.addEventListener('scroll', function() {
                if (isScrolling) return;
                isScrolling = true;

                panes.forEach(otherPane => {
                    if (otherPane !== pane) {
                        otherPane.scrollTop = pane.scrollTop;
                    }
                });

                setTimeout(() => {
                    isScrolling = false;
                }, 50);
            });
        });
    </script>
</body>
</html>`;
    }

    public dispose() {
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
