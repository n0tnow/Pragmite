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
exports.PragmiteService = void 0;
const vscode = __importStar(require("vscode"));
const path = __importStar(require("path"));
const child_process = __importStar(require("child_process"));
class PragmiteService {
    constructor(context) {
        this.jarPath = path.join(context.extensionPath, 'lib', 'pragmite-core-1.0.5.jar');
        const config = vscode.workspace.getConfiguration('pragmite');
        this.javaPath = config.get('javaPath', 'java');
        this.outputChannel = vscode.window.createOutputChannel('Pragmite');
    }
    /**
     * Analyze a single Java file (returns both file analysis and full result for dashboard)
     */
    async analyzeFile(filePath) {
        try {
            this.outputChannel.appendLine(`Analyzing file: ${filePath}`);
            // Find workspace root (Pragmite needs project directory, not single file)
            const workspaceFolder = vscode.workspace.getWorkspaceFolder(vscode.Uri.file(filePath));
            if (!workspaceFolder) {
                this.outputChannel.appendLine('Error: File is not in a workspace');
                return { fileAnalysis: null, fullResult: null };
            }
            const projectRoot = workspaceFolder.uri.fsPath;
            this.outputChannel.appendLine(`Using project root: ${projectRoot}`);
            // Run analysis on entire project
            const result = await this.runPragmite(projectRoot);
            if (!result || !result.fileAnalyses || result.fileAnalyses.length === 0) {
                return { fileAnalysis: null, fullResult: null };
            }
            // Filter results for the specific file
            const normalizedFilePath = filePath.replace(/\\/g, '/');
            const fileAnalysis = result.fileAnalyses.find(fa => fa.filePath.replace(/\\/g, '/').endsWith(normalizedFilePath.split('/').pop() || ''));
            return { fileAnalysis: fileAnalysis || null, fullResult: result };
        }
        catch (error) {
            this.outputChannel.appendLine(`Error analyzing file: ${error}`);
            return { fileAnalysis: null, fullResult: null };
        }
    }
    /**
     * Analyze entire workspace or directory
     */
    async analyzeWorkspace(workspacePath) {
        try {
            this.outputChannel.appendLine(`Analyzing workspace: ${workspacePath}`);
            return await this.runPragmite(workspacePath);
        }
        catch (error) {
            this.outputChannel.appendLine(`Error analyzing workspace: ${error}`);
            vscode.window.showErrorMessage(`Pragmite analysis failed: ${error}`);
            return null;
        }
    }
    /**
     * Execute Pragmite JAR with given path
     */
    async runPragmite(targetPath) {
        return new Promise((resolve, reject) => {
            const outputFile = path.join(require('os').tmpdir(), `pragmite-${Date.now()}.json`);
            const args = [
                '-jar',
                this.jarPath,
                targetPath,
                '-f', 'json',
                '-o', outputFile
            ];
            this.outputChannel.appendLine(`Running: ${this.javaPath} ${args.join(' ')}`);
            const process = child_process.spawn(this.javaPath, args, {
                cwd: path.dirname(this.jarPath)
            });
            let stdout = '';
            let stderr = '';
            process.stdout.on('data', (data) => {
                stdout += data.toString();
                this.outputChannel.append(data.toString());
            });
            process.stderr.on('data', (data) => {
                stderr += data.toString();
                this.outputChannel.append(`[ERROR] ${data.toString()}`);
            });
            process.on('close', (code) => {
                if (code !== 0) {
                    reject(new Error(`Pragmite exited with code ${code}: ${stderr}`));
                    return;
                }
                try {
                    const fs = require('fs');
                    if (!fs.existsSync(outputFile)) {
                        reject(new Error(`Output file not created: ${outputFile}`));
                        return;
                    }
                    const result = JSON.parse(fs.readFileSync(outputFile, 'utf-8'));
                    // Clean up temp file
                    fs.unlinkSync(outputFile);
                    resolve(result);
                }
                catch (error) {
                    reject(error);
                }
            });
            process.on('error', (error) => {
                reject(error);
            });
        });
    }
    /**
     * Show output channel
     */
    showOutput() {
        this.outputChannel.show();
    }
    /**
     * Dispose resources
     */
    dispose() {
        this.outputChannel.dispose();
    }
}
exports.PragmiteService = PragmiteService;
//# sourceMappingURL=pragmiteService.js.map