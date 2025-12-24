import * as vscode from 'vscode';
import { ComplexityInfo, FileAnalysis } from './models';

export class PragmiteDecorationProvider {
    private complexityDecorations: Map<string, vscode.TextEditorDecorationType> = new Map();

    constructor() {
        this.initializeDecorationTypes();
    }

    private initializeDecorationTypes(): void {
        // O(1) - Excellent (Green)
        this.complexityDecorations.set('O_1', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' ✓ O(1)',
                color: new vscode.ThemeColor('terminal.ansiGreen'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            },
            rangeBehavior: vscode.DecorationRangeBehavior.ClosedClosed
        }));

        // O(log n) - Very Good (Cyan)
        this.complexityDecorations.set('O_LOG_N', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' ⚡ O(log n)',
                color: new vscode.ThemeColor('terminal.ansiCyan'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            }
        }));

        // O(n) - Good (Blue)
        this.complexityDecorations.set('O_N', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' → O(n)',
                color: new vscode.ThemeColor('terminal.ansiBlue'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            }
        }));

        // O(n log n) - Acceptable (Yellow)
        this.complexityDecorations.set('O_N_LOG_N', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' ⚠ O(n log n)',
                color: new vscode.ThemeColor('terminal.ansiYellow'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            }
        }));

        // O(n²) - Warning (Orange)
        this.complexityDecorations.set('O_N_SQUARED', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' ⚠️ O(n²)',
                color: new vscode.ThemeColor('editorWarning.foreground'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            },
            backgroundColor: new vscode.ThemeColor('editorWarning.background')
        }));

        // O(n³) - Error (Red)
        this.complexityDecorations.set('O_N_CUBED', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' 🔥 O(n³)',
                color: new vscode.ThemeColor('editorError.foreground'),
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            },
            backgroundColor: new vscode.ThemeColor('editorError.background')
        }));

        // O(2^n) - Critical (Dark Red)
        this.complexityDecorations.set('O_EXPONENTIAL', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' 💥 O(2ⁿ)',
                color: '#ff0000',
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            },
            backgroundColor: 'rgba(255, 0, 0, 0.2)'
        }));

        // O(n!) - Critical (Dark Red)
        this.complexityDecorations.set('O_FACTORIAL', vscode.window.createTextEditorDecorationType({
            after: {
                contentText: ' 💣 O(n!)',
                color: '#ff0000',
                fontWeight: 'bold',
                margin: '0 0 0 1em'
            },
            backgroundColor: 'rgba(255, 0, 0, 0.3)'
        }));
    }

    updateDecorations(editor: vscode.TextEditor, fileAnalysis: FileAnalysis | null): void {
        if (!fileAnalysis || !editor) {
            this.clearDecorations(editor);
            return;
        }

        const config = vscode.workspace.getConfiguration('pragmite');
        const showInlineHints = config.get('showInlineHints', true);

        if (!showInlineHints) {
            this.clearDecorations(editor);
            return;
        }

        // Group complexities by type
        const decorationsByType = new Map<string, vscode.DecorationOptions[]>();

        for (const complexity of fileAnalysis.complexities) {
            const decorationType = this.complexityDecorations.get(complexity.complexity);
            if (!decorationType) {
                continue;
            }

            const line = complexity.lineNumber - 1;
            const lineText = editor.document.lineAt(line).text;

            // Find method declaration
            const methodMatch = lineText.match(/\b(public|private|protected|static|\s)+[\w<>[\],\s]+\s+(\w+)\s*\(/);
            if (!methodMatch) {
                continue;
            }

            const startPos = lineText.indexOf(methodMatch[2]);
            const endPos = startPos + methodMatch[2].length;

            const range = new vscode.Range(
                new vscode.Position(line, endPos),
                new vscode.Position(line, endPos)
            );

            const hoverMessage = new vscode.MarkdownString();
            hoverMessage.appendMarkdown(`**Time Complexity:** \`${complexity.complexity}\`\n\n`);
            hoverMessage.appendMarkdown(`**Cyclomatic Complexity:** ${complexity.cyclomaticComplexity}\n\n`);
            hoverMessage.appendMarkdown(`**Nested Loop Depth:** ${complexity.nestedLoopDepth}\n\n`);
            if (complexity.reason) {
                hoverMessage.appendMarkdown(`**Reason:** ${complexity.reason}`);
            }

            const decoration: vscode.DecorationOptions = {
                range: range,
                hoverMessage: hoverMessage
            };

            if (!decorationsByType.has(complexity.complexity)) {
                decorationsByType.set(complexity.complexity, []);
            }
            decorationsByType.get(complexity.complexity)!.push(decoration);
        }

        // Apply decorations
        for (const [complexityType, decorations] of decorationsByType.entries()) {
            const decorationType = this.complexityDecorations.get(complexityType);
            if (decorationType) {
                editor.setDecorations(decorationType, decorations);
            }
        }

        // Clear unused decoration types
        for (const [type, decorationType] of this.complexityDecorations.entries()) {
            if (!decorationsByType.has(type)) {
                editor.setDecorations(decorationType, []);
            }
        }
    }

    private clearDecorations(editor: vscode.TextEditor): void {
        for (const decorationType of this.complexityDecorations.values()) {
            editor.setDecorations(decorationType, []);
        }
    }

    dispose(): void {
        for (const decorationType of this.complexityDecorations.values()) {
            decorationType.dispose();
        }
        this.complexityDecorations.clear();
    }
}
