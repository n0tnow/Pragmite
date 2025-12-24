import { AnalysisResult } from './models';

export function generateReportHtml(result: AnalysisResult): string {
    const smellsByType = result.codeSmells.reduce((acc, smell) => {
        acc[smell.type] = (acc[smell.type] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    const topSmellTypes = Object.entries(smellsByType)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);

    const scoreColor = getScoreColor(result.qualityScore.grade);

    return `<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <title>Pragmite Kalite Raporu</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            padding: 30px;
            background: var(--vscode-editor-background);
            color: var(--vscode-editor-foreground);
            line-height: 1.6;
        }
        .header {
            background: var(--vscode-sideBar-background);
            border-left: 5px solid ${scoreColor};
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }
        .score {
            font-size: 48px;
            font-weight: bold;
            color: ${scoreColor};
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid var(--vscode-panel-border);
        }
        th {
            background: var(--vscode-sideBar-background);
            font-weight: 600;
        }
        .severity-CRITICAL { background: #f44336; color: white; padding: 4px 8px; border-radius: 4px; }
        .severity-MAJOR { background: #ff9800; color: white; padding: 4px 8px; border-radius: 4px; }
        .severity-MINOR { background: #2196F3; color: white; padding: 4px 8px; border-radius: 4px; }
        code { background: var(--vscode-textCodeBlock-background); padding: 2px 6px; border-radius: 3px; }
        .suggestion { color: #4CAF50; font-style: italic; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔬 Pragmite Kod Kalite Raporu</h1>
        <p>${new Date(result.analyzedAt).toLocaleString('tr-TR')} | ${result.analysisDurationMs}ms</p>
        <div class="score">${result.qualityScore.overallScore}/100 (${result.qualityScore.grade})</div>
    </div>

    <h2>📊 Özet</h2>
    <table>
        <tr><td><strong>Dosya Sayısı</strong></td><td>${result.totalFiles}</td></tr>
        <tr><td><strong>Toplam Satır</strong></td><td>${result.totalLines.toLocaleString('tr-TR')}</td></tr>
        <tr><td><strong>Code Smell</strong></td><td>${result.codeSmells.length}</td></tr>
    </table>

    <h2>📋 En Sık Code Smell'ler</h2>
    <table>
        <tr><th>Tip</th><th>Adet</th></tr>
        ${topSmellTypes.map(([type, count]) => `<tr><td><code>${type}</code></td><td><strong>${count}</strong></td></tr>`).join('')}
    </table>

    <h2>🔍 Detaylı Code Smell Listesi</h2>
    <table>
        <tr>
            <th>Tip</th>
            <th>Severity</th>
            <th>Dosya</th>
            <th>Satır</th>
            <th>Açıklama</th>
            <th>💡 Çözüm</th>
        </tr>
        ${result.codeSmells.slice(0, 50).map(smell => `
            <tr>
                <td><code>${smell.type}</code></td>
                <td><span class="severity-${smell.severity}">${smell.severity}</span></td>
                <td>${smell.filePath.split(/[\\/]/).pop()}</td>
                <td><strong>${smell.startLine}</strong></td>
                <td>${smell.message || '-'}</td>
                <td class="suggestion">${smell.suggestion || 'Refactor edilmeli'}</td>
            </tr>
        `).join('')}
    </table>
</body>
</html>`;
}

function getScoreColor(grade: string): string {
    switch (grade) {
        case 'A': return '#4CAF50';
        case 'B': return '#8BC34A';
        case 'C': return '#FFC107';
        case 'D': return '#FF9800';
        case 'F': return '#f44336';
        default: return '#9E9E9E';
    }
}
