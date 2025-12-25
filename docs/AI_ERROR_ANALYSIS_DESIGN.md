# AI-Powered Error Analysis & Prompt Suggestion System

**Feature:** v1.4.0 (Future Enhancement)
**Status:** Design Phase
**Priority:** ⭐⭐⭐⭐ (High)

## 🎯 Overview

Enhance Pragmite with AI-powered error analysis that provides:
1. **Detailed Error Explanations** - Why the code smell exists and its impact
2. **AI Prompt Suggestions** - Ready-to-use prompts for AI coding assistants (Claude, GPT-4, etc.)
3. **Context-Aware Recommendations** - Tailored suggestions based on code context

## 🌟 Key Features

### 1. Error Analysis Engine

For each detected code smell, provide:
- **Root Cause Analysis** - Why this is problematic
- **Impact Assessment** - Severity and consequences
- **Context Information** - Surrounding code patterns
- **Best Practices** - Industry standards violated

### 2. AI Prompt Generator

Generate ready-to-use prompts in English:
```
Line 42 in UserService.java: Long Method (78 lines)

Problem:
This method exceeds the recommended 50-line threshold and has
cyclomatic complexity of 15, making it difficult to test and maintain.

AI Prompt Suggestion:
"Please refactor the following Java method to improve maintainability.
The method is currently 78 lines long with complexity 15. Break it into
smaller, focused methods following Single Responsibility Principle.
Preserve all existing functionality and add unit tests.

[CODE SNIPPET]
public void processUserRegistration(User user) {
    // ... 78 lines of code ...
}
[/CODE SNIPPET]"
```

### 3. Multi-Language Support

All error messages, analyses, and prompts will be in **English** for:
- Universal accessibility
- AI assistant compatibility
- Professional documentation standards

## 🏗️ Architecture

### Component Structure

```
ai-analysis/
├── AnalysisEngine.java         # Core analysis orchestrator
├── ErrorAnalyzer.java          # Per-smell-type analyzers
├── PromptGenerator.java        # AI prompt template engine
├── ContextExtractor.java       # Code context extraction
└── templates/
    ├── long-method.template    # Prompt template for long methods
    ├── god-class.template      # Prompt template for god classes
    └── ...                     # Templates for each smell type
```

### Data Flow

```
CodeSmell Detection
    ↓
ErrorAnalyzer.analyze()
    ↓
ContextExtractor.extractContext()
    ↓
PromptGenerator.generatePrompt()
    ↓
Enhanced AnalysisResult
    ↓
Output (Console/HTML/JSON)
```

## 📝 Implementation Plan

### Phase 1: Core Infrastructure (v1.4.0)

**1.1 Create AnalysisEngine**
```java
public class AnalysisEngine {
    /**
     * Analyze a code smell and generate AI-ready insights.
     */
    public AIAnalysisResult analyze(CodeSmell smell, String sourceCode);
}
```

**1.2 Create AIAnalysisResult Model**
```java
public class AIAnalysisResult {
    private String rootCause;           // Why the problem exists
    private String impact;              // Consequences
    private String recommendation;      // What to do
    private String aiPrompt;           // Ready-to-use AI prompt
    private List<String> codeSnippets; // Relevant code sections
    private Map<String, String> metadata; // Additional context
}
```

**1.3 Implement PromptGenerator**
```java
public class PromptGenerator {
    /**
     * Generate AI prompt based on code smell and context.
     */
    public String generatePrompt(
        CodeSmell smell,
        String codeSnippet,
        String context
    );
}
```

### Phase 2: Template System (v1.4.0)

**2.1 Prompt Templates**

Create template for each code smell type:

