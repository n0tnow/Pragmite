import * as vscode from 'vscode';
import { CodeSmell } from './models';

export class PragmiteQuickFixProvider implements vscode.CodeActionProvider {
    private smellsByFile: Map<string, CodeSmell[]> = new Map();

    constructor() {}

    updateSmells(uri: vscode.Uri, smells: CodeSmell[]): void {
        this.smellsByFile.set(uri.fsPath, smells);
    }

    provideCodeActions(
        document: vscode.TextDocument,
        range: vscode.Range | vscode.Selection,
        context: vscode.CodeActionContext,
        token: vscode.CancellationToken
    ): vscode.CodeAction[] | Thenable<vscode.CodeAction[]> {
        const actions: vscode.CodeAction[] = [];
        const smells = this.smellsByFile.get(document.uri.fsPath) || [];

        // Find smells in the current range
        const relevantSmells = smells.filter(smell =>
            smell.startLine - 1 <= range.start.line && smell.endLine - 1 >= range.end.line
        );

        for (const smell of relevantSmells) {
            const fixes = this.getQuickFixesForSmell(smell, document);
            actions.push(...fixes);
        }

        return actions;
    }

    private getQuickFixesForSmell(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction[] {
        const actions: vscode.CodeAction[] = [];

        switch (smell.type) {
            case 'MAGIC_NUMBER':
                actions.push(this.createExtractConstantAction(smell, document));
                break;

            case 'MAGIC_STRING':
                actions.push(this.createExtractConstantAction(smell, document));
                break;

            case 'UNUSED_IMPORT':
                actions.push(this.createRemoveImportAction(smell, document));
                break;

            case 'UNUSED_VARIABLE':
                actions.push(this.createRemoveVariableAction(smell, document));
                break;

            case 'EMPTY_CATCH_BLOCK':
                actions.push(this.createAddLoggingAction(smell, document));
                break;

            case 'MISSING_TRY_WITH_RESOURCES':
                actions.push(this.createConvertToTryWithResourcesAction(smell, document));
                break;

            case 'DUPLICATED_CODE':
                actions.push(this.createExtractMethodAction(smell, document));
                break;
        }

        // Always add "Ignore this issue" option
        actions.push(this.createIgnoreAction(smell, document));

        return actions;
    }

    private createExtractConstantAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '💡 Extract to constant',
            vscode.CodeActionKind.QuickFix
        );
        action.isPreferred = true;

        // This would require more sophisticated analysis to implement properly
        // For now, we'll show a message
        action.command = {
            command: 'pragmite.showRefactoringSuggestion',
            title: 'Show Refactoring Suggestion',
            arguments: [{
                type: 'EXTRACT_CONSTANT',
                smell: smell,
                message: 'Extract magic value to a named constant:\n\n' +
                        'private static final TYPE CONSTANT_NAME = value;'
            }]
        };

        return action;
    }

    private createRemoveImportAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '🗑️ Remove unused import',
            vscode.CodeActionKind.QuickFix
        );
        action.isPreferred = true;

        const line = smell.startLine - 1;
        const edit = new vscode.WorkspaceEdit();
        edit.delete(document.uri, new vscode.Range(line, 0, line + 1, 0));

        action.edit = edit;
        return action;
    }

    private createRemoveVariableAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '🗑️ Remove unused variable',
            vscode.CodeActionKind.QuickFix
        );

        const range = new vscode.Range(
            smell.startLine - 1, 0,
            smell.endLine - 1, Number.MAX_SAFE_INTEGER
        );

        const edit = new vscode.WorkspaceEdit();
        edit.delete(document.uri, range);

        action.edit = edit;
        return action;
    }

    private createAddLoggingAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '📝 Add exception logging',
            vscode.CodeActionKind.QuickFix
        );
        action.isPreferred = true;

        action.command = {
            command: 'pragmite.showRefactoringSuggestion',
            title: 'Show Refactoring Suggestion',
            arguments: [{
                type: 'ADD_LOGGING',
                smell: smell,
                message: 'Add proper exception handling:\n\n' +
                        'catch (Exception e) {\n' +
                        '    logger.error("Error message", e);\n' +
                        '    // Handle exception appropriately\n' +
                        '}'
            }]
        };

        return action;
    }

    private createConvertToTryWithResourcesAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '🔄 Convert to try-with-resources',
            vscode.CodeActionKind.RefactorRewrite
        );

        action.command = {
            command: 'pragmite.showRefactoringSuggestion',
            title: 'Show Refactoring Suggestion',
            arguments: [{
                type: 'TRY_WITH_RESOURCES',
                smell: smell,
                message: 'Use try-with-resources for automatic resource management:\n\n' +
                        'try (ResourceType resource = new ResourceType()) {\n' +
                        '    // Use resource\n' +
                        '} // Automatically closed'
            }]
        };

        return action;
    }

    private createExtractMethodAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '🔧 Extract duplicated code to method',
            vscode.CodeActionKind.RefactorExtract
        );

        action.command = {
            command: 'pragmite.showRefactoringSuggestion',
            title: 'Show Refactoring Suggestion',
            arguments: [{
                type: 'EXTRACT_METHOD',
                smell: smell,
                message: 'Extract duplicated code into a reusable method:\n\n' +
                        'private ReturnType extractedMethod(Parameters params) {\n' +
                        '    // Duplicated code here\n' +
                        '}'
            }]
        };

        return action;
    }

    private createIgnoreAction(smell: CodeSmell, document: vscode.TextDocument): vscode.CodeAction {
        const action = new vscode.CodeAction(
            '🙈 Ignore this issue',
            vscode.CodeActionKind.Empty
        );

        const line = smell.startLine - 1;
        const edit = new vscode.WorkspaceEdit();

        // Add @SuppressWarnings annotation
        const indent = document.lineAt(line).text.match(/^\s*/)?.[0] || '';
        edit.insert(
            document.uri,
            new vscode.Position(line, 0),
            `${indent}@SuppressWarnings("pragmite:${smell.type}")\n`
        );

        action.edit = edit;
        return action;
    }
}
