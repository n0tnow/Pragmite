import * as vscode from 'vscode';
import { PragmiteService } from './pragmiteService';
import { DiagnosticProvider } from './diagnosticProvider';
import { PragmiteTreeProvider } from './treeViewProvider';
import { PragmiteCodeLensProvider } from './codeLensProvider';
import { PragmiteQuickFixProvider } from './quickFixProvider';
import { PragmiteDecorationProvider } from './decorationProvider';
import { AnalysisResult } from './models';
import { generateReportHtml } from './reportGenerator';
import { PragmiteWebServer } from './webServer';

let pragmiteService: PragmiteService;
let diagnosticProvider: DiagnosticProvider;
let treeViewProvider: PragmiteTreeProvider;
let codeLensProvider: PragmiteCodeLensProvider;
let quickFixProvider: PragmiteQuickFixProvider;
let decorationProvider: PragmiteDecorationProvider;
let statusBarItem: vscode.StatusBarItem;
let webServer: PragmiteWebServer;

export function activate(context: vscode.ExtensionContext) {
    console.log('Pragmite extension is now active!');

    // Initialize services
    pragmiteService = new PragmiteService(context);
    diagnosticProvider = new DiagnosticProvider();
    treeViewProvider = new PragmiteTreeProvider();
    codeLensProvider = new PragmiteCodeLensProvider();
    quickFixProvider = new PragmiteQuickFixProvider();
    decorationProvider = new PragmiteDecorationProvider();

    // Initialize Web Server
    const outputChannel = vscode.window.createOutputChannel('Pragmite Dashboard');
    webServer = new PragmiteWebServer(outputChannel);
    webServer.start().then(port => {
        vscode.window.showInformationMessage(
            `🌐 Pragmite Dashboard is live at http://localhost:${port}`,
            'Open Dashboard'
        ).then(selection => {
            if (selection === 'Open Dashboard') {
                vscode.env.openExternal(vscode.Uri.parse(webServer.getUrl()));
            }
        });
    }).catch(error => {
        console.error('Failed to start web server:', error);
    });

    // Register Tree View
    const treeView = vscode.window.createTreeView('pragmiteResults', {
        treeDataProvider: treeViewProvider,
        showCollapseAll: true
    });
    context.subscriptions.push(treeView);

    // Register Code Lens Provider
    context.subscriptions.push(
        vscode.languages.registerCodeLensProvider({ language: 'java' }, codeLensProvider)
    );

    // Register Quick Fix Provider
    context.subscriptions.push(
        vscode.languages.registerCodeActionsProvider({ language: 'java' }, quickFixProvider, {
            providedCodeActionKinds: [
                vscode.CodeActionKind.QuickFix,
                vscode.CodeActionKind.RefactorExtract,
                vscode.CodeActionKind.RefactorRewrite
            ]
        })
    );

    // Create status bar item
    statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    statusBarItem.text = '$(microscope) Pragmite';
    statusBarItem.tooltip = 'Click to analyze workspace';
    statusBarItem.command = 'pragmite.analyzeWorkspace';
    statusBarItem.show();
    context.subscriptions.push(statusBarItem);

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.analyzeFile', async () => {
            await analyzeCurrentFile();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.analyzeWorkspace', async () => {
            await analyzeWorkspace();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.showReport', async () => {
            await showQualityReport();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.openDashboard', () => {
            vscode.env.openExternal(vscode.Uri.parse(webServer.getUrl()));
        })
    );

    // Register new commands
    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.showRefactoringSuggestion', (data: any) => {
            vscode.window.showInformationMessage(data.message, 'OK', 'Learn More').then(selection => {
                if (selection === 'Learn More') {
                    vscode.env.openExternal(vscode.Uri.parse('https://github.com/pragmite/pragmite/wiki'));
                }
            });
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.showSmellDetails', (smells: any[]) => {
            const message = smells.map((s, i) => `${i + 1}. ${s.message}`).join('\n');
            vscode.window.showWarningMessage(`Code Smells Detected:\n${message}`);
        })
    );

    // Register auto-fix command
    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.applyAutoFix', async (suggestion: any) => {
            await applyAutoFix(suggestion);
        })
    );

    // Register bulk auto-fix command
    context.subscriptions.push(
        vscode.commands.registerCommand('pragmite.applyAllAutoFixes', async (suggestions: any[]) => {
            await applyAllAutoFixes(suggestions);
        })
    );

    // Auto-analyze on save if enabled
    context.subscriptions.push(
        vscode.workspace.onDidSaveTextDocument(async (document) => {
            const config = vscode.workspace.getConfiguration('pragmite');
            const enabled = config.get('enabled', true);
            const analyzeOnSave = config.get('analyzeOnSave', true);

            if (enabled && analyzeOnSave && document.languageId === 'java') {
                await analyzeDocument(document);
            }
        })
    );

    // Analyze open Java files
    context.subscriptions.push(
        vscode.workspace.onDidOpenTextDocument(async (document) => {
            const config = vscode.workspace.getConfiguration('pragmite');
            const enabled = config.get('enabled', true);

            if (enabled && document.languageId === 'java') {
                await analyzeDocument(document);
            }
        })
    );

    // Push disposables
    context.subscriptions.push(pragmiteService);
    context.subscriptions.push(diagnosticProvider);

    // Analyze currently open Java files on activation
    vscode.workspace.textDocuments.forEach(async (document) => {
        const config = vscode.workspace.getConfiguration('pragmite');
        const enabled = config.get('enabled', true);

        if (enabled && document.languageId === 'java') {
            await analyzeDocument(document);
        }
    });

    vscode.window.showInformationMessage('Pragmite is ready to analyze your Java code!');
}