**Long Method Template:**
```
Refactor the following Java method to improve maintainability.

Current Issues:
- Length: {line_count} lines (threshold: {threshold})
- Complexity: {complexity} (threshold: {complexity_threshold})
- Responsibilities: {responsibility_count}

Refactoring Goals:
1. Break into smaller methods (max {threshold} lines each)
2. Reduce complexity to below {complexity_threshold}
3. Apply Single Responsibility Principle
4. Maintain existing functionality
5. Add comprehensive unit tests

Code to Refactor:
```java
{code_snippet}
```

Please provide:
1. Refactored code with method extraction
2. Brief explanation of changes
3. Unit test examples for new methods
```

**Magic Number Template:**
```
Replace magic numbers with named constants in this Java code.

Detected Magic Numbers:
{magic_numbers_list}

Current Code:
```java
{code_snippet}
```

Please:
1. Create descriptive constant names (e.g., MAX_RETRY_COUNT)
2. Use appropriate visibility (private static final)
3. Group related constants
4. Add javadoc explaining the constant's purpose
5. Preserve all functionality

Format: Follow Java naming conventions (UPPER_SNAKE_CASE)
```

**2.2 Template Variables**

Standard variables available in all templates:
- `{smell_type}` - Type of code smell
- `{file_path}` - File location
- `{line_number}` - Line number
- `{severity}` - Severity level
- `{code_snippet}` - Extracted code
- `{context_before}` - Code before the smell
- `{context_after}` - Code after the smell
- Custom variables per smell type

### Phase 3: Context Extraction (v1.4.0)

**3.1 Code Context Extraction**
```java
public class ContextExtractor {
    /**
     * Extract relevant code context for AI analysis.
     */
    public CodeContext extractContext(
        CodeSmell smell,
        CompilationUnit cu
    ) {
        // Extract:
        // - Method/class containing the smell
        // - Surrounding code (±5 lines)
        // - Import statements
        // - Related methods/fields
        // - Comments and documentation
    }
}
```

**3.2 Smart Snippet Selection**
- Extract minimal but complete code context
- Include necessary imports
- Preserve indentation and formatting
- Add line numbers for reference

### Phase 4: Output Integration (v1.4.0)

**4.1 Console Output Enhancement**
```
❌ Long Method (MAJOR)
   File: src/UserService.java:42
   Length: 78 lines (threshold: 50)
   Complexity: 15 (threshold: 10)

   Analysis:
   This method handles multiple responsibilities including validation,
   database operations, and notification sending. This violates the
   Single Responsibility Principle and makes the method difficult to
   test and maintain.

   Impact:
   - High testing complexity (multiple mock dependencies)
   - Difficult to understand and modify
   - Increased bug risk in future changes

   💡 AI Prompt (Copy & Paste to Claude/GPT-4):
   ┌────────────────────────────────────────────────────────
   │ Please refactor this Java method to improve maintainability...
   │ [Full prompt here]
   └────────────────────────────────────────────────────────
```

**4.2 HTML Report Enhancement**
Add "AI Assistant" section to each code smell card:
```html
<div class="ai-prompt-section">
    <h4>🤖 AI Assistant Prompt</h4>
    <button onclick="copyPrompt()">Copy to Clipboard</button>
    <pre class="ai-prompt">{generated_prompt}</pre>
</div>
```

**4.3 JSON Output Enhancement**
```json
{
  "codeSmells": [
    {
      "type": "LONG_METHOD",
      "file": "UserService.java",
      "line": 42,
      "aiAnalysis": {
        "rootCause": "Multiple responsibilities in single method",
        "impact": "High testing complexity, difficult maintenance",
        "recommendation": "Extract into 3-4 focused methods",
        "aiPrompt": "Please refactor the following...",
        "codeSnippet": "public void processUserRegistration...",
        "promptTokenCount": 450
      }
    }
  ]
}
```

### Phase 5: CLI Integration (v1.4.0)

**New CLI Flags:**
```bash
# Generate AI prompts for all issues
pragmite --generate-ai-prompts

# Generate prompts only for specific severity
pragmite --generate-ai-prompts --severity CRITICAL,MAJOR

# Export prompts to file
pragmite --generate-ai-prompts --output-prompts prompts.txt

# Copy first prompt to clipboard (interactive mode)
pragmite --interactive-prompts
```

