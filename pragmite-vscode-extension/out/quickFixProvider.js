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
exports.PragmiteQuickFixProvider = void 0;
const vscode = __importStar(require("vscode"));
class PragmiteQuickFixProvider {
    constructor() {
        this.smellsByFile = new Map();
    }
    updateSmells(uri, smells) {
        this.smellsByFile.set(uri.fsPath, smells);
    }
    provideCodeActions(document, range, context, token) {
        const actions = [];
        const smells = this.smellsByFile.get(document.uri.fsPath) || [];
        // Find smells in the current range
        const relevantSmells = smells.filter(smell => smell.startLine - 1 <= range.start.line && smell.endLine - 1 >= range.end.line);
        for (const smell of relevantSmells) {
            const fixes = this.getQuickFixesForSmell(smell, document);
            actions.push(...fixes);
        }
        return actions;
    }
    getQuickFixesForSmell(smell, document) {
        const actions = [];
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
    createExtractConstantAction(smell, document) {
        const action = new vscode.CodeAction('💡 Extract to constant', vscode.CodeActionKind.QuickFix);
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
    createRemoveImportAction(smell, document) {
        const action = new vscode.CodeAction('🗑️ Remove unused import', vscode.CodeActionKind.QuickFix);
        action.isPreferred = true;
        const line = smell.startLine - 1;
        const edit = new vscode.WorkspaceEdit();
        edit.delete(document.uri, new vscode.Range(line, 0, line + 1, 0));
        action.edit = edit;
        return action;
    }
    createRemoveVariableAction(smell, document) {
        const action = new vscode.CodeAction('🗑️ Remove unused variable', vscode.CodeActionKind.QuickFix);
        const range = new vscode.Range(smell.startLine - 1, 0, smell.endLine - 1, Number.MAX_SAFE_INTEGER);
        const edit = new vscode.WorkspaceEdit();
        edit.delete(document.uri, range);
        action.edit = edit;
        return action;
    }
    createAddLoggingAction(smell, document) {
        const action = new vscode.CodeAction('📝 Add exception logging', vscode.CodeActionKind.QuickFix);
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
    createConvertToTryWithResourcesAction(smell, document) {
        const action = new vscode.CodeAction('🔄 Convert to try-with-resources', vscode.CodeActionKind.RefactorRewrite);
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
    createExtractMethodAction(smell, document) {
        const action = new vscode.CodeAction('🔧 Extract duplicated code to method', vscode.CodeActionKind.RefactorExtract);
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
    createIgnoreAction(smell, document) {
        const action = new vscode.CodeAction('🙈 Ignore this issue', vscode.CodeActionKind.Empty);
        const line = smell.startLine - 1;
        const edit = new vscode.WorkspaceEdit();
        // Add @SuppressWarnings annotation
        const indent = document.lineAt(line).text.match(/^\s*/)?.[0] || '';
        edit.insert(document.uri, new vscode.Position(line, 0), `${indent}@SuppressWarnings("pragmite:${smell.type}")\n`);
        action.edit = edit;
        return action;
    }
}
exports.PragmiteQuickFixProvider = PragmiteQuickFixProvider;
//# sourceMappingURL=quickFixProvider.js.map