/**
 * Analyze current active file
 */
async function analyzeCurrentFile() {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showWarningMessage('No active editor found');
        return;
    }

    if (editor.document.languageId !== 'java') {
        vscode.window.showWarningMessage('Current file is not a Java file');
        return;
    }

    await analyzeDocument(editor.document);
}

/**
 * Analyze a document
 */
async function analyzeDocument(document: vscode.TextDocument) {
    if (!document.uri.fsPath) {
        return;
    }

    statusBarItem.text = '$(sync~spin) Analyzing...';

    try {
        const result = await pragmiteService.analyzeFile(document.uri.fsPath);
        const fileAnalysis = result.fileAnalysis;
        const fullResult = result.fullResult;

        // Update all providers
        diagnosticProvider.updateDiagnostics(document.uri, fileAnalysis);
        codeLensProvider.updateFileAnalysis(document.uri, fileAnalysis);

        if (fileAnalysis) {
            quickFixProvider.updateSmells(document.uri, fileAnalysis.smells);

            // Update decorations for active editor
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.uri.toString() === document.uri.toString()) {
                decorationProvider.updateDecorations(editor, fileAnalysis);
            }

            const smellCount = fileAnalysis.smells.length;
            const complexityCount = fileAnalysis.complexities.filter(c =>
                ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL'].includes(c.complexity)
            ).length;

            statusBarItem.text = `$(microscope) ${smellCount} issues, ${complexityCount} high complexity`;
        } else {
            statusBarItem.text = '$(microscope) Pragmite';
        }

        // Update web dashboard with full result
        if (fullResult) {
            webServer.updateAnalysis(fullResult);
        }
    } catch (error) {
        console.error('Analysis error:', error);
        statusBarItem.text = '$(microscope) Analysis failed';
        vscode.window.showErrorMessage(`Pragmite analysis failed: ${error}`);
    }
}

/**
 * Analyze entire workspace
 */
async function analyzeWorkspace() {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders || workspaceFolders.length === 0) {
        vscode.window.showWarningMessage('No workspace folder open');
        return;
    }

    const workspacePath = workspaceFolders[0].uri.fsPath;

    await vscode.window.withProgress({
        location: vscode.ProgressLocation.Notification,
        title: 'Analyzing workspace with Pragmite',
        cancellable: false
    }, async (progress) => {
        progress.report({ increment: 0, message: 'Starting analysis...' });

        try {
            const result = await pragmiteService.analyzeWorkspace(workspacePath);

            if (result) {
                progress.report({ increment: 100, message: 'Analysis complete!' });

                // Update diagnostics for all files
                result.fileAnalyses.forEach(fileAnalysis => {
                    const uri = vscode.Uri.file(fileAnalysis.filePath);
                    diagnosticProvider.updateDiagnostics(uri, fileAnalysis);
                    codeLensProvider.updateFileAnalysis(uri, fileAnalysis);
                    quickFixProvider.updateSmells(uri, fileAnalysis.smells);
                });

                // Update tree view
                treeViewProvider.updateAnalysisResult(result);

                // Update web server
                webServer.updateAnalysis(result);

                // Show summary
                const totalSmells = result.codeSmells.length;
                const score = result.qualityScore.overallScore;
                const grade = result.qualityScore.grade;

                vscode.window.showInformationMessage(
                    `Analysis complete! Score: ${score}/100 (${grade}), Found ${totalSmells} code smells in ${result.totalFiles} files`,
                    'View Dashboard'
                ).then(selection => {
                    if (selection === 'View Dashboard') {
                        vscode.env.openExternal(vscode.Uri.parse(webServer.getUrl()));
                    }
                });

                // Store result for report
                (global as any).lastAnalysisResult = result;
            } else {
                vscode.window.showErrorMessage('Analysis failed');
            }
        } catch (error) {
            console.error('Workspace analysis error:', error);
            vscode.window.showErrorMessage(`Workspace analysis failed: ${error}`);
        }
    });
}

