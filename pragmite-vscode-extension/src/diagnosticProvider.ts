import * as vscode from 'vscode';
import { CodeSmell, FileAnalysis, ComplexityInfo } from './models';

export class DiagnosticProvider {
    private diagnosticCollection: vscode.DiagnosticCollection;

    constructor() {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('pragmite');
    }

    updateDiagnostics(uri: vscode.Uri, fileAnalysis: FileAnalysis | null) {
        if (!fileAnalysis) {
            this.diagnosticCollection.delete(uri);
            return;
        }

        const diagnostics: vscode.Diagnostic[] = [];

        for (const smell of fileAnalysis.smells) {
            const diagnostic = this.createDiagnosticFromSmell(smell);
            if (diagnostic) {
                diagnostics.push(diagnostic);
            }
        }

        for (const complexity of fileAnalysis.complexities) {
            const diagnostic = this.createDiagnosticFromComplexity(complexity);
            if (diagnostic) {
                diagnostics.push(diagnostic);
            }
        }

        this.diagnosticCollection.set(uri, diagnostics);
    }

    private createDiagnosticFromSmell(smell: CodeSmell): vscode.Diagnostic | null {
        try {
            const line = Math.max(0, smell.startLine - 1);
            const endLine = Math.max(line, smell.endLine - 1);

            const range = new vscode.Range(
                new vscode.Position(line, 0),
                new vscode.Position(endLine, Number.MAX_SAFE_INTEGER)
            );

            let message = `[${smell.type}] ${smell.message}`;
            if (smell.suggestion) {
                message += `\n\n💡 Çözüm: ${smell.suggestion}`;
            }

            const severity = this.mapSeverity(smell.severity);
            const diagnostic = new vscode.Diagnostic(range, message, severity);

            diagnostic.source = 'Pragmite';
            diagnostic.code = smell.type;

            return diagnostic;
        } catch (error) {
            console.error('Error creating diagnostic from smell:', error);
            return null;
        }
    }

    private createDiagnosticFromComplexity(complexity: ComplexityInfo): vscode.Diagnostic | null {
        try {
            if (!this.isHighComplexity(complexity.complexity)) {
                return null;
            }

            const line = Math.max(0, complexity.lineNumber - 1);
            const range = new vscode.Range(
                new vscode.Position(line, 0),
                new vscode.Position(line, Number.MAX_SAFE_INTEGER)
            );

            let message = `Yüksek karmaşıklık (${complexity.complexity}) - '${complexity.methodName}' metodu`;
            if (complexity.reason) {
                message += `\n${complexity.reason}`;
            }

            message += '\n\n💡 Çözüm: ';
            if (complexity.complexity === 'O_N_SQUARED') {
                message += 'İç içe döngüleri azalt, daha verimli veri yapıları kullan (HashMap, HashSet)';
            } else if (complexity.complexity === 'O_N_CUBED') {
                message += 'Üç seviye iç içe döngü var - algoritma tasarımını yeniden düşün';
            } else if (complexity.complexity === 'O_EXPONENTIAL') {
                message += 'Exponential karmaşıklık! Dinamik programlama veya memoization kullan';
            } else if (complexity.complexity === 'O_FACTORIAL') {
                message += 'Factorial karmaşıklık! Alternatif algoritma aramak kritik';
            } else {
                message += 'Metodu daha küçük parçalara böl, sorumluluklarını azalt';
            }

            const diagnostic = new vscode.Diagnostic(range, message, vscode.DiagnosticSeverity.Warning);
            diagnostic.source = 'Pragmite Complexity';
            diagnostic.code = 'HIGH_COMPLEXITY';

            return diagnostic;
        } catch (error) {
            console.error('Error creating diagnostic from complexity:', error);
            return null;
        }
    }

    private mapSeverity(severity: string): vscode.DiagnosticSeverity {
        switch (severity) {
            case 'CRITICAL':
                return vscode.DiagnosticSeverity.Error;
            case 'MAJOR':
                return vscode.DiagnosticSeverity.Warning;
            case 'MINOR':
                return vscode.DiagnosticSeverity.Information;
            default:
                return vscode.DiagnosticSeverity.Hint;
        }
    }

    private isHighComplexity(complexity: string): boolean {
        const highComplexities = ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL', 'O_FACTORIAL'];
        return highComplexities.includes(complexity);
    }

    clear() {
        this.diagnosticCollection.clear();
    }

    dispose() {
        this.diagnosticCollection.dispose();
    }
}
