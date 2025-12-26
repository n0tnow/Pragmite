# 🚀 AI Features Implementation Roadmap

**Created:** December 25, 2025
**Updated:** December 25, 2025
**Status:** Phase 1 & 2 Completed ✅ | Planning Phase 3 (v1.5.0)

---

## 🎊 Completed Milestones

### ✅ v1.4.0 - AI-Powered Analysis & Auto-Refactoring (COMPLETED)

**Phase 0: Core AI Infrastructure** ✅
**Phase 1: HTML Report AI Section** ✅
**Phase 2: Claude API Auto-Refactoring** ✅

**Release Date:** December 25, 2025
**Status:** Production Ready 🚀

**Delivered Features:**
- AI-powered error analysis with 28 specialized templates
- Claude Sonnet 4.5 API integration
- Automatic code refactoring
- Before/after comparison in HTML reports
- JSON output with refactored code
- Comprehensive documentation

---

## 📋 Implementation Plan (Sıralı)

### ✅ Phase 0: Core AI Infrastructure (COMPLETED - v1.4.0)
- [x] AnalysisEngine - Root cause, impact, recommendations
- [x] PromptGenerator - 28 code smell templates
- [x] ContextExtractor - Smart code snippet extraction
- [x] AIAnalysisResult - Rich result model
- [x] CLI Integration - `--generate-ai-prompts` flag
- [x] JSON output for AI analysis

---

### ✅ Phase 1: HTML Report AI Section (COMPLETED - v1.4.0)
**Priority:** ⭐⭐⭐⭐⭐
**Completion Date:** December 25, 2025

**Goal:** HTML report'a AI analysis section ekle

**Features:**
- [x] Her code smell için AI analysis card
- [x] Root cause, impact, recommendation gösterimi
- [x] AI prompt gösterimi (copy button ile)
- [x] Before/After code comparison (with refactored code)
- [x] "Why This is Better" section
- [x] Responsive design
- [x] Print-friendly CSS

**Files Modified:**
- ✅ `src/main/java/com/pragmite/report/HtmlReportGenerator.java`
- ✅ HTML template with AI section

**Acceptance Criteria:**
- ✅ AI analysis her code smell altında görünür
- ✅ Prompt kopyalanabilir (clipboard)
- ✅ Visual olarak attractive
- ✅ Mobile-responsive
- ✅ Before/after comparison with color coding

---

### ✅ Phase 2: Claude API Auto-Refactoring (COMPLETED - v1.4.0)
**Priority:** ⭐⭐⭐⭐⭐
**Completion Date:** December 25, 2025

**Goal:** Claude API ile otomatik refactored code generation

**Features:**
- [x] Claude API client implementation (ClaudeApiClient.java)
- [x] API key configuration (environment variables + CLI)
- [x] Automatic refactored code generation
- [x] Error handling and retry logic
- [x] RefactoredCode model with Builder pattern
- [x] JSON output with refactored code

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

---

## 🚀 v1.5.0 - Auto-Apply & Performance Metrics (PLANNED)

**Target Release:** Q1 2026
**Priority:** ⭐⭐⭐⭐⭐

### Phase 3: Automatic Code Application (10-12 hours)
**Priority:** ⭐⭐⭐⭐

**Goal:** AI'nin önerdiği refactored code'u otomatik uygula

**Features:**
- [ ] One-command auto-fix with AI
- [ ] Smart code replacement (AST-based)
- [ ] Backup before application
- [ ] Rollback support
- [ ] Dry-run mode
- [ ] Interactive approval
- [ ] Batch processing
- [ ] Git integration (auto-commit)

**CLI Usage:**
```bash
# Dry run - shows what would be changed
pragmite ./project --auto-refactor --auto-apply --dry-run

# Apply with confirmation
pragmite ./project --auto-refactor --auto-apply --confirm

# Auto-apply all with backup
pragmite ./project --auto-refactor --auto-apply --backup

# Auto-apply and commit
pragmite ./project --auto-refactor --auto-apply --git-commit
```

**Safety Features:**
- ✓ Automatic backup before changes
- ✓ AST-aware replacement (not regex)
- ✓ Compilation check after each change
- ✓ Rollback on compilation failure
- ✓ Git integration for easy revert

---

### Phase 4: Result Caching System (6-8 hours)
**Priority:** ⭐⭐⭐⭐

**Goal:** Cache AI responses to reduce API costs

**Features:**
- [ ] Smart cache key generation
- [ ] SQLite cache storage
- [ ] TTL (time-to-live) support
- [ ] Cache invalidation on code change
- [ ] Cache statistics
- [ ] Cost savings report
- [ ] Cache export/import

**Implementation:**
```java
public class AIResultCache {
    private final DatabaseManager db;

    public RefactoredCode get(String codeHash, String promptHash) {
        // Check cache
        if (cached && !expired) {
            return cachedResult;
        }
        return null;
    }

    public void put(String codeHash, String promptHash, RefactoredCode result) {
        // Store with TTL (30 days default)
    }

    public CacheStats getStats() {
        return new CacheStats(
            hitRate: 0.73,  // 73% cache hits
            costSaved: 45.30  // $45.30 saved
        );
    }
}
```

**Expected Savings:**
- First run: $0 saved (all API calls)
- Second run (same code): $10 saved (100% cache hit)
- Incremental changes: $7 saved (70% cache hit)

---

### Phase 5: .pragmite.yaml AI Configuration (4-5 hours)
**Priority:** ⭐⭐⭐

