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
exports.PragmiteTreeProvider = void 0;
const vscode = __importStar(require("vscode"));
const path = __importStar(require("path"));
class PragmiteTreeProvider {
    constructor() {
        this._onDidChangeTreeData = new vscode.EventEmitter();
        this.onDidChangeTreeData = this._onDidChangeTreeData.event;
        this.analysisResult = null;
    }
    refresh() {
        this._onDidChangeTreeData.fire();
    }
    updateAnalysisResult(result) {
        this.analysisResult = result;
        this.refresh();
    }
    getTreeItem(element) {
        return element;
    }
    getChildren(element) {
        if (!this.analysisResult) {
            return Promise.resolve([]);
        }
        if (!element) {
            // Root level: show categories
            return Promise.resolve(this.getRootCategories());
        }
        else if (element.contextValue === 'category') {
            // Show items in category
            return Promise.resolve(this.getCategoryItems(element.label));
        }
        return Promise.resolve([]);
    }
    getRootCategories() {
        if (!this.analysisResult) {
            return [];
        }
        const items = [];
        // Quality Score
        const score = this.analysisResult.qualityScore;
        items.push(new TreeItem(`Quality Score: ${score.overallScore}/100 (${score.grade})`, vscode.TreeItemCollapsibleState.None, 'score', this.getGradeIcon(score.grade)));
        // Code Smells by Severity
        const smells = this.analysisResult.codeSmells;
        const critical = smells.filter(s => s.severity === 'CRITICAL').length;
        const major = smells.filter(s => s.severity === 'MAJOR').length;
        const minor = smells.filter(s => s.severity === 'MINOR').length;
        if (critical > 0) {
            items.push(new TreeItem(`Critical Issues (${critical})`, vscode.TreeItemCollapsibleState.Collapsed, 'category', '$(error)'));
        }
        if (major > 0) {
            items.push(new TreeItem(`Major Issues (${major})`, vscode.TreeItemCollapsibleState.Collapsed, 'category', '$(warning)'));
        }
        if (minor > 0) {
            items.push(new TreeItem(`Minor Issues (${minor})`, vscode.TreeItemCollapsibleState.Collapsed, 'category', '$(info)'));
        }
        // High Complexity Methods
        const highComplexity = this.analysisResult.complexities.filter(c => ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL', 'O_FACTORIAL'].includes(c.complexity));
        if (highComplexity.length > 0) {
            items.push(new TreeItem(`High Complexity (${highComplexity.length})`, vscode.TreeItemCollapsibleState.Collapsed, 'category', '$(flame)'));
        }
        return items;
    }
    getCategoryItems(category) {
        if (!this.analysisResult) {
            return [];
        }
        const items = [];
        if (category.startsWith('Critical Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'CRITICAL');
            return this.createSmellItems(smells);
        }
        else if (category.startsWith('Major Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'MAJOR');
            return this.createSmellItems(smells);
        }
        else if (category.startsWith('Minor Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'MINOR');
            return this.createSmellItems(smells);
        }
        else if (category.startsWith('High Complexity')) {
            const complexities = this.analysisResult.complexities.filter(c => ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL', 'O_FACTORIAL'].includes(c.complexity));
            return complexities.map(c => {
                const fileName = path.basename(c.filePath);
                const item = new TreeItem(`${c.methodName} (${c.complexity})`, vscode.TreeItemCollapsibleState.None, 'complexity', '$(flame)');
                item.description = `${fileName}:${c.lineNumber}`;
                item.tooltip = `${c.complexity} complexity in ${c.methodName}\n${c.reason || ''}`;
                item.command = {
                    command: 'vscode.open',
                    title: 'Open File',
                    arguments: [
                        vscode.Uri.file(c.filePath),
                        { selection: new vscode.Range(c.lineNumber - 1, 0, c.lineNumber - 1, 0) }
                    ]
                };
                return item;
            });
        }
        return items;
    }
    createSmellItems(smells) {
        return smells.map(smell => {
            const fileName = path.basename(smell.filePath);
            const item = new TreeItem(smell.type.replace(/_/g, ' '), vscode.TreeItemCollapsibleState.None, 'smell', this.getSmellIcon(smell.severity));
            item.description = `${fileName}:${smell.startLine}`;
            item.tooltip = smell.message + (smell.suggestion ? `\n\nSuggestion: ${smell.suggestion}` : '');
            item.command = {
                command: 'vscode.open',
                title: 'Open File',
                arguments: [
                    vscode.Uri.file(smell.filePath),
                    { selection: new vscode.Range(smell.startLine - 1, 0, smell.endLine - 1, 0) }
                ]
            };
            return item;
        });
    }
    getGradeIcon(grade) {
        switch (grade) {
            case 'A': return '$(pass-filled)';
            case 'B': return '$(pass)';
            case 'C': return '$(circle-slash)';
            case 'D': return '$(warning)';
            case 'F': return '$(error)';
            default: return '$(question)';
        }
    }
    getSmellIcon(severity) {
        switch (severity) {
            case 'CRITICAL': return '$(error)';
            case 'MAJOR': return '$(warning)';
            case 'MINOR': return '$(info)';
            default: return '$(circle-outline)';
        }
    }
}
exports.PragmiteTreeProvider = PragmiteTreeProvider;
class TreeItem extends vscode.TreeItem {
    constructor(label, collapsibleState, contextValue, iconString) {
        super(label, collapsibleState);
        this.label = label;
        this.collapsibleState = collapsibleState;
        this.contextValue = contextValue;
        if (iconString) {
            this.iconPath = new vscode.ThemeIcon(iconString.replace('$(', '').replace(')', ''));
        }
    }
}
//# sourceMappingURL=treeViewProvider.js.map