import * as vscode from 'vscode';
import { CodeSmell, AnalysisResult } from './models';
import * as path from 'path';

export class PragmiteTreeProvider implements vscode.TreeDataProvider<TreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<TreeItem | undefined | null | void> =
        new vscode.EventEmitter<TreeItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<TreeItem | undefined | null | void> =
        this._onDidChangeTreeData.event;

    private analysisResult: AnalysisResult | null = null;

    constructor() {}

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    updateAnalysisResult(result: AnalysisResult | null): void {
        this.analysisResult = result;
        this.refresh();
    }

    getTreeItem(element: TreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: TreeItem): Thenable<TreeItem[]> {
        if (!this.analysisResult) {
            return Promise.resolve([]);
        }

        if (!element) {
            // Root level: show categories
            return Promise.resolve(this.getRootCategories());
        } else if (element.contextValue === 'category') {
            // Show items in category
            return Promise.resolve(this.getCategoryItems(element.label as string));
        }

        return Promise.resolve([]);
    }

    private getRootCategories(): TreeItem[] {
        if (!this.analysisResult) {
            return [];
        }

        const items: TreeItem[] = [];

        // Quality Score
        const score = this.analysisResult.qualityScore;
        items.push(new TreeItem(
            `Quality Score: ${score.overallScore}/100 (${score.grade})`,
            vscode.TreeItemCollapsibleState.None,
            'score',
            this.getGradeIcon(score.grade)
        ));

        // Code Smells by Severity
        const smells = this.analysisResult.codeSmells;
        const critical = smells.filter(s => s.severity === 'CRITICAL').length;
        const major = smells.filter(s => s.severity === 'MAJOR').length;
        const minor = smells.filter(s => s.severity === 'MINOR').length;

        if (critical > 0) {
            items.push(new TreeItem(
                `Critical Issues (${critical})`,
                vscode.TreeItemCollapsibleState.Collapsed,
                'category',
                '$(error)'
            ));
        }

        if (major > 0) {
            items.push(new TreeItem(
                `Major Issues (${major})`,
                vscode.TreeItemCollapsibleState.Collapsed,
                'category',
                '$(warning)'
            ));
        }

        if (minor > 0) {
            items.push(new TreeItem(
                `Minor Issues (${minor})`,
                vscode.TreeItemCollapsibleState.Collapsed,
                'category',
                '$(info)'
            ));
        }

        // High Complexity Methods
        const highComplexity = this.analysisResult.complexities.filter(c =>
            ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL', 'O_FACTORIAL'].includes(c.complexity)
        );
        if (highComplexity.length > 0) {
            items.push(new TreeItem(
                `High Complexity (${highComplexity.length})`,
                vscode.TreeItemCollapsibleState.Collapsed,
                'category',
                '$(flame)'
            ));
        }

        return items;
    }

    private getCategoryItems(category: string): TreeItem[] {
        if (!this.analysisResult) {
            return [];
        }

        const items: TreeItem[] = [];

        if (category.startsWith('Critical Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'CRITICAL');
            return this.createSmellItems(smells);
        } else if (category.startsWith('Major Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'MAJOR');
            return this.createSmellItems(smells);
        } else if (category.startsWith('Minor Issues')) {
            const smells = this.analysisResult.codeSmells.filter(s => s.severity === 'MINOR');
            return this.createSmellItems(smells);
        } else if (category.startsWith('High Complexity')) {
            const complexities = this.analysisResult.complexities.filter(c =>
                ['O_N_SQUARED', 'O_N_CUBED', 'O_EXPONENTIAL', 'O_FACTORIAL'].includes(c.complexity)
            );
            return complexities.map(c => {
                const fileName = path.basename(c.filePath);
                const item = new TreeItem(
                    `${c.methodName} (${c.complexity})`,
                    vscode.TreeItemCollapsibleState.None,
                    'complexity',
                    '$(flame)'
                );
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

    private createSmellItems(smells: CodeSmell[]): TreeItem[] {
        return smells.map(smell => {
            const fileName = path.basename(smell.filePath);
            const item = new TreeItem(
                smell.type.replace(/_/g, ' '),
                vscode.TreeItemCollapsibleState.None,
                'smell',
                this.getSmellIcon(smell.severity)
            );
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

    private getGradeIcon(grade: string): string {
        switch (grade) {
            case 'A': return '$(pass-filled)';
            case 'B': return '$(pass)';
            case 'C': return '$(circle-slash)';
            case 'D': return '$(warning)';
            case 'F': return '$(error)';
            default: return '$(question)';
        }
    }

    private getSmellIcon(severity: string): string {
        switch (severity) {
            case 'CRITICAL': return '$(error)';
            case 'MAJOR': return '$(warning)';
            case 'MINOR': return '$(info)';
            default: return '$(circle-outline)';
        }
    }
}

class TreeItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly contextValue: string,
        iconString?: string
    ) {
        super(label, collapsibleState);
        if (iconString) {
            this.iconPath = new vscode.ThemeIcon(iconString.replace('$(', '').replace(')', ''));
        }
    }
}
