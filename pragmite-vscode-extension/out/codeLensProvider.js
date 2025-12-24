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
exports.PragmiteCodeLensProvider = void 0;
const vscode = __importStar(require("vscode"));
class PragmiteCodeLensProvider {
    constructor() {
        this._onDidChangeCodeLenses = new vscode.EventEmitter();
        this.onDidChangeCodeLenses = this._onDidChangeCodeLenses.event;
        this.fileAnalyses = new Map();
    }
    refresh() {
        this._onDidChangeCodeLenses.fire();
    }
    updateFileAnalysis(uri, fileAnalysis) {
        if (fileAnalysis) {
            this.fileAnalyses.set(uri.fsPath, fileAnalysis);
        }
        else {
            this.fileAnalyses.delete(uri.fsPath);
        }
        this.refresh();
    }
    provideCodeLenses(document, token) {
        const fileAnalysis = this.fileAnalyses.get(document.uri.fsPath);
        if (!fileAnalysis) {
            return [];
        }
        const codeLenses = [];
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
    getComplexityTitle(complexity) {
        const icon = this.getComplexityIcon(complexity.complexity);
        return `${icon} ${complexity.complexity} | CC: ${complexity.cyclomaticComplexity}`;
    }
    getComplexityTooltip(complexity) {
        let tooltip = `Time Complexity: ${complexity.complexity}\n`;
        tooltip += `Cyclomatic Complexity: ${complexity.cyclomaticComplexity}\n`;
        tooltip += `Nested Loop Depth: ${complexity.nestedLoopDepth}`;
        if (complexity.reason) {
            tooltip += `\n\nReason: ${complexity.reason}`;
        }
        return tooltip;
    }
    getComplexityIcon(complexity) {
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
    groupSmellsByMethod(fileAnalysis) {
        const grouped = new Map();
        for (const smell of fileAnalysis.smells) {
            // Find method that contains this smell
            const method = fileAnalysis.methods.find(m => m.startLine <= smell.startLine && m.endLine >= smell.endLine);
            if (method) {
                const key = `${method.name}:${method.startLine}`;
                if (!grouped.has(key)) {
                    grouped.set(key, []);
                }
                grouped.get(key).push(smell);
            }
        }
        return grouped;
    }
}
exports.PragmiteCodeLensProvider = PragmiteCodeLensProvider;
//# sourceMappingURL=codeLensProvider.js.map