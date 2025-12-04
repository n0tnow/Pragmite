import * as http from 'http';
import * as vscode from 'vscode';
import { AnalysisResult } from './models';

export class PragmiteWebServer {
    private server: http.Server | null = null;
    private port: number = 3745;
    private latestResult: AnalysisResult | null = null;
    private outputChannel: vscode.OutputChannel;
    private sseClients: http.ServerResponse[] = [];

    constructor(outputChannel: vscode.OutputChannel) {
        this.outputChannel = outputChannel;
    }

    async start(): Promise<number> {
        return new Promise((resolve, reject) => {
            this.server = http.createServer((req, res) => {
                this.handleRequest(req, res);
            });

            this.server.on('error', (error: any) => {
                if (error.code === 'EADDRINUSE') {
                    this.port++;
                    this.outputChannel.appendLine(`Port ${this.port - 1} in use, trying ${this.port}`);
                    this.server?.listen(this.port);
                } else {
                    reject(error);
                }
            });

            this.server.listen(this.port, () => {
                this.outputChannel.appendLine(`Pragmite Dashboard started on http://localhost:${this.port}`);
                resolve(this.port);
            });
        });
    }

    private handleRequest(req: http.IncomingMessage, res: http.ServerResponse) {
        const url = req.url || '/';

        if (url === '/') {
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(this.getDashboardHtml());
            return;
        }

        if (url === '/api/analysis') {
            res.writeHead(200, {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            });
            res.end(JSON.stringify(this.latestResult || {}));
            return;
        }

        if (url === '/api/events') {
            res.writeHead(200, {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive',
                'Access-Control-Allow-Origin': '*'
            });

            res.write(`data: ${JSON.stringify({ type: 'connected' })}\n\n`);
            this.sseClients.push(res);
            this.outputChannel.appendLine(`SSE client connected. Total clients: ${this.sseClients.length}`);

            req.on('close', () => {
                this.sseClients = this.sseClients.filter(client => client !== res);
                this.outputChannel.appendLine(`SSE client disconnected. Total clients: ${this.sseClients.length}`);
            });
            return;
        }

        if (url === '/api/health') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ status: 'ok', port: this.port }));
            return;
        }

        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
    }

    updateAnalysis(result: AnalysisResult) {
        this.latestResult = result;
        this.outputChannel.appendLine(`Dashboard updated: ${result.totalFiles} files, ${result.codeSmells.length} smells`);
        this.notifyClients();
    }

    private notifyClients() {
        if (this.sseClients.length === 0) {
            return;
        }

        const eventData = JSON.stringify({
            type: 'update',
            timestamp: Date.now()
        });

        this.outputChannel.appendLine(`Notifying ${this.sseClients.length} SSE clients of new analysis data`);

        this.sseClients.forEach((client, index) => {
            try {
                client.write(`data: ${eventData}\n\n`);
            } catch (error) {
                this.outputChannel.appendLine(`Error sending to client ${index}: ${error}`);
            }
        });
    }

    getUrl(): string {
        return `http://localhost:${this.port}`;
    }

    stop() {
        this.sseClients.forEach(client => {
            try {
                client.end();
            } catch (error) {
                // Ignore
            }
        });
        this.sseClients = [];

        if (this.server) {
            this.server.close();
            this.outputChannel.appendLine('Pragmite Dashboard stopped');
        }
    }

    private getDashboardHtml(): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pragmite Live Dashboard</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #000000;
            color: #ffffff;
            min-height: 100vh;
            padding: 20px;
            line-height: 1.6;
        }

        .container {
            max-width: 1600px;
            margin: 0 auto;
        }

        /* Glass Effect */
        .glass {
            background: rgba(20, 20, 20, 0.6);
            backdrop-filter: blur(20px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.8);
        }

        /* Header */
        .header {
            text-align: center;
            padding: 40px;
            border-radius: 16px;
            margin-bottom: 30px;
            background: linear-gradient(135deg, rgba(30, 30, 30, 0.8) 0%, rgba(15, 15, 15, 0.8) 100%);
            border: 1px solid rgba(255, 255, 255, 0.1);
            position: relative;
        }

        .theme-toggle-container {
            position: absolute;
            top: 20px;
            right: 20px;
        }

        .header h1 {
            font-size: 48px;
            margin-bottom: 10px;
            color: #fff;
            font-weight: 700;
        }

        .header .subtitle {
            font-size: 16px;
            opacity: 0.6;
            color: #aaa;
        }

        .live-indicator {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            background: rgba(0, 200, 100, 0.15);
            padding: 10px 20px;
            border-radius: 30px;
            margin-top: 20px;
            border: 1px solid rgba(0, 200, 100, 0.3);
        }

        .live-dot {
            width: 10px;
            height: 10px;
            background: #00c864;
            border-radius: 50%;
            animation: pulse 2s infinite;
            box-shadow: 0 0 10px #00c864;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.5; transform: scale(1.3); }
        }

        /* Stats Grid - Single Row Responsive */
        .stats-container {
            display: flex;
            flex-wrap: nowrap;
            gap: 16px;
            margin-bottom: 30px;
            overflow-x: auto;
            scrollbar-width: thin;
            scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
        }

        .stats-container::-webkit-scrollbar {
            height: 6px;
        }

        .stats-container::-webkit-scrollbar-track {
            background: transparent;
        }

        .stats-container::-webkit-scrollbar-thumb {
            background: rgba(255, 255, 255, 0.2);
            border-radius: 3px;
        }

        @media (max-width: 1400px) {
            .stats-container {
                justify-content: flex-start;
            }
        }

        @media (min-width: 1401px) {
            .stats-container {
                justify-content: space-between;
                flex-wrap: wrap;
            }
        }

        .stat-card {
            min-width: 180px;
            flex: 1 1 auto;
            padding: 24px;
            border-radius: 12px;
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
            background: rgba(20, 20, 20, 0.8);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 40px rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.2);
        }

        .stat-label {
            font-size: 11px;
            opacity: 0.5;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 12px;
            font-weight: 600;
        }

        .stat-value {
            font-size: 36px;
            font-weight: 700;
            color: #fff;
        }

        /* File Groups */
        .files-container {
            display: flex;
            flex-direction: column;
            gap: 24px;
        }

        .files-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            padding: 16px 20px;
            background: rgba(20, 20, 20, 0.8);
            border-radius: 12px;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        .files-header-title {
            font-size: 20px;
            font-weight: 700;
            color: #fff;
        }

        .files-header-actions {
            display: flex;
            gap: 12px;
        }

        .action-btn {
            padding: 8px 16px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 8px;
            color: #fff;
            font-size: 13px;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .action-btn:hover {
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.2);
            transform: translateY(-2px);
        }

        .file-group {
            border-radius: 12px;
            padding: 24px;
            transition: all 0.3s ease;
            background: rgba(20, 20, 20, 0.8);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        .file-group.collapsed .issues-list {
            display: none;
        }

        .file-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 16px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            cursor: pointer;
            user-select: none;
        }

        .file-header:hover {
            opacity: 0.8;
        }

        .file-name-container {
            flex: 1;
        }

        .file-name {
            font-size: 18px;
            font-weight: 600;
            color: #fff;
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 4px;
        }

        .file-path {
            font-size: 12px;
            color: #64748b;
            font-family: 'Courier New', monospace;
            opacity: 0.7;
        }

        .collapse-icon {
            font-size: 20px;
            transition: transform 0.3s ease;
            color: #64748b;
            margin-right: 8px;
        }

        .file-group.collapsed .collapse-icon {
            transform: rotate(-90deg);
        }

        .file-stats {
            display: flex;
            gap: 15px;
            font-size: 13px;
        }

        .file-stat {
            padding: 4px 12px;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 12px;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        /* Issues */
        .issues-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .issue-item {
            padding: 16px;
            border-radius: 10px;
            background: rgba(30, 30, 30, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.08);
            transition: all 0.3s ease;
            cursor: pointer;
        }

        .issue-item:hover {
            background: rgba(40, 40, 40, 0.8);
            border-color: rgba(255, 255, 255, 0.2);
            transform: translateX(4px);
        }

        .issue-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 8px;
        }

        .issue-type {
            font-family: 'Courier New', monospace;
            font-size: 13px;
            color: #aaa;
            font-weight: 600;
        }

        .severity-badge {
            padding: 4px 10px;
            border-radius: 8px;
            font-size: 10px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .severity-CRITICAL {
            background: rgba(255, 50, 50, 0.2);
            color: #ff5555;
            border: 1px solid rgba(255, 50, 50, 0.4);
        }

        .severity-MAJOR {
            background: rgba(255, 150, 0, 0.2);
            color: #ffa500;
            border: 1px solid rgba(255, 150, 0, 0.4);
        }

        .severity-MINOR {
            background: rgba(100, 150, 255, 0.2);
            color: #6495ff;
            border: 1px solid rgba(100, 150, 255, 0.4);
        }

        .issue-location {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 12px;
            opacity: 0.6;
            margin-bottom: 8px;
        }

        .line-badge {
            background: rgba(255, 255, 255, 0.1);
            padding: 2px 8px;
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 11px;
        }

        .issue-message {
            font-size: 14px;
            line-height: 1.5;
            opacity: 0.8;
            margin-bottom: 8px;
        }

        .solution-preview {
            font-size: 12px;
            color: #00c864;
            opacity: 0.8;
            font-style: italic;
        }

        /* Modal */
        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.9);
            backdrop-filter: blur(10px);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }

        .modal.active {
            display: flex;
        }

        .modal-content {
            background: #0a0a0a;
            border-radius: 16px;
            padding: 32px;
            max-width: 700px;
            width: 90%;
            border: 1px solid rgba(255, 255, 255, 0.2);
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.9);
            animation: slideUp 0.3s ease-out;
        }

        @keyframes slideUp {
            from { transform: translateY(50px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 24px;
            padding-bottom: 16px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }

        .modal-title {
            font-size: 18px;
            font-weight: 700;
            color: #fff;
            font-family: 'Courier New', monospace;
        }

        .modal-close {
            background: rgba(255, 50, 50, 0.2);
            border: 1px solid rgba(255, 50, 50, 0.4);
            color: #ff5555;
            border-radius: 50%;
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 18px;
            transition: all 0.3s ease;
        }

        .modal-close:hover {
            background: rgba(255, 50, 50, 0.3);
            transform: rotate(90deg);
        }

        .modal-section {
            margin-bottom: 20px;
        }

        .modal-section-title {
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 1px;
            opacity: 0.5;
            margin-bottom: 8px;
            font-weight: 600;
        }

        .modal-section-content {
            font-size: 14px;
            line-height: 1.7;
            padding: 12px;
            background: rgba(255, 255, 255, 0.03);
            border-radius: 8px;
            border: 1px solid rgba(255, 255, 255, 0.05);
        }

        .solution-box {
            background: rgba(0, 200, 100, 0.1);
            border: 1px solid rgba(0, 200, 100, 0.3);
            border-radius: 8px;
            padding: 16px;
            color: #00c864;
        }

        .vscode-link {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: rgba(100, 150, 255, 0.2);
            color: #6495ff;
            padding: 10px 16px;
            border-radius: 8px;
            text-decoration: none;
            border: 1px solid rgba(100, 150, 255, 0.3);
            font-size: 13px;
            font-weight: 600;
            transition: all 0.3s ease;
        }

        .vscode-link:hover {
            background: rgba(100, 150, 255, 0.3);
            transform: translateX(4px);
        }

        .no-data {
            text-align: center;
            padding: 100px 20px;
            opacity: 0.4;
        }

        .no-data-icon {
            font-size: 64px;
            margin-bottom: 20px;
        }

        .refresh-info {
            text-align: center;
            padding: 16px;
            border-radius: 12px;
            margin-top: 30px;
            font-size: 12px;
            opacity: 0.4;
            background: rgba(20, 20, 20, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.05);
        }

        /* Quality Score Details */
        .quality-details {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 16px;
            margin-bottom: 30px;
            padding: 24px;
            border-radius: 12px;
            background: rgba(20, 20, 20, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        .quality-metric {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px;
            background: rgba(30, 30, 30, 0.6);
            border-radius: 8px;
            border: 1px solid rgba(255, 255, 255, 0.08);
            transition: all 0.3s ease;
        }

        .quality-metric:hover {
            border-color: rgba(255, 255, 255, 0.2);
            transform: translateY(-2px);
        }

        .quality-metric-label {
            font-size: 13px;
            font-weight: 600;
            opacity: 0.7;
        }

        .quality-metric-value {
            font-size: 24px;
            font-weight: 700;
            color: #fff;
        }

        .quality-metric-description {
            font-size: 11px;
            opacity: 0.5;
            margin-top: 4px;
        }

        /* Suggestions Section */
        .suggestions-section {
            margin-bottom: 30px;
            padding: 24px;
            border-radius: 12px;
            background: rgba(0, 200, 100, 0.05);
            border: 1px solid rgba(0, 200, 100, 0.2);
        }

        .suggestions-title {
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 16px;
            color: #00c864;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .suggestion-item {
            padding: 16px;
            margin-bottom: 12px;
            background: rgba(0, 200, 100, 0.1);
            border: 1px solid rgba(0, 200, 100, 0.2);
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s ease;
        }

        .suggestion-item:hover {
            background: rgba(0, 200, 100, 0.15);
            border-color: rgba(0, 200, 100, 0.4);
            transform: translateX(4px);
        }

        .suggestion-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
        }

        .suggestion-title-text {
            font-size: 15px;
            font-weight: 600;
            color: #00c864;
        }

        .difficulty-badge {
            padding: 4px 10px;
            border-radius: 8px;
            font-size: 10px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .difficulty-EASY {
            background: rgba(0, 200, 100, 0.2);
            color: #00c864;
            border: 1px solid rgba(0, 200, 100, 0.4);
        }

        .difficulty-MEDIUM {
            background: rgba(255, 150, 0, 0.2);
            color: #ffa500;
            border: 1px solid rgba(255, 150, 0, 0.4);
        }

        .difficulty-HARD {
            background: rgba(255, 50, 50, 0.2);
            color: #ff5555;
            border: 1px solid rgba(255, 50, 50, 0.4);
        }

        .suggestion-description {
            font-size: 13px;
            line-height: 1.5;
            opacity: 0.8;
            color: #fff;
        }

        /* Theme Toggle Button */
        .theme-toggle {
            position: relative;
            display: inline-block;
            width: 104px;
            height: 64px;
        }

        .theme-toggle-btn {
            position: relative;
            width: 100%;
            height: 100%;
            border-radius: 32px;
            border: 2px solid rgba(51, 65, 85, 0.6);
            background: radial-gradient(ellipse at top left, #1e293b 0%, #0f172a 40%, #020617 100%);
            cursor: pointer;
            transition: all 0.3s ease;
            box-shadow:
                inset 5px 5px 12px rgba(0, 0, 0, 0.9),
                inset -5px -5px 12px rgba(71, 85, 105, 0.4),
                0 8px 16px rgba(0, 0, 0, 0.4);
        }

        .theme-toggle-btn:hover {
            transform: scale(1.05);
        }

        .theme-toggle-track {
            position: absolute;
            inset: 6px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 12px;
            pointer-events: none;
        }

        .theme-icon {
            width: 20px;
            height: 20px;
            opacity: 0.5;
            transition: opacity 0.3s ease;
        }

        .theme-toggle.light .theme-icon.sun {
            opacity: 1;
            color: #f59e0b;
        }

        .theme-toggle.dark .theme-icon.moon {
            opacity: 1;
            color: #fbbf24;
        }

        .theme-toggle-thumb {
            position: absolute;
            top: 10px;
            left: 6px;
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: linear-gradient(145deg, #64748b 0%, #475569 50%, #334155 100%);
            transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            box-shadow:
                inset 2px 2px 4px rgba(100, 116, 139, 0.4),
                inset -2px -2px 4px rgba(0, 0, 0, 0.8),
                0 8px 32px rgba(0, 0, 0, 0.6);
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }

        .theme-toggle.light .theme-toggle-thumb {
            transform: translateX(0);
        }

        .theme-toggle.dark .theme-toggle-thumb {
            transform: translateX(46px);
        }

        .theme-toggle-icon {
            width: 20px;
            height: 20px;
            transition: all 0.3s ease;
        }

        .theme-toggle.light .theme-toggle-icon.sun {
            display: block;
            color: #f59e0b;
        }

        .theme-toggle.light .theme-toggle-icon.moon {
            display: none;
        }

        .theme-toggle.dark .theme-toggle-icon.sun {
            display: none;
        }

        .theme-toggle.dark .theme-toggle-icon.moon {
            display: block;
            color: #fbbf24;
        }

        /* Light Theme Overrides */
        body.light-theme {
            background: #f1f5f9;
            color: #1e293b;
        }

        body.light-theme .glass {
            background: rgba(255, 255, 255, 0.8);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .header {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(241, 245, 249, 0.9) 100%);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .header h1 {
            color: #1e293b;
        }

        body.light-theme .header .subtitle {
            color: #64748b;
        }

        body.light-theme .stat-card {
            background: rgba(255, 255, 255, 0.9);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .stat-label {
            color: #64748b;
        }

        body.light-theme .stat-value {
            color: #1e293b;
        }

        body.light-theme .file-group {
            background: rgba(255, 255, 255, 0.9);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .file-name {
            color: #1e293b;
        }

        body.light-theme .issue-item {
            background: rgba(241, 245, 249, 0.8);
            border: 1px solid rgba(0, 0, 0, 0.08);
        }

        body.light-theme .issue-item:hover {
            background: rgba(226, 232, 240, 0.9);
        }

        body.light-theme .issue-type {
            color: #64748b;
        }

        body.light-theme .issue-message {
            color: #475569;
        }

        body.light-theme .quality-details {
            background: rgba(255, 255, 255, 0.8);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .quality-metric {
            background: rgba(241, 245, 249, 0.8);
            border: 1px solid rgba(0, 0, 0, 0.08);
        }

        body.light-theme .quality-metric-label {
            color: #64748b;
        }

        body.light-theme .quality-metric-value {
            color: #1e293b;
        }

        body.light-theme .theme-toggle-btn {
            background: radial-gradient(ellipse at top left, #ffffff 0%, #f1f5f9 40%, #cbd5e1 100%);
            border: 2px solid rgba(203, 213, 225, 0.6);
            box-shadow:
                inset 5px 5px 12px rgba(148, 163, 184, 0.5),
                inset -5px -5px 12px rgba(255, 255, 255, 1),
                0 8px 16px rgba(0, 0, 0, 0.08);
        }

        body.light-theme .theme-toggle-thumb {
            background: linear-gradient(145deg, #ffffff 0%, #fefefe 50%, #f8fafc 100%);
            box-shadow:
                inset 2px 2px 4px rgba(203, 213, 225, 0.3),
                inset -2px -2px 4px rgba(255, 255, 255, 1),
                0 8px 32px rgba(0, 0, 0, 0.18);
        }

        body.light-theme .files-header {
            background: rgba(255, 255, 255, 0.9);
            border: 1px solid rgba(0, 0, 0, 0.1);
        }

        body.light-theme .files-header-title {
            color: #1e293b;
        }

        body.light-theme .action-btn {
            background: rgba(0, 0, 0, 0.05);
            border: 1px solid rgba(0, 0, 0, 0.1);
            color: #1e293b;
        }

        body.light-theme .action-btn:hover {
            background: rgba(0, 0, 0, 0.1);
            border-color: rgba(0, 0, 0, 0.2);
        }

        body.light-theme .file-path {
            color: #64748b;
        }

        body.light-theme .collapse-icon {
            color: #64748b;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="theme-toggle-container">
                <div class="theme-toggle dark" id="themeToggle" onclick="toggleTheme()">
                    <button class="theme-toggle-btn" aria-label="Toggle theme">
                        <div class="theme-toggle-track">
                            <svg class="theme-icon sun" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="5"></circle>
                                <line x1="12" y1="1" x2="12" y2="3"></line>
                                <line x1="12" y1="21" x2="12" y2="23"></line>
                                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                                <line x1="1" y1="12" x2="3" y2="12"></line>
                                <line x1="21" y1="12" x2="23" y2="12"></line>
                                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                            </svg>
                            <svg class="theme-icon moon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                            </svg>
                        </div>
                        <div class="theme-toggle-thumb">
                            <svg class="theme-toggle-icon sun" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="5"></circle>
                                <line x1="12" y1="1" x2="12" y2="3"></line>
                                <line x1="12" y1="21" x2="12" y2="23"></line>
                                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                                <line x1="1" y1="12" x2="3" y2="12"></line>
                                <line x1="21" y1="12" x2="23" y2="12"></line>
                                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                            </svg>
                            <svg class="theme-toggle-icon moon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                            </svg>
                        </div>
                    </button>
                </div>
            </div>
            <h1>🔬 Pragmite Live Dashboard</h1>
            <p class="subtitle">Real-Time Java Code Quality Monitor</p>
            <div class="live-indicator">
                <div class="live-dot"></div>
                <span>LIVE</span>
            </div>
        </div>

        <div id="content">
            <div class="no-data">
                <div class="no-data-icon">⏳</div>
                <h2>Loading Data...</h2>
                <p>Waiting for analysis results. Analyze a Java project in VSCode.</p>
            </div>
        </div>

        <div class="refresh-info">
            🔄 Auto-refresh: Every 3 seconds | Port: ${this.port}
        </div>
    </div>

    <div id="modal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <div class="modal-title" id="modal-title">Issue Details</div>
                <div class="modal-close" onclick="closeModal()">✕</div>
            </div>
            <div id="modal-body"></div>
        </div>
    </div>

    <script>
        let currentData = null;
        let eventSource = null;

        // Theme Management
        function toggleTheme() {
            const body = document.body;
            const themeToggle = document.getElementById('themeToggle');
            const isDark = themeToggle.classList.contains('dark');

            if (isDark) {
                body.classList.add('light-theme');
                themeToggle.classList.remove('dark');
                themeToggle.classList.add('light');
                localStorage.setItem('pragmite-theme', 'light');
            } else {
                body.classList.remove('light-theme');
                themeToggle.classList.remove('light');
                themeToggle.classList.add('dark');
                localStorage.setItem('pragmite-theme', 'dark');
            }
        }

        // Load saved theme on page load
        function initTheme() {
            const savedTheme = localStorage.getItem('pragmite-theme') || 'dark';
            const body = document.body;
            const themeToggle = document.getElementById('themeToggle');

            if (savedTheme === 'light') {
                body.classList.add('light-theme');
                themeToggle.classList.remove('dark');
                themeToggle.classList.add('light');
            } else {
                body.classList.remove('light-theme');
                themeToggle.classList.remove('light');
                themeToggle.classList.add('dark');
            }
        }

        // Initialize theme on page load
        initTheme();

        function setupSSE() {
            eventSource = new EventSource('/api/events');

            eventSource.onmessage = (event) => {
                const data = JSON.parse(event.data);

                if (data.type === 'connected') {
                    console.log('✅ SSE connected - Instant updates enabled!');
                } else if (data.type === 'update') {
                    console.log('⚡ New analysis data received! Updating instantly...');
                    loadData();
                }
            };

            eventSource.onerror = (error) => {
                console.error('SSE connection error:', error);
            };
        }

        async function loadData() {
            try {
                const response = await fetch('/api/analysis');
                const data = await response.json();

                if (data && data.totalFiles) {
                    currentData = data;
                    renderDashboard(data);
                }
            } catch (error) {
                console.error('Error loading data:', error);
            }
        }

        let smellsByFile = {};
        let fileGroupsArray = [];

        function renderDashboard(data) {
            const smellsBySeverity = {
                CRITICAL: (data.codeSmells || []).filter(s => s.severity === 'CRITICAL').length,
                MAJOR: (data.codeSmells || []).filter(s => s.severity === 'MAJOR').length,
                MINOR: (data.codeSmells || []).filter(s => s.severity === 'MINOR').length
            };

            smellsByFile = {};
            (data.codeSmells || []).forEach(smell => {
                const fileName = smell.filePath.split(/[\\\\/]/).pop();
                if (!smellsByFile[smell.filePath]) {
                    smellsByFile[smell.filePath] = {
                        fileName: fileName,
                        fullPath: smell.filePath,
                        smells: []
                    };
                }
                smellsByFile[smell.filePath].smells.push(smell);
            });

            // Convert to array for indexed access
            fileGroupsArray = Object.values(smellsByFile);

            let html = \`
                <div class="stats-container">
                    <div class="stat-card glass">
                        <div class="stat-label">Quality Score</div>
                        <div class="stat-value">\${data.qualityScore?.overallScore || 0}/100</div>
                        <div style="margin-top: 8px; font-size: 16px; opacity: 0.6;">Grade \${data.qualityScore?.grade || 'N/A'}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Files</div>
                        <div class="stat-value">\${data.totalFiles || 0}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Lines of Code</div>
                        <div class="stat-value">\${(data.totalLines || 0).toLocaleString()}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Total Issues</div>
                        <div class="stat-value">\${data.codeSmells?.length || 0}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Critical</div>
                        <div class="stat-value" style="color: #ff5555;">\${smellsBySeverity.CRITICAL || 0}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Major</div>
                        <div class="stat-value" style="color: #ffa500;">\${smellsBySeverity.MAJOR || 0}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Minor</div>
                        <div class="stat-value" style="color: #6495ff;">\${smellsBySeverity.MINOR || 0}</div>
                    </div>

                    <div class="stat-card glass">
                        <div class="stat-label">Analysis Time</div>
                        <div class="stat-value" style="font-size: 28px;">\${data.analysisDurationMs || 0}ms</div>
                    </div>
                </div>

                \${data.qualityScore ? \`
                <div class="quality-details glass">
                    <div class="quality-metric">
                        <div>
                            <div class="quality-metric-label">🔄 DRY Score</div>
                            <div class="quality-metric-description">Don't Repeat Yourself</div>
                        </div>
                        <div class="quality-metric-value">\${Math.round(data.qualityScore.dryScore || 0)}</div>
                    </div>
                    <div class="quality-metric">
                        <div>
                            <div class="quality-metric-label">🔗 Orthogonality</div>
                            <div class="quality-metric-description">Low Coupling</div>
                        </div>
                        <div class="quality-metric-value">\${Math.round(data.qualityScore.orthogonalityScore || 0)}</div>
                    </div>
                    <div class="quality-metric">
                        <div>
                            <div class="quality-metric-label">✅ Correctness</div>
                            <div class="quality-metric-description">Bug Prevention</div>
                        </div>
                        <div class="quality-metric-value">\${Math.round(data.qualityScore.correctnessScore || 0)}</div>
                    </div>
                    <div class="quality-metric">
                        <div>
                            <div class="quality-metric-label">⚡ Performance</div>
                            <div class="quality-metric-description">Time/Space Efficiency</div>
                        </div>
                        <div class="quality-metric-value">\${Math.round(data.qualityScore.performanceScore || 0)}</div>
                    </div>
                </div>
                \` : ''}

                \${(data.suggestions && data.suggestions.length > 0) ? \`
                <div class="suggestions-section glass">
                    <div class="suggestions-title">
                        <span>💡</span>
                        <span>Improvement Suggestions</span>
                    </div>
                    \${data.suggestions.map((suggestion, idx) => \`
                        <div class="suggestion-item" onclick="showSuggestionModal(\${idx})">
                            <div class="suggestion-header">
                                <div class="suggestion-title-text">\${suggestion.title || 'Refactoring Suggestion'}</div>
                                <span class="difficulty-badge difficulty-\${suggestion.difficulty || 'MEDIUM'}">\${suggestion.difficulty || 'MEDIUM'}</span>
                            </div>
                            <div class="suggestion-description">\${suggestion.description || 'Click to view details'}</div>
                        </div>
                    \`).join('')}
                </div>
                \` : ''}

                \${Object.keys(smellsByFile).length > 0 ? \`
                <div class="files-header glass">
                    <div class="files-header-title">📁 Issues by File (\${Object.keys(smellsByFile).length})</div>
                    <div class="files-header-actions">
                        <button class="action-btn" onclick="expandAllFiles()">▼ Expand All</button>
                        <button class="action-btn" onclick="collapseAllFiles()">▶ Collapse All</button>
                    </div>
                </div>
                \` : ''}

                <div class="files-container">
            \`;

            Object.values(smellsByFile).forEach((fileGroup, fileIndex) => {
                const criticalCount = fileGroup.smells.filter(s => s.severity === 'CRITICAL').length;
                const majorCount = fileGroup.smells.filter(s => s.severity === 'MAJOR').length;
                const minorCount = fileGroup.smells.filter(s => s.severity === 'MINOR').length;

                html += \`
                    <div class="file-group glass" id="file-group-\${fileIndex}">
                        <div class="file-header" onclick="toggleFileGroup(\${fileIndex})">
                            <div class="file-name-container">
                                <div class="file-name">
                                    <span class="collapse-icon">▼</span>
                                    <span>📄</span>
                                    <span>\${fileGroup.fileName}</span>
                                </div>
                                <div class="file-path">\${fileGroup.fullPath}</div>
                            </div>
                            <div class="file-stats">
                                \${criticalCount > 0 ? \`<div class="file-stat" style="border-color: rgba(255, 50, 50, 0.4);"><span style="color: #ff5555;">🔥 \${criticalCount}</span></div>\` : ''}
                                \${majorCount > 0 ? \`<div class="file-stat" style="border-color: rgba(255, 150, 0, 0.4);"><span style="color: #ffa500;">⚡ \${majorCount}</span></div>\` : ''}
                                \${minorCount > 0 ? \`<div class="file-stat" style="border-color: rgba(100, 150, 255, 0.4);"><span style="color: #6495ff;">ℹ️ \${minorCount}</span></div>\` : ''}
                            </div>
                        </div>
                        <div class="issues-list">
                \`;

                fileGroup.smells.forEach((smell, index) => {
                    const solutionPreview = smell.suggestion ?
                        (smell.suggestion.length > 50 ? smell.suggestion.substring(0, 50) + '...' : smell.suggestion) :
                        'Click for solution';

                    html += \`
                        <div class="issue-item" data-file-index="\${fileIndex}" data-smell-index="\${index}" onclick="showModalByFileIndex(\${fileIndex}, \${index})">
                            <div class="issue-header">
                                <div class="issue-type">\${smell.type}</div>
                                <span class="severity-badge severity-\${smell.severity}">\${smell.severity}</span>
                            </div>
                            <div class="issue-location">
                                <span class="line-badge">Line \${smell.startLine}\${smell.endLine && smell.endLine > 0 && smell.endLine !== smell.startLine ? '-' + smell.endLine : ''}</span>
                                \${smell.affectedElement ? \`<span>→ <code>\${smell.affectedElement}()</code></span>\` : ''}
                            </div>
                            <div class="issue-message">\${smell.description || smell.message || 'No description'}</div>
                            <div class="solution-preview">💡 \${solutionPreview}</div>
                        </div>
                    \`;
                });

                html += \`
                        </div>
                    </div>
                \`;
            });

            html += '</div>';
            document.getElementById('content').innerHTML = html;
        }

        function showModalByFileIndex(fileIndex, smellIndex) {
            if (!fileGroupsArray[fileIndex] || !fileGroupsArray[fileIndex].smells[smellIndex]) {
                console.error('Smell not found:', fileIndex, smellIndex);
                return;
            }

            const smell = fileGroupsArray[fileIndex].smells[smellIndex];
            const modal = document.getElementById('modal');
            const modalTitle = document.getElementById('modal-title');
            const modalBody = document.getElementById('modal-body');

            modalTitle.textContent = smell.type || 'Code Smell';

            let bodyHtml = \`
                <div class="modal-section">
                    <div class="modal-section-title">📍 Location</div>
                    <div class="modal-section-content">
                        <strong>File:</strong> \${(smell.filePath || '').split(/[\\\\/]/).pop()}<br>
                        <strong>Path:</strong> <code style="font-size: 11px; color: #64748b;">\${smell.filePath || 'Unknown'}</code><br>
                        <strong>Line:</strong> \${smell.startLine || 0}\${smell.endLine && smell.endLine > 0 && smell.endLine !== smell.startLine ? ' - ' + smell.endLine : ''}<br>
                        \${smell.affectedElement ? \`<strong>Method:</strong> <code>\${smell.affectedElement}()</code><br>\` : ''}
                        \${smell.className ? \`<strong>Class:</strong> <code>\${smell.className}</code>\` : ''}
                    </div>
                </div>

                <div class="modal-section">
                    <div class="modal-section-title">⚠️ Issue</div>
                    <div class="modal-section-content">
                        <span class="severity-badge severity-\${smell.severity || 'MINOR'}">\${smell.severity || 'MINOR'}</span>
                        <div style="margin-top: 12px;">\${smell.description || smell.message || 'No description available'}</div>
                    </div>
                </div>

                \${smell.suggestion ? \`
                <div class="modal-section">
                    <div class="modal-section-title">💡 Solution</div>
                    <div class="solution-box">
                        \${smell.suggestion}
                    </div>
                </div>
                \` : ''}

                <div class="modal-section">
                    <a href="vscode://file/\${smell.filePath}:\${smell.startLine || 1}" class="vscode-link">
                        <span>📝</span>
                        <span>Open in VSCode (Line \${smell.startLine || 1})</span>
                    </a>
                </div>
            \`;

            modalBody.innerHTML = bodyHtml;
            modal.classList.add('active');
        }

        // Legacy function for backward compatibility
        function showModalByIndex(filePath, index) {
            if (!smellsByFile[filePath] || !smellsByFile[filePath].smells[index]) {
                console.error('Smell not found:', filePath, index);
                return;
            }

            const smell = smellsByFile[filePath].smells[index];
            const modal = document.getElementById('modal');
            const modalTitle = document.getElementById('modal-title');
            const modalBody = document.getElementById('modal-body');

            modalTitle.textContent = smell.type || 'Code Smell';

            let bodyHtml = \`
                <div class="modal-section">
                    <div class="modal-section-title">📍 Location</div>
                    <div class="modal-section-content">
                        <strong>File:</strong> \${(smell.filePath || '').split(/[\\\\/]/).pop()}<br>
                        <strong>Line:</strong> \${smell.startLine || 0}\${smell.endLine && smell.endLine > 0 && smell.endLine !== smell.startLine ? ' - ' + smell.endLine : ''}<br>
                        \${smell.affectedElement ? \`<strong>Method:</strong> <code>\${smell.affectedElement}()</code><br>\` : ''}
                        \${smell.className ? \`<strong>Class:</strong> <code>\${smell.className}</code>\` : ''}
                    </div>
                </div>

                <div class="modal-section">
                    <div class="modal-section-title">⚠️ Issue</div>
                    <div class="modal-section-content">
                        <span class="severity-badge severity-\${smell.severity || 'MINOR'}">\${smell.severity || 'MINOR'}</span>
                        <div style="margin-top: 12px;">\${smell.description || smell.message || 'No description available'}</div>
                    </div>
                </div>

                \${smell.suggestion ? \`
                <div class="modal-section">
                    <div class="modal-section-title">💡 Solution</div>
                    <div class="solution-box">
                        \${smell.suggestion}
                    </div>
                </div>
                \` : ''}

                <div class="modal-section">
                    <a href="vscode://file/\${smell.filePath}:\${smell.startLine || 1}" class="vscode-link">
                        <span>📝</span>
                        <span>Open in VSCode (Line \${smell.startLine || 1})</span>
                    </a>
                </div>
            \`;

            modalBody.innerHTML = bodyHtml;
            modal.classList.add('active');
        }

        function closeModal() {
            document.getElementById('modal').classList.remove('active');
        }

        document.getElementById('modal').addEventListener('click', function(e) {
            if (e.target === this) {
                closeModal();
            }
        });

        function toggleFileGroup(index) {
            const fileGroup = document.getElementById(\`file-group-\${index}\`);
            if (fileGroup) {
                fileGroup.classList.toggle('collapsed');
            }
        }

        function expandAllFiles() {
            const fileGroups = document.querySelectorAll('.file-group');
            fileGroups.forEach(group => {
                group.classList.remove('collapsed');
            });
        }

        function collapseAllFiles() {
            const fileGroups = document.querySelectorAll('.file-group');
            fileGroups.forEach(group => {
                group.classList.add('collapsed');
            });
        }

        function showSuggestionModal(index) {
            if (!currentData || !currentData.suggestions || !currentData.suggestions[index]) {
                console.error('Suggestion not found:', index);
                return;
            }

            const suggestion = currentData.suggestions[index];
            const modal = document.getElementById('modal');
            const modalTitle = document.getElementById('modal-title');
            const modalBody = document.getElementById('modal-body');

            modalTitle.textContent = suggestion.title || 'Refactoring Suggestion';

            let stepsHtml = '';
            if (suggestion.steps && suggestion.steps.length > 0) {
                stepsHtml = \`
                    <div class="modal-section">
                        <div class="modal-section-title">📋 Steps</div>
                        <div class="modal-section-content">
                            <ol style="margin-left: 20px;">
                                \${suggestion.steps.map(step => \`<li style="margin-bottom: 8px;">\${step}</li>\`).join('')}
                            </ol>
                        </div>
                    </div>
                \`;
            }

            let beforeAfterHtml = '';
            if (suggestion.beforeCode || suggestion.afterCode) {
                beforeAfterHtml = \`
                    <div class="modal-section">
                        <div class="modal-section-title">🔍 Before & After</div>
                        \${suggestion.beforeCode ? \`
                            <div class="modal-section-content" style="margin-bottom: 12px;">
                                <strong style="color: #ff5555;">❌ Before:</strong>
                                <pre style="margin-top: 8px; padding: 12px; background: rgba(255, 50, 50, 0.1); border-left: 3px solid #ff5555; overflow-x: auto;"><code>\${suggestion.beforeCode}</code></pre>
                            </div>
                        \` : ''}
                        \${suggestion.afterCode ? \`
                            <div class="modal-section-content">
                                <strong style="color: #00c864;">✅ After:</strong>
                                <pre style="margin-top: 8px; padding: 12px; background: rgba(0, 200, 100, 0.1); border-left: 3px solid #00c864; overflow-x: auto;"><code>\${suggestion.afterCode}</code></pre>
                            </div>
                        \` : ''}
                    </div>
                \`;
            }

            let bodyHtml = \`
                <div class="modal-section">
                    <div class="modal-section-title">📝 Description</div>
                    <div class="modal-section-content">
                        <span class="difficulty-badge difficulty-\${suggestion.difficulty || 'MEDIUM'}">\${suggestion.difficulty || 'MEDIUM'}</span>
                        <div style="margin-top: 12px;">\${suggestion.description || 'No description available'}</div>
                    </div>
                </div>

                \${stepsHtml}
                \${beforeAfterHtml}

                \${suggestion.autoFixAvailable ? \`
                    <div class="modal-section">
                        <div style="padding: 12px; background: rgba(0, 200, 100, 0.1); border: 1px solid rgba(0, 200, 100, 0.3); border-radius: 8px; color: #00c864;">
                            <strong>✨ Auto-fix available!</strong> This refactoring can be applied automatically.
                        </div>
                    </div>
                \` : ''}
            \`;

            modalBody.innerHTML = bodyHtml;
            modal.classList.add('active');
        }

        setupSSE();
        loadData();
        setInterval(loadData, 3000);
    </script>
</body>
</html>`;
    }
}
