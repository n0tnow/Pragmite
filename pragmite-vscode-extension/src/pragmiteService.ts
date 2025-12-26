import * as vscode from 'vscode';
import * as path from 'path';
import * as child_process from 'child_process';
import { AnalysisResult, FileAnalysis } from './models';

export class PragmiteService {
    private jarPath: string;
    private javaPath: string;
    private outputChannel: vscode.OutputChannel;

    constructor(context: vscode.ExtensionContext) {
        this.jarPath = path.join(context.extensionPath, 'lib', 'pragmite-core-1.5.0.jar');
        const config = vscode.workspace.getConfiguration('pragmite');
        this.javaPath = config.get('javaPath', 'java');
        this.outputChannel = vscode.window.createOutputChannel('Pragmite');
    }

    /**
     * Analyze a single Java file (returns both file analysis and full result for dashboard)
     */
    async analyzeFile(filePath: string): Promise<{ fileAnalysis: FileAnalysis | null, fullResult: AnalysisResult | null }> {
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
            const fileAnalysis = result.fileAnalyses.find(fa =>
                fa.filePath.replace(/\\/g, '/').endsWith(normalizedFilePath.split('/').pop() || '')
            );

            return { fileAnalysis: fileAnalysis || null, fullResult: result };
        } catch (error) {
            this.outputChannel.appendLine(`Error analyzing file: ${error}`);
            return { fileAnalysis: null, fullResult: null };
        }
    }

    /**
     * Analyze entire workspace or directory
     */
    async analyzeWorkspace(workspacePath: string): Promise<AnalysisResult | null> {
        try {
            this.outputChannel.appendLine(`Analyzing workspace: ${workspacePath}`);
            return await this.runPragmite(workspacePath);
        } catch (error) {
            this.outputChannel.appendLine(`Error analyzing workspace: ${error}`);
            vscode.window.showErrorMessage(`Pragmite analysis failed: ${error}`);
            return null;
        }
    }

    /**
     * Execute Pragmite JAR with given path
     */
    private async runPragmite(targetPath: string): Promise<AnalysisResult | null> {
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

                    const result: AnalysisResult = JSON.parse(fs.readFileSync(outputFile, 'utf-8'));

                    // Clean up temp file
                    fs.unlinkSync(outputFile);

                    resolve(result);
                } catch (error) {
                    reject(error);
                }
            });

            process.on('error', (error) => {
                reject(error);
            });
        });
    }

    /**
     * Get JAR path for external use (v1.5.0)
     */
    getJarPath(): string {
        return this.jarPath;
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