/**
 * Show quality report in webview
 */
async function showQualityReport() {
    const result: AnalysisResult | undefined = (global as any).lastAnalysisResult;

    if (!result) {
        vscode.window.showWarningMessage('No analysis results available. Run workspace analysis first.');
        return;
    }

    const panel = vscode.window.createWebviewPanel(
        'pragmiteReport',
        'Pragmite Quality Report',
        vscode.ViewColumn.One,
        {}
    );

    panel.webview.html = generateReportHtml(result);
}

// HTML report generator moved to reportGenerator.ts

/**
 * Apply auto-fix for a single suggestion
 */
async function applyAutoFix(suggestion: any) {
    try {
        if (!suggestion || !suggestion.filePath) {
            vscode.window.showErrorMessage('Invalid suggestion data');
            return;
        }

        const uri = vscode.Uri.file(suggestion.filePath);
        const document = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(document);

        // Apply the fix based on suggestion type
        const edit = new vscode.WorkspaceEdit();

        if (suggestion.afterCode && suggestion.startLine && suggestion.endLine) {
            // Replace code block
            const startPos = new vscode.Position(suggestion.startLine - 1, 0);
            const endPos = new vscode.Position(suggestion.endLine, 0);
            const range = new vscode.Range(startPos, endPos);

            edit.replace(uri, range, suggestion.afterCode + '\n');

            await vscode.workspace.applyEdit(edit);
            await document.save();

            vscode.window.showInformationMessage(`✅ Auto-fix applied: ${suggestion.title}`);
        } else {
            // If no code provided, show suggestion for manual fix
            vscode.window.showInformationMessage(
                `Manual refactoring required: ${suggestion.title}`,
                'View Details'
            ).then(selection => {
                if (selection === 'View Details') {
                    // Navigate to the location
                    const startLine = suggestion.startLine ? suggestion.startLine - 1 : 0;
                    const position = new vscode.Position(startLine, 0);
                    editor.selection = new vscode.Selection(position, position);
                    editor.revealRange(new vscode.Range(position, position));
                }
            });
        }
    } catch (error) {
        vscode.window.showErrorMessage(`Failed to apply auto-fix: ${error}`);
        console.error('Auto-fix error:', error);
    }
}

/**
 * Apply all auto-fixes that are available
 */
async function applyAllAutoFixes(suggestions: any[]) {
    if (!suggestions || suggestions.length === 0) {
        vscode.window.showInformationMessage('No auto-fixes available');
        return;
    }

    const autoFixableSuggestions = suggestions.filter(s => s.autoFixAvailable && s.afterCode);

    if (autoFixableSuggestions.length === 0) {
        vscode.window.showInformationMessage('No automatic fixes available. All suggestions require manual intervention.');
        return;
    }

    const result = await vscode.window.showWarningMessage(
        `Apply ${autoFixableSuggestions.length} auto-fix(es)?`,
        'Yes',
        'No'
    );

    if (result !== 'Yes') {
        return;
    }

    let successCount = 0;
    let failCount = 0;

    for (const suggestion of autoFixableSuggestions) {
        try {
            await applyAutoFix(suggestion);
            successCount++;
        } catch (error) {
            failCount++;
            console.error('Failed to apply fix:', error);
        }
    }

    if (successCount > 0) {
        vscode.window.showInformationMessage(
            `✅ Applied ${successCount} auto-fix(es) successfully` +
            (failCount > 0 ? `. ${failCount} failed.` : '')
        );
    } else {
        vscode.window.showErrorMessage(`❌ All auto-fixes failed`);
    }

    // Re-analyze workspace after bulk fixes
    await analyzeWorkspace();
}

export function deactivate() {
    if (webServer) {
        webServer.stop();
    }
    if (pragmiteService) {
        pragmiteService.dispose();
    }
    if (diagnosticProvider) {
        diagnosticProvider.dispose();
    }
    if (decorationProvider) {
        decorationProvider.dispose();
    }
    if (statusBarItem) {
        statusBarItem.dispose();
    }
}