## 📊 Example Output

### Console Output Example

```
╔══════════════════════════════════════════════════════════════╗
║              PRAGMITE AI-POWERED ANALYSIS v1.4.0             ║
║          Detected 3 issues with AI refactoring prompts       ║
╚══════════════════════════════════════════════════════════════╝

Issue 1/3: Long Method
─────────────────────────────────────────────────────────────
📍 Location: src/com/example/UserService.java:42-120
📊 Metrics: 78 lines, complexity 15

🔍 Analysis:
The processUserRegistration method violates Single Responsibility
Principle by handling validation, persistence, email notification,
and audit logging. This makes the method:
- Hard to test (requires 4+ mocks)
- Difficult to modify safely
- Prone to bugs in future changes

💡 Recommended Refactoring:
Extract into 4 focused methods:
1. validateUser(User) - Input validation
2. saveUser(User) - Database persistence
3. sendWelcomeEmail(User) - Email notification
4. logUserCreation(User) - Audit logging

🤖 AI ASSISTANT PROMPT (English):
┌──────────────────────────────────────────────────────────────┐
│ Please refactor the following Java method to improve         │
│ maintainability. The method is currently 78 lines long with  │
│ cyclomatic complexity of 15.                                 │
│                                                              │
│ Current Issues:                                              │
│ - Handles multiple responsibilities (validation, DB, email)  │
│ - High complexity makes testing difficult                    │
│ - Violates Single Responsibility Principle                   │
│                                                              │
│ Refactoring Requirements:                                    │
│ 1. Extract into 4 focused methods (see recommendations)      │
│ 2. Each method should be < 20 lines                         │
│ 3. Reduce complexity to < 5 per method                      │
│ 4. Maintain exact same functionality                         │
│ 5. Add unit tests for each extracted method                 │
│                                                              │
│ Code to Refactor:                                            │
│ ```java                                                      │
│ public void processUserRegistration(User user) {             │
│     // Validation                                            │
│     if (user == null) throw new IllegalArgumentException(); │
│     if (user.getEmail() == null) throw new ...;             │
│     // ... 70 more lines ...                                │
│ }                                                            │
│ ```                                                          │
│                                                              │
│ Expected Output:                                             │
│ - Refactored code with extracted methods                     │
│ - Explanation of architectural improvements                  │
│ - Unit test examples for new methods                        │
└──────────────────────────────────────────────────────────────┘

Press 'C' to copy prompt to clipboard, 'N' for next issue...
```

### HTML Report Enhancement

Add interactive "AI Refactoring Assistant" panel:
```html
<div class="smell-card">
    <div class="smell-header">
        <span class="smell-type">Long Method</span>
        <span class="severity major">MAJOR</span>
    </div>

    <div class="smell-details">
        <p><strong>File:</strong> UserService.java:42</p>
        <p><strong>Length:</strong> 78 lines (threshold: 50)</p>
    </div>

    <div class="ai-analysis-section">
        <h4>🔍 AI Analysis</h4>
        <p class="root-cause">
            <strong>Root Cause:</strong> Multiple responsibilities in single method
        </p>
        <p class="impact">
            <strong>Impact:</strong> High testing complexity, difficult to maintain
        </p>
        <p class="recommendation">
            <strong>Recommendation:</strong> Extract into 4 focused methods
        </p>
    </div>

    <div class="ai-prompt-section">
        <h4>🤖 AI Assistant Prompt</h4>
        <button class="copy-btn" onclick="copyToClipboard(this)">
            📋 Copy to Clipboard
        </button>
        <pre class="ai-prompt">Please refactor the following...</pre>
    </div>
</div>
```

## 🎨 Prompt Templates Library

### Template Categories

1. **Refactoring Prompts**
   - Long Method → Method Extraction
   - God Class → Class Decomposition
   - Feature Envy → Move Method
   - Data Class → Add Behavior

