import * as vscode from 'vscode';
import * as path from 'path';
import * as child_process from 'child_process';
import WebSocket from 'ws';

/**
 * Auto-Apply Panel for Pragmite v1.5.0
 * Provides UI for auto-applying AI refactorings with diff preview
 */
export class AutoApplyPanel {
    public static currentPanel: AutoApplyPanel | undefined;
    private readonly _panel: vscode.WebviewPanel;
    private readonly _extensionPath: string;
    private _disposables: vscode.Disposable[] = [];
    private _jarPath: string;
    private _javaPath: string;
    private _wsClient: WebSocket | null = null; // v1.6.2 - WebSocket client

    public static createOrShow(extensionPath: string, jarPath: string) {
        const column = vscode.ViewColumn.Two;

        // If we already have a panel, show it
        if (AutoApplyPanel.currentPanel) {
            AutoApplyPanel.currentPanel._panel.reveal(column);
            return;
        }

        // Otherwise, create a new panel
        const panel = vscode.window.createWebviewPanel(
            'pragmiteAutoApply',
            'Pragmite Auto-Apply',
            column,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [
                    vscode.Uri.file(path.join(extensionPath, 'out'))
                ]
            }
        );

        AutoApplyPanel.currentPanel = new AutoApplyPanel(panel, extensionPath, jarPath);
    }

    private constructor(panel: vscode.WebviewPanel, extensionPath: string, jarPath: string) {
        this._panel = panel;
        this._extensionPath = extensionPath;
        this._jarPath = jarPath;

        const config = vscode.workspace.getConfiguration('pragmite');
        this._javaPath = config.get('javaPath', 'java');

        // Set the webview's initial html content
        this._update();

        // Listen for when the panel is disposed
        this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

        // Handle messages from the webview
        this._panel.webview.onDidReceiveMessage(
            message => {
                switch (message.command) {
                    case 'runAutoApply':
                        this._runAutoApply(message.options);
                        return;
                    case 'listBackups':
                        this._listBackups(message.fileName);
                        return;
                    case 'rollback':
                        this._rollbackFile(message.fileName);
                        return;
                }
            },
            null,
            this._disposables
        );
    }

    /**
     * Run auto-apply with specified options
     */
    private async _runAutoApply(options: any) {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        if (!workspaceFolders) {
            vscode.window.showErrorMessage('No workspace folder open');
            return;
        }

        const workspacePath = workspaceFolders[0].uri.fsPath;

        // Notify webview that analysis is starting
        this._panel.webview.postMessage({
            command: 'analysisStarted'
        });

        // Build command
        const args = [
            '-jar',
            this._jarPath,
            workspacePath,
            '--ai-analysis',
            '--auto-apply'
        ];

        if (options.dryRun) {
            args.push('--dry-run');
        }

        if (!options.createBackup) {
            args.push('--no-backup');
        }

        if (options.interactive) {
            args.push('--interactive');
        }

        // Set API key from environment or config
        const apiKey = process.env.CLAUDE_API_KEY ||
                      vscode.workspace.getConfiguration('pragmite').get('claudeApiKey');

        if (apiKey) {
            process.env.CLAUDE_API_KEY = apiKey as string;
        }

        // Execute command
        const proc = child_process.spawn(this._javaPath, args, {
            cwd: path.dirname(this._jarPath),
            env: process.env
        });

        let output = '';

        proc.stdout.on('data', (data) => {
            output += data.toString();
            this._panel.webview.postMessage({
                command: 'analysisProgress',
                data: data.toString()
            });
        });

        proc.stderr.on('data', (data) => {
            this._panel.webview.postMessage({
                command: 'analysisError',
                data: data.toString()
            });
        });

        proc.on('close', (code) => {
            this._panel.webview.postMessage({
                command: 'analysisComplete',
                code: code,
                output: output
            });

            if (code === 0) {
                vscode.window.showInformationMessage('Auto-apply completed successfully!');
            } else {
                vscode.window.showErrorMessage(`Auto-apply failed with code ${code}`);
            }
        });
    }

    /**
     * List backups for a specific file
     */
    private async _listBackups(fileName: string) {
        const args = [
            '-jar',
            this._jarPath,
            '--list-backups-for',
            fileName
        ];

        const proc = child_process.spawn(this._javaPath, args, {
            cwd: path.dirname(this._jarPath)
        });

        let output = '';

        proc.stdout.on('data', (data) => {
            output += data.toString();
        });

        proc.on('close', (code) => {
            if (code === 0) {
                this._panel.webview.postMessage({
                    command: 'backupsListed',
                    data: output
                });
            }
        });
    }

    /**
     * Rollback a file to latest backup
     */
    private async _rollbackFile(fileName: string) {
        const result = await vscode.window.showWarningMessage(
            `Rollback ${fileName} to latest backup?`,
            'Yes',
            'No'
        );

        if (result !== 'Yes') {
            return;
        }

        const args = [
            '-jar',
            this._jarPath,
            '--rollback-file-backup',
            fileName
        ];

        const proc = child_process.spawn(this._javaPath, args, {
            cwd: path.dirname(this._jarPath)
        });

        let output = '';

        proc.stdout.on('data', (data) => {
            output += data.toString();
        });

        proc.on('close', (code) => {
            if (code === 0) {
                vscode.window.showInformationMessage(`Successfully rolled back ${fileName}`);
                this._panel.webview.postMessage({
                    command: 'rollbackComplete',
                    fileName: fileName
                });
            } else {
                vscode.window.showErrorMessage(`Rollback failed: ${output}`);
            }
        });
    }

    /**
     * Update webview content
     */
    private _update() {
        this._panel.webview.html = this._getHtmlForWebview();
    }

    /**
     * Generate HTML for the webview
     */
    private _getHtmlForWebview() {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pragmite Auto-Apply</title>
    <style>
        body {
            font-family: var(--vscode-font-family);
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            padding: 20px;
            line-height: 1.6;
        }

        h1 {
            color: var(--vscode-textLink-activeForeground);
            border-bottom: 2px solid var(--vscode-panel-border);
            padding-bottom: 10px;
        }

        .section {
            margin: 20px 0;
            padding: 15px;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border-radius: 5px;
        }

        .controls {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin: 15px 0;
        }

        button {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            padding: 10px 20px;
            cursor: pointer;
            border-radius: 3px;
            font-size: 14px;
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

        .checkbox-group {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin: 15px 0;
        }

        .checkbox-item {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        input[type="checkbox"] {
            width: 18px;
            height: 18px;
            cursor: pointer;
        }

        .output {
            background-color: var(--vscode-terminal-background);
            color: var(--vscode-terminal-foreground);
            padding: 15px;
            border-radius: 5px;
            font-family: var(--vscode-editor-font-family);
            font-size: 13px;
            white-space: pre-wrap;
            max-height: 400px;
            overflow-y: auto;
            margin-top: 15px;
            display: none;
        }

        .output.visible {
            display: block;
        }

        .feature-list {
            list-style: none;
            padding: 0;
        }

        .feature-list li {
            padding: 8px 0;
            padding-left: 25px;
            position: relative;
        }

        .feature-list li:before {
            content: "✓";
            position: absolute;
            left: 0;
            color: var(--vscode-terminal-ansiGreen);
            font-weight: bold;
        }

        .status {
            padding: 10px;
            margin: 10px 0;
            border-radius: 5px;
            display: none;
        }

        .status.info {
            background-color: var(--vscode-inputValidation-infoBackground);
            border: 1px solid var(--vscode-inputValidation-infoBorder);
            display: block;
        }

        .status.success {
            background-color: var(--vscode-testing-iconPassed);
            color: white;
            display: block;
        }

        .status.error {
            background-color: var(--vscode-inputValidation-errorBackground);
            border: 1px solid var(--vscode-inputValidation-errorBorder);
            display: block;
        }

        .spinner {
            border: 3px solid var(--vscode-panel-border);
            border-top: 3px solid var(--vscode-progressBar-background);
            border-radius: 50%;
            width: 30px;
            height: 30px;
            animation: spin 1s linear infinite;
            display: inline-block;
            vertical-align: middle;
            margin-right: 10px;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <h1>🤖 Pragmite Auto-Apply (v1.5.0)</h1>

    <div class="section">
        <h2>Features</h2>
        <ul class="feature-list">
            <li>Automatic code application with safety backups</li>
            <li>JavaParser validation before applying changes</li>
            <li>Dry-run mode for preview</li>
            <li>File-based rollback system</li>
            <li>MD5 checksum verification</li>
            <li>Automatic cleanup (keeps last 10 backups)</li>
        </ul>
    </div>

    <div class="section">
        <h2>Options</h2>
        <div class="checkbox-group">
            <div class="checkbox-item">
                <input type="checkbox" id="dryRun">
                <label for="dryRun">Dry-run (preview only, don't apply changes)</label>
            </div>
            <div class="checkbox-item">
                <input type="checkbox" id="createBackup" checked>
                <label for="createBackup">Create backups before applying</label>
            </div>
            <div class="checkbox-item">
                <input type="checkbox" id="interactive">
                <label for="interactive">Interactive mode (ask confirmation for each change) 🆕 v1.6.0</label>
            </div>
        </div>
    </div>

    <div class="section">
        <h2>Actions</h2>
        <div class="controls">
            <button id="runAutoApply">▶️ Run Auto-Apply</button>
            <button id="listBackups" class="secondary">📦 List All Backups</button>
            <button id="showHelp" class="secondary">❓ Help</button>
        </div>
    </div>

    <div id="status" class="status"></div>
    <div id="output" class="output"></div>

    <script>
        const vscode = acquireVsCodeApi();

        // Button handlers
        document.getElementById('runAutoApply').addEventListener('click', () => {
            const dryRun = document.getElementById('dryRun').checked;
            const createBackup = document.getElementById('createBackup').checked;
            const interactive = document.getElementById('interactive').checked;

            vscode.postMessage({
                command: 'runAutoApply',
                options: {
                    dryRun: dryRun,
                    createBackup: createBackup,
                    interactive: interactive
                }
            });
        });

        document.getElementById('listBackups').addEventListener('click', () => {
            vscode.postMessage({
                command: 'listBackups',
                fileName: '*'
            });
        });

        document.getElementById('showHelp').addEventListener('click', () => {
            const helpText = \`
Pragmite Auto-Apply v1.5.0 - Help

USAGE:
1. Click "Run Auto-Apply" to analyze and automatically apply AI refactorings
2. Use "Dry-run" to preview changes without modifying files
3. Backups are created automatically (unless disabled)
4. Use "List All Backups" to see all backup files
5. Rollback is available if something goes wrong

SAFETY FEATURES:
- Automatic backups with timestamps
- JavaParser validation before applying
- MD5 checksum verification
- Safety backup before rollback
- Automatic cleanup (keeps last 10 backups)

For more information, see:
docs/AUTO_APPLY_GUIDE.md
            \`;

            showStatus('info', helpText);
        });

        // Handle messages from extension
        window.addEventListener('message', event => {
            const message = event.data;

            switch (message.command) {
                case 'analysisStarted':
                    showStatus('info', '<div class="spinner"></div> Analysis started...');
                    showOutput('');
                    break;

                case 'analysisProgress':
                    appendOutput(message.data);
                    break;

                case 'analysisError':
                    appendOutput('[ERROR] ' + message.data);
                    break;

                case 'analysisComplete':
                    if (message.code === 0) {
                        showStatus('success', '✅ Auto-apply completed successfully!');
                    } else {
                        showStatus('error', '❌ Auto-apply failed with code ' + message.code);
                    }
                    break;

                case 'backupsListed':
                    showOutput(message.data);
                    break;

                case 'rollbackComplete':
                    showStatus('success', '✅ Rollback completed: ' + message.fileName);
                    break;
            }
        });

        function showStatus(type, message) {
            const statusDiv = document.getElementById('status');
            statusDiv.className = 'status ' + type;
            statusDiv.innerHTML = message;
        }

        function showOutput(text) {
            const outputDiv = document.getElementById('output');
            outputDiv.textContent = text;
            outputDiv.classList.add('visible');
        }

        function appendOutput(text) {
            const outputDiv = document.getElementById('output');
            outputDiv.textContent += text;
            outputDiv.classList.add('visible');
            outputDiv.scrollTop = outputDiv.scrollHeight;
        }
    </script>
</body>
</html>`;
    }

    public dispose() {
        AutoApplyPanel.currentPanel = undefined;

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
