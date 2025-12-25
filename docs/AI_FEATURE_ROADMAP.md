# 🚀 AI Features Implementation Roadmap

**Created:** December 25, 2025
**Status:** In Progress

---

## 📋 Implementation Plan (Sıralı)

### ✅ Phase 0: Core AI Infrastructure (COMPLETED)
- [x] AnalysisEngine - Root cause, impact, recommendations
- [x] PromptGenerator - 28 code smell templates
- [x] ContextExtractor - Smart code snippet extraction
- [x] AIAnalysisResult - Rich result model
- [x] CLI Integration - `--generate-ai-prompts` flag
- [x] JSON output for AI analysis

---

### 🔄 Phase 1: HTML Report AI Section (IN PROGRESS - 2-3 hours)
**Priority:** ⭐⭐⭐⭐⭐ (Highest - En kolay ve immediate value)

**Goal:** HTML report'a AI analysis section ekle

**Features:**
- [ ] Her code smell için AI analysis card
- [ ] Root cause, impact, recommendation gösterimi
- [ ] AI prompt gösterimi (copy button ile)
- [ ] Before/After code comparison (statik)
- [ ] Metrics comparison table
- [ ] "Why This is Better" section
- [ ] Responsive design
- [ ] Print-friendly CSS

**Files to Modify:**
- `src/main/java/com/pragmite/report/HtmlReportGenerator.java`
- HTML template içine AI section inject et

**Acceptance Criteria:**
- ✓ AI analysis her code smell altında görünür
- ✓ Prompt kopyalanabilir (clipboard)
- ✓ Visual olarak attractive
- ✓ Mobile-responsive

---

### 🔄 Phase 2: Claude API Integration (8-10 hours)
**Priority:** ⭐⭐⭐⭐⭐

**Goal:** Claude API ile otomatik refactored code generation

**Features:**
- [ ] Claude API client implementation
- [ ] API key configuration (.env veya config file)
- [ ] Automatic refactored code generation
- [ ] Error handling and retry logic
- [ ] Rate limiting support
- [ ] Cache refactored results (cost optimization)

**Implementation Details:**

#### 2.1. API Key Configuration
```yaml
# .pragmite.yaml
ai:
  provider: "claude"  # claude, gpt-4, gemini
  apiKey: "${CLAUDE_API_KEY}"  # Environment variable
  model: "claude-sonnet-4-5"
  enabled: true
  cacheResults: true
```

#### 2.2. Claude API Client
```java
// src/main/java/com/pragmite/ai/ClaudeApiClient.java
public class ClaudeApiClient {
    private final String apiKey;
    private final String model;

    public RefactoredCode generateRefactoring(String prompt, String originalCode) {
        // Call Claude API
        // Parse response
        // Return refactored code with explanation
    }
}
```

#### 2.3. Refactored Code Model
```java
public class RefactoredCode {
    private String code;              // Refactored code
    private String explanation;       // Why it's better
    private List<String> changes;     // List of changes made
    private Map<String, Metric> beforeMetrics;
    private Map<String, Metric> afterMetrics;
}
```

**CLI Integration:**
```bash
# Generate AI prompts + auto-refactor with Claude
java -jar pragmite-core-1.4.0.jar /path/to/project \
  --generate-ai-prompts \
  --auto-refactor \
  --ai-provider claude
```

**Files to Create:**
- `src/main/java/com/pragmite/ai/ClaudeApiClient.java`
- `src/main/java/com/pragmite/ai/RefactoredCode.java`
- `src/main/java/com/pragmite/ai/ApiConfig.java`

**Acceptance Criteria:**
- ✓ API key yapılandırılabilir (.env, config, CLI arg)
- ✓ Refactored code otomatik generate edilir
- ✓ Error handling robust
- ✓ Results cache'lenir (maliyet optimizasyonu)

---

### 🔄 Phase 3: Online Code Editor/Playground (15-20 hours)
**Priority:** ⭐⭐⭐⭐

**Goal:** Interactive before/after comparison with code execution

**Features:**
- [ ] Monaco Editor integration (VSCode editor)
- [ ] Before/After side-by-side view
- [ ] Syntax highlighting
- [ ] Diff view (highlighting changes)
- [ ] Code execution (compile + run)
- [ ] Performance measurement
- [ ] Test execution
- [ ] Metrics comparison visualization

**Sub-tasks:**