2. **Code Quality Prompts**
   - Magic Numbers → Named Constants
   - Magic Strings → Constants/Enums
   - Complex Boolean → Extract Method
   - Deep Nesting → Guard Clauses

3. **Design Pattern Prompts**
   - Missing Strategy → Implement Strategy Pattern
   - Switch Statement → Polymorphism
   - Primitive Obsession → Value Objects

4. **Testing Prompts**
   - Untested Code → Generate Unit Tests
   - Missing Edge Cases → Add Test Coverage

## 🔧 Configuration

Add to `.pragmite.yaml`:
```yaml
ai_analysis:
  enabled: true
  language: "en"  # Always English

  prompt_templates:
    # Use custom templates
    template_dir: ".pragmite/templates"

  output:
    include_in_console: true
    include_in_html: true
    include_in_json: true

  # AI provider hints (for future API integration)
  target_ai:
    - "claude"
    - "gpt-4"
    - "gemini"
```

## 📈 Success Metrics

Track effectiveness:
1. **Prompt Usage** - How many developers copy prompts
2. **Fix Success Rate** - Issues resolved after using prompts
3. **Time to Resolution** - Faster fixes with AI assistance
4. **User Satisfaction** - Feedback on prompt quality

## 🚀 Future Enhancements (v1.5.0+)

1. **Direct AI Integration**
   - Call Claude/GPT-4 API directly
   - Auto-apply AI-suggested fixes
   - Interactive refinement

2. **Learning System**
   - Track which prompts work best
   - Learn from user modifications
   - Improve templates over time

3. **Multi-Language Prompts**
   - Generate prompts in Turkish (optional)
   - Support other languages
   - Maintain English as default

4. **Context-Aware Prompts**
   - Analyze git history
   - Include team coding standards
   - Reference project architecture

5. **Batch Prompt Generation**
   - Generate prompts for all issues
   - Prioritize by impact
   - Create refactoring roadmap

## 📝 Implementation Checklist

- [ ] Create AnalysisEngine infrastructure
- [ ] Implement AIAnalysisResult model
- [ ] Build PromptGenerator with templates
- [ ] Create ContextExtractor for code analysis
- [ ] Design prompt templates for each smell type
- [ ] Integrate with Console output
- [ ] Enhance HTML report with AI section
- [ ] Add JSON output fields
- [ ] Implement CLI flags (--generate-ai-prompts)
- [ ] Add clipboard copy functionality
- [ ] Write unit tests for prompt generation
- [ ] Create documentation and examples
- [ ] Add configuration options
- [ ] Implement prompt quality metrics

## 💡 Example Use Cases

### Use Case 1: Refactoring Legacy Code
```bash
# Analyze legacy codebase
pragmite --generate-ai-prompts legacy-app/

# Get prompts sorted by impact
pragmite --generate-ai-prompts --sort-by impact

# Focus on critical issues only
pragmite --generate-ai-prompts --severity CRITICAL
```

### Use Case 2: Team Code Review
```bash
# Generate prompts for PR review
pragmite --generate-ai-prompts feature-branch/ > review-prompts.txt

# Share with team for AI-assisted refactoring
# Each developer can use prompts with their preferred AI
```

### Use Case 3: Educational Purpose
```bash
# Students learn by seeing refactoring prompts
pragmite --generate-ai-prompts student-project/

# Prompts teach best practices and patterns
```

## 🎯 Success Criteria

Feature is successful if:
1. ✅ 90%+ of prompts are grammatically correct English
2. ✅ Prompts include all necessary context
3. ✅ AI assistants (Claude/GPT-4) produce working refactorings
4. ✅ Developers report 50%+ time savings on fixes
5. ✅ Prompt templates cover all 21 code smell types

---

**Status:** Design Complete - Ready for v1.4.0 Implementation
**Estimated Effort:** 40-50 hours
**Dependencies:** v1.3.0 (Database & Auto-Fix Infrastructure)
