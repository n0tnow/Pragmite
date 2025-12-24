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
exports.DiagnosticProvider = void 0;
const vscode = __importStar(require("vscode"));
class DiagnosticProvider {
    constructor() {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('pragmite');
    }
    updateDiagnostics(uri, fileAnalysis) {
        if (!fileAnalysis) {
            this.diagnosticCollection.delete(uri);
            return;
        }
        const diagnostics = [];
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
    createDiagnosticFromSmell(smell) {
        try {
            const line = Math.max(0, smell.startLine - 1);
            const endLine = Math.max(line, smell.endLine - 1);
            const range = new vscode.Range(new vscode.Position(line, 0), new vscode.Position(endLine, Number.MAX_SAFE_INTEGER));
            let message = `[${smell.type}] ${smell.message}`;
            if (smell.suggestion) {
                message += `\n\n💡 Çözüm: ${smell.suggestion}`;
            }
            const severity = this.mapSeverity(smell.severity);
            const diagnostic = new vscode.Diagnostic(range, message, severity);
            diagnostic.source = 'Pragmite';
            diagnostic.code = smell.type;
            return diagnostic;
        }
        catch (error) {
            console.error('Error creating diagnostic from smell:', error);
            return null;
        }
    }
    createDiagnosticFromComplexity(complexity) {
        try {
            if (!this.isHighComplexity(complexity.complexity)) {
                return null;
            }
            const line = Math.max(0, complexity.lineNumber - 1);
            const range = new vscode.Range(new vscode.Position(line, 0), new vscode.Position(line, Number.MAX_SAFE_INTEGER));
            let message = `Yüksek karmaşıklık (${complexity.complexity}) - '${complexity.methodName}' metodu`;
            if (complexity.reason) {
                message += `\n${complexity.reason}`;
            }
            message += '\n\n💡 Çözüm: ';
            if (complexity.complexity === 'O_N_SQUARED') {
                message += 'İç içe döngüleri azalt, daha verimli veri yapıları kullan (HashMap, HashSet)';
            }
            else if (complexity.complexity === 'O_N_CUBED') {
                message += 'Üç seviye iç içe döngü var - algoritma tasarımını yeniden düşün';
            }
            else if (complexity.complexity === 'O_EXPONENTIAL') {
                message += 'Exponential karmaşıklık! Dinamik programlama veya memoization kullan';
            }
            else if (complexity.complexity === 'O_FACTORIAL') {
                message += 'Factorial karmaşıklık! Alternatif algoritma aramak kritik';
            }
            else {
                message += 'Metodu daha küçük parçalara böl, sorumluluklarını azalt';
            }
            const diagnostic = new vscode.Diagnostic(range, message, vscode.DiagnosticSeverity.Warning);
            diagnostic.source = 'Pragmite Complexity';
            diagnostic.code = 'HIGH_COMPLEXITY';
            return diagnostic;
        }
        catch (error) {
            console.error('Error creating diagnostic from complexity:', error);
            return null;
        }
    }
    mapSeverity(severity) {
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
    isHighComplexity(complexity) {
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
exports.DiagnosticProvider = DiagnosticProvider;
//# sourceMappingURL=diagnosticProvider.js.map