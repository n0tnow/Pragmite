# Pragmite - Java Code Quality Analyzer

**Pragmite** is a powerful VSCode extension that brings real-time Java code quality analysis directly into your editor. Built on the Pragmatic Programmer principles, Pragmite helps you write better, more maintainable code.

## ✨ Features

### 🔍 **Real-time Code Analysis**
- Analyze Java files on save automatically
- Instant feedback on code quality issues
- 31 different code smell detectors

### 📊 **Big-O Complexity Detection**
- Automatic algorithmic complexity analysis
- Detects O(1), O(n), O(n²), O(log n), O(n log n), and more
- Warns about performance bottlenecks

### 🎯 **Code Smell Detection**
- **Duplicated Code**: Find copy-paste violations
- **Deeply Nested Code**: Identify hard-to-maintain nested structures
- **Long Methods**: Detect methods that do too much
- **Magic Numbers/Strings**: Find hard-coded values
- **God Classes**: Spot classes with too many responsibilities
- **And 26 more detectors!**

### 📈 **Quality Metrics**
- **DRY Score**: Don't Repeat Yourself principle compliance
- **Orthogonality Score**: Component independence
- **Correctness Score**: Code reliability metrics
- **Performance Score**: Efficiency analysis
- **Overall Quality Grade**: A-F rating system

### 🎨 **Beautiful Reports**
- Interactive HTML quality reports
- Visual score dashboards
- Detailed code smell breakdown
- Exportable analysis results

## 🚀 Getting Started

### Prerequisites
- **Java 21+** must be installed
- **VSCode 1.85.0** or higher

### Installation

1. Install the extension from VSCode Marketplace
2. Open a Java project
3. Pragmite will automatically analyze your code!

### Usage

#### Analyze Current File
- Save any Java file (auto-analysis)
- Or use command: `Pragmite: Analyze Current File`
- Or right-click in editor → `Pragmite: Analyze Current File`

#### Analyze Entire Workspace
- Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`)
- Type: `Pragmite: Analyze Entire Workspace`
- Click the status bar item `🔬 Pragmite`

#### View Quality Report
- Command: `Pragmite: Show Quality Report`
- See comprehensive quality metrics and code smells

## ⚙️ Configuration

Open VSCode Settings (`Ctrl+,` / `Cmd+,`) and search for "Pragmite":

```json
{
  "pragmite.enabled": true,
  "pragmite.analyzeOnSave": true,
  "pragmite.showInlineHints": true,
  "pragmite.javaPath": "java"
}
```

### Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `pragmite.enabled` | boolean | `true` | Enable/disable Pragmite analysis |
| `pragmite.analyzeOnSave` | boolean | `true` | Analyze file automatically on save |
| `pragmite.showInlineHints` | boolean | `true` | Show inline complexity hints |
| `pragmite.javaPath` | string | `"java"` | Path to Java executable |

## 📊 Example Output

### In-Editor Diagnostics
```
⚠ [DEEPLY_NESTED_CODE] Excessive nesting depth (5 levels)
⚠ [HIGH_COMPLEXITY] High complexity (O_N_SQUARED) in method 'processData'
ℹ [MAGIC_NUMBER] Magic number 42 should be a named constant
```

### Quality Score
```
Overall Score: 72/100 (C)
├─ DRY Score:          85/100
├─ Orthogonality:      60/100
├─ Correctness:        70/100
├─ Performance:        90/100
└─ Pragmatic Score:    72/100
```

## 🏗️ Architecture

Pragmite consists of:

1. **Java Core Engine** (pragmite-core-1.0.0.jar)
   - JavaParser 3.27.1 (Java 1-24 support)
   - AST-based code analysis
   - 31 code smell detectors
   - Big-O complexity analyzer

2. **VSCode Extension** (TypeScript)
   - Real-time diagnostics
   - Command integration
   - Webview reports
   - Status bar integration

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## 📝 License

MIT License - see LICENSE file for details

## 🔗 Links

- [GitHub Repository](https://github.com/pragmite/pragmite)
- [Issue Tracker](https://github.com/pragmite/pragmite/issues)
- [Documentation](https://github.com/pragmite/pragmite/wiki)

## 🎓 Based on The Pragmatic Programmer

Pragmite is inspired by the principles from "The Pragmatic Programmer" by David Thomas and Andrew Hunt:

- **DRY**: Don't Repeat Yourself
- **Orthogonality**: Keep components independent
- **Tracer Bullets**: Quick feedback loops
- **Good Enough Software**: Balance quality and pragmatism

## 📸 Screenshots

*Coming soon*

---

**Made with ❤️ by the Pragmite team**