#### 3.1. Monaco Editor Integration
```html
<!-- HTML Report içinde -->
<div class="code-playground">
  <div class="editor-container">
    <div id="editor-before" class="editor"></div>
    <div id="editor-after" class="editor"></div>
  </div>
  <div class="controls">
    <button onclick="runCode('before')">▶ Run Original</button>
    <button onclick="runCode('after')">▶ Run Refactored</button>
    <button onclick="showDiff()">📊 Show Diff</button>
  </div>
  <div class="execution-results">
    <!-- Runtime, memory, output comparison -->
  </div>
</div>

<script src="monaco-editor/min/vs/loader.js"></script>
<script>
  // Initialize Monaco editors
  // Show before/after code
  // Enable diff mode
</script>
```

#### 3.2. Code Execution Engine
**Options:**

**Option A: JDoodle API (Recommended)**
- Pros: Simple, reliable, supports Java
- Cons: Requires internet, API key, rate limits

**Option B: Local Docker Container**
- Pros: No external dependency, unlimited runs
- Cons: Requires Docker, more complex setup

**Decision:** Use JDoodle for MVP, add local execution later

```java
// src/main/java/com/pragmite/ai/CodeExecutor.java
public class CodeExecutor {
    public ExecutionResult executeCode(String code) {
        // Compile Java code
        // Run with timeout
        // Measure execution time
        // Capture output
        return new ExecutionResult(output, executionTime, memoryUsed);
    }
}
```

#### 3.3. Runtime Analysis
```java
public class RuntimeAnalyzer {
    public ComparisonResult compare(String originalCode, String refactoredCode) {
        ExecutionResult before = executor.execute(originalCode);
        ExecutionResult after = executor.execute(refactoredCode);

        return new ComparisonResult(
            before.executionTime,
            after.executionTime,
            calculateImprovement()
        );
    }
}
```

#### 3.4. Visualization
```html
<div class="comparison-metrics">
  <table>
    <tr>
      <th>Metric</th>
      <th>Before</th>
      <th>After</th>
      <th>Improvement</th>
    </tr>
    <tr>
      <td>Execution Time</td>
      <td>142ms</td>
      <td>38ms</td>
      <td class="positive">-73% ⬇️</td>
    </tr>
    <tr>
      <td>Memory Used</td>
      <td>2.4 MB</td>
      <td>1.1 MB</td>
      <td class="positive">-54% ⬇️</td>
    </tr>
    <tr>
      <td>Complexity</td>
      <td>15</td>
      <td>4</td>
      <td class="positive">-67% ⬇️</td>
    </tr>
  </table>
</div>
```

**Files to Create:**
- `src/main/java/com/pragmite/ai/CodeExecutor.java`
- `src/main/java/com/pragmite/ai/RuntimeAnalyzer.java`
- `src/main/resources/templates/code-playground.html`

**Acceptance Criteria:**
- ✓ Monaco editor shows before/after code
- ✓ Code can be compiled and run
- ✓ Performance metrics displayed
- ✓ Diff view highlights changes
- ✓ "Why it's better" explanation shown

---

### 🔄 Phase 4: VSCode Extension AI Features (10-15 hours)
**Priority:** ⭐⭐⭐⭐

**Goal:** Native IDE integration for AI-powered refactoring

**Features:**
- [ ] Right-click context menu: "Get AI Refactoring Suggestion"
- [ ] Side panel for AI analysis
- [ ] Inline code actions
- [ ] Quick fix suggestions
- [ ] Direct Claude API integration
- [ ] One-click apply refactoring
- [ ] Diff preview before applying

**Implementation:**

#### 4.1. VSCode Extension Commands
```typescript
// pragmite-vscode-extension/src/extension.ts

// Register command: "Pragmite: Analyze with AI"
vscode.commands.registerCommand('pragmite.analyzeWithAI', async () => {
  const editor = vscode.window.activeTextEditor;
  if (!editor) return;

  const selection = editor.document.getText(editor.selection);
  const aiResult = await analyzeCode(selection);

  showAIPanel(aiResult);
});

// Register code action provider
vscode.languages.registerCodeActionsProvider('java', {
  provideCodeActions(document, range) {
    // Detect code smells in real-time
    // Offer quick fixes
  }
});
```

#### 4.2. AI Analysis Panel
```typescript
// Show AI analysis in webview panel
const panel = vscode.window.createWebviewPanel(
  'pragmiteAI',
  'AI Refactoring Suggestion',
  vscode.ViewColumn.Beside,
  { enableScripts: true }
);

panel.webview.html = getAIWebviewContent(aiResult);
```

