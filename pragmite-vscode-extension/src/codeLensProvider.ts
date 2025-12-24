import * as vscode from 'vscode';
import { ComplexityInfo, FileAnalysis } from './models';

export class PragmiteCodeLensProvider implements vscode.CodeLensProvider {
    private _onDidChangeCodeLenses: vscode.EventEmitter<void> = new vscode.EventEmitter<void>();
    public readonly onDidChangeCodeLenses: vscode.Event<void> = this._onDidChangeCodeLenses.event;

    private fileAnalyses: Map<string, FileAnalysis> = new Map();

    constructor() {}

    refresh(): void {
        this._onDidChangeCodeLenses.fire();
    }

    updateFileAnalysis(uri: vscode.Uri, fileAnalysis: FileAnalysis | null): void {
        if (fileAnalysis) {
            this.fileAnalyses.set(uri.fsPath, fileAnalysis);
        } else {
            this.fileAnalyses.delete(uri.fsPath);
        }
        this.refresh();
    }

    provideCodeLenses(
        document: vscode.TextDocument,
        token: vscode.CancellationToken
    ): vscode.CodeLens[] | Thenable<vscode.CodeLens[]> {
        const fileAnalysis = this.fileAnalyses.get(document.uri.fsPath);
        if (!fileAnalysis) {
            return [];
        }

        const codeLenses: vscode.CodeLens[] = [];

        // Add code lens for each method with complexity info
        for (const complexity of fileAnalysis.complexities) {
            const line = complexity.lineNumber - 1;
            const range = new vscode.Range(line, 0, line, 0);

            // Complexity info code lens
            codeLenses.push(new vscode.CodeLens(range, {
                title: this.getComplexityTitle(complexity),
                tooltip: this.getComplexityTooltip(complexity),
                command: ''
            }));
        }

        // Add code lens for methods with code smells
        const methodSmells = this.groupSmellsByMethod(fileAnalysis);
        for (const [methodName, smells] of methodSmells.entries()) {
            const firstSmell = smells[0];
            const line = firstSmell.startLine - 1;
            const range = new vscode.Range(line, 0, line, 0);

            codeLenses.push(new vscode.CodeLens(range, {
                title: `$(warning) ${smells.length} issue${smells.length > 1 ? 's' : ''}`,
                tooltip: smells.map(s => s.message).join('\n'),
                command: 'pragmite.showSmellDetails',
                arguments: [smells]
            }));
        }

        return codeLenses;
    }

    private getComplexityTitle(complexity: ComplexityInfo): string {
        const icon = this.getComplexityIcon(complexity.complexity);
        return `${icon} ${complexity.complexity} | CC: ${complexity.cyclomaticComplexity}`;
    }

    private getComplexityTooltip(complexity: ComplexityInfo): string {
        let tooltip = `Time Complexity: ${complexity.complexity}\n`;
        tooltip += `Cyclomatic Complexity: ${complexity.cyclomaticComplexity}\n`;
        tooltip += `Nested Loop Depth: ${complexity.nestedLoopDepth}`;
        if (complexity.reason) {
            tooltip += `\n\nReason: ${complexity.reason}`;
        }
        return tooltip;
    }

    private getComplexityIcon(complexity: string): string {
        switch (complexity) {
            case 'O_1':
                return '$(check)'; // Green check - excellent
            case 'O_LOG_N':
                return '$(chevron-up)'; // Logarithmic - very good
            case 'O_N':
                return '$(arrow-right)'; // Linear - acceptable
            case 'O_N_LOG_N':
                return '$(graph-line)'; // Linearithmic - okay
            case 'O_N_SQUARED':
                return '$(warning)'; // Quadratic - warning
            case 'O_N_CUBED':
                return '$(error)'; // Cubic - error
            case 'O_EXPONENTIAL':
            case 'O_FACTORIAL':
                return '$(flame)'; // Exponential/Factorial - critical
            default:
                return '$(question)';
        }
    }

    private groupSmellsByMethod(fileAnalysis: FileAnalysis): Map<string, any[]> {
        const grouped = new Map<string, any[]>();

        for (const smell of fileAnalysis.smells) {
            // Find method that contains this smell
            const method = fileAnalysis.methods.find(m =>
                m.startLine <= smell.startLine && m.endLine >= smell.endLine
            );

            if (method) {
                const key = `${method.name}:${method.startLine}`;
                if (!grouped.has(key)) {
                    grouped.set(key, []);
                }
                grouped.get(key)!.push(smell);
            }
        }

        return grouped;
    }
}