**Goal:** Tam AI konfigürasyon desteği

**Features:**
- [ ] AI provider settings in YAML
- [ ] Model selection
- [ ] Temperature, max tokens
- [ ] Custom prompts per smell type
- [ ] Retry and timeout configuration
- [ ] Cost limits
- [ ] Batch size configuration

**Configuration Example:**
```yaml
ai:
  # Provider settings
  provider: "claude"
  fallback: ["gpt-4", "gemini"]

  # API keys (from environment)
  claude:
    apiKey: "${CLAUDE_API_KEY}"
    model: "claude-sonnet-4-5"
    maxTokens: 4096
    temperature: 0.3

  # Cost controls
  costLimits:
    maxCostPerRun: 5.00  # Max $5 per analysis
    warnThreshold: 3.00  # Warn at $3

  # Caching
  cache:
    enabled: true
    ttlDays: 30
    maxSizeMB: 500

  # Automatic application
  autoApply:
    enabled: false
    mode: "safe"  # safe, aggressive
    backup: true
    gitCommit: false

  # Custom templates (optional)
  customPrompts:
    MAGIC_NUMBER: |
      Extract this magic number: {{code}}
      Provide a better name.
```

---

### Phase 6: Performance Metrics & A/B Testing (8-10 hours)
**Priority:** ⭐⭐⭐⭐⭐

**Goal:** Refactored code'un gerçek performans iyileşmesini ölç

**Features:**
- [ ] JMH benchmark integration
- [ ] Before/after execution time comparison
- [ ] Memory usage comparison
- [ ] Complexity metrics (cyclomatic, cognitive)
- [ ] Test coverage impact analysis
- [ ] A/B test framework for validating improvements
- [ ] Performance regression detection
- [ ] Automated benchmark reports

**Implementation:**

#### 6.1. JMH Benchmark Integration
```java
// src/main/java/com/pragmite/metrics/PerformanceBenchmark.java
@State(Scope.Benchmark)
public class PerformanceBenchmark {
    @Benchmark
    public void benchmarkOriginal(Blackhole bh) {
        // Run original code
        bh.consume(originalMethod());
    }

    @Benchmark
    public void benchmarkRefactored(Blackhole bh) {
        // Run refactored code
        bh.consume(refactoredMethod());
    }
}
```

#### 6.2. Metrics Comparison
```java
public class MetricsComparator {
    public ComparisonResult compare(String originalCode, String refactoredCode) {
        // Execution time
        long beforeTime = measureExecutionTime(originalCode);
        long afterTime = measureExecutionTime(refactoredCode);

        // Memory usage
        long beforeMemory = measureMemoryUsage(originalCode);
        long afterMemory = measureMemoryUsage(refactoredCode);

        // Complexity
        int beforeComplexity = calculateComplexity(originalCode);
        int afterComplexity = calculateComplexity(refactoredCode);

        return new ComparisonResult(
            timeImprovement: calculateImprovement(beforeTime, afterTime),
            memoryImprovement: calculateImprovement(beforeMemory, afterMemory),
            complexityReduction: beforeComplexity - afterComplexity
        );
    }
}
```

#### 6.3. HTML Report Integration
```html
<div class="performance-metrics">
  <h4>📊 Performance Comparison</h4>
  <table>
    <tr>
      <th>Metric</th>
      <th>Before</th>
      <th>After</th>
      <th>Improvement</th>
    </tr>
    <tr>
      <td>Execution Time</td>
      <td>125ms</td>
      <td>45ms</td>
      <td class="positive">64% faster ⬆</td>
    </tr>
    <tr>
      <td>Memory Usage</td>
      <td>2.4MB</td>
      <td>1.1MB</td>
      <td class="positive">54% less ⬇</td>
    </tr>
    <tr>
      <td>Cyclomatic Complexity</td>
      <td>15</td>
      <td>6</td>
      <td class="positive">60% simpler ⬇</td>
    </tr>
  </table>
</div>
```

#### 6.4. Automated Testing
```java
public class RefactoringValidator {
    public ValidationResult validate(RefactoredCode refactored) {
        // Compile both versions
        boolean originalCompiles = compile(refactored.getOriginalCode());
        boolean refactoredCompiles = compile(refactored.getRefactoredCode());

        // Run tests
        TestResults originalTests = runTests(refactored.getOriginalCode());
        TestResults refactoredTests = runTests(refactored.getRefactoredCode());

        // Compare performance
        PerformanceMetrics originalPerf = benchmark(refactored.getOriginalCode());
        PerformanceMetrics refactoredPerf = benchmark(refactored.getRefactoredCode());

        return new ValidationResult(
            compilesSuccessfully: refactoredCompiles,
            testsPass: refactoredTests.allPassed(),
            performanceImproved: refactoredPerf.isBetterThan(originalPerf),
            noRegressions: !hasRegressions(originalTests, refactoredTests)
        );
    }
}
```

**Expected Benefits:**
- Quantifiable evidence that refactoring improves code
- Catch performance regressions early
- Build confidence in AI-generated refactorings
- Data-driven decision making

---

## 🎯 Current Focus (Updated)

**COMPLETED:** ✅ Phase 0, 1, 2 (v1.4.0)

**NOW:** Planning v1.5.0 features

**NEXT (v1.5.0):**
1. Automatic Code Application (PRIMARY FOCUS)
2. Result Caching System
3. Full .pragmite.yaml AI configuration
4. Performance Metrics & A/B Testing (CRITICAL FEATURE)

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