#### 4.3. Webview Content
```html
<div class="ai-suggestion">
  <h2>🤖 AI-Powered Refactoring Suggestion</h2>

  <section class="root-cause">
    <h3>Root Cause</h3>
    <p>{{ rootCause }}</p>
  </section>

  <section class="refactored-code">
    <h3>Suggested Refactoring</h3>
    <pre><code>{{ refactoredCode }}</code></pre>
    <button onclick="applyRefactoring()">✅ Apply Changes</button>
    <button onclick="copyPrompt()">📋 Copy AI Prompt</button>
  </section>

  <section class="explanation">
    <h3>Why This is Better</h3>
    <ul>
      <li>{{ benefit1 }}</li>
      <li>{{ benefit2 }}</li>
    </ul>
  </section>
</div>
```

#### 4.4. Apply Refactoring
```typescript
async function applyRefactoring(refactoredCode: string) {
  const editor = vscode.window.activeTextEditor;
  if (!editor) return;

  // Show diff first
  const uri = editor.document.uri;
  const diffUri = vscode.Uri.parse(`pragmite-diff:${uri.path}`);

  await vscode.commands.executeCommand('vscode.diff',
    uri,
    diffUri,
    'Original ↔ Refactored'
  );

  // Confirm and apply
  const apply = await vscode.window.showInformationMessage(
    'Apply AI refactoring?',
    'Apply', 'Cancel'
  );

  if (apply === 'Apply') {
    await editor.edit(editBuilder => {
      const range = editor.selection;
      editBuilder.replace(range, refactoredCode);
    });
  }
}
```

**Files to Modify:**
- `pragmite-vscode-extension/src/extension.ts`
- `pragmite-vscode-extension/package.json` (add commands)

**Files to Create:**
- `pragmite-vscode-extension/src/ai/AIAnalyzer.ts`
- `pragmite-vscode-extension/src/ai/ClaudeClient.ts`
- `pragmite-vscode-extension/src/webview/AIPanel.ts`

**Acceptance Criteria:**
- ✓ Right-click menu item works
- ✓ AI panel shows analysis
- ✓ Refactoring can be applied with one click
- ✓ Diff preview shown before applying
- ✓ Claude API integration works

---

## 🔧 Technical Requirements

### Claude API Configuration

**Environment Variable (Recommended):**
```bash
export CLAUDE_API_KEY="sk-ant-xxxxx"
```

**Config File (.pragmite.yaml):**
```yaml
ai:
  provider: "claude"
  apiKey: "${CLAUDE_API_KEY}"  # Reads from environment
  model: "claude-sonnet-4-5"
  maxTokens: 4096
  temperature: 0.3
  enabled: true
  cacheResults: true
```

**CLI Argument:**
```bash
java -jar pragmite-core-1.4.0.jar /path/to/project \
  --generate-ai-prompts \
  --auto-refactor \
  --ai-key "sk-ant-xxxxx"
```

**VSCode Extension Settings:**
```json
{
  "pragmite.ai.provider": "claude",
  "pragmite.ai.apiKey": "",  // User will paste here
  "pragmite.ai.model": "claude-sonnet-4-5",
  "pragmite.ai.enabled": true
}
```

---

## 📊 Success Metrics

### Phase 1 (HTML Report)
- ✓ AI section renders correctly
- ✓ Prompts copyable
- ✓ Visual quality high
- ✓ Mobile-responsive

### Phase 2 (Claude API)
- ✓ 90%+ successful refactorings
- ✓ API calls < 1000ms average
- ✓ Cache hit rate > 50%
- ✓ Error rate < 5%

### Phase 3 (Online Editor)
- ✓ Code execution success rate > 95%
- ✓ Diff view clear and accurate
- ✓ Performance metrics meaningful
- ✓ User can understand improvements

### Phase 4 (VSCode Extension)
- ✓ Response time < 2 seconds
- ✓ Refactoring accuracy > 90%
- ✓ Diff preview accurate
- ✓ Zero crashes

---

## 🎯 Current Focus

**NOW:** Phase 1 - HTML Report AI Section (2-3 hours)

**NEXT:** Phase 2 - Claude API Integration (8-10 hours)

---

## 📝 Notes

- API key her zaman environment variable veya config file'dan okunacak
- User sadece key'i yapıştıracak, kod hazır olacak
- Monaco editor ~5MB, HTML report büyüyecek ama kabul edilebilir
- Code execution için JDoodle API kullanacağız (simple + reliable)
- VSCode extension için TypeScript kullanılacak
- Tüm AI prompts English olacak (Claude için optimal)

---

**Last Updated:** December 25, 2025
