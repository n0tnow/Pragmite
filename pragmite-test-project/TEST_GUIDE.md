# 🧪 PRAGMITE EXTENSION TEST GUIDE

## ✅ PRE-TEST CHECKLIST
- [x] Extension installed: `pragmite-1.0.0.vsix`
- [x] Test project created: 2 Java files, 225 lines
- [x] CLI Analysis successful: 30 code smells detected in 342ms

## 🎯 TEST RESULTS (CLI Verification)

### Code Smells Detected (30 total)
```
String Concat In Loop            6
Magic Number                     5
Unused Variable                  3
Speculative Generality           3
Long Method                      2
Empty Catch Block                2
Missing Try With Resources       2
High Cyclomatic Complexity       1
Deeply Nested Code               1
Unused Import                    1
Magic String                     1
Long Parameter List              1
Lazy Class                       1
Primitive Obsession              1
```

### Files Analyzed
- ✅ Calculator.java (143 lines, 11 methods)
- ✅ UserService.java (82 lines, 6 methods)

---

## 📋 VSCODE EXTENSION TEST SCENARIOS

### 1️⃣ **Extension Activation**
**Steps:**
1. VSCode should already be open with test project
2. Check bottom-right status bar

**Expected:**
- ✅ Status bar shows: `🔬 Pragmite`
- ✅ No errors in console (Ctrl+Shift+U → Output → Pragmite)

---

### 2️⃣ **Automatic File Analysis**
**Steps:**
1. Open `Calculator.java`
2. Wait 1-2 seconds

**Expected:**
- ✅ Yellow/red underlines appear on issues
- ✅ Status bar updates: `🔬 X issues, Y high complexity`
- ✅ Problems panel (Ctrl+Shift+M) shows Pragmite issues

---

### 3️⃣ **Tree View (Explorer Panel)**
**Steps:**
1. Open Explorer sidebar (Ctrl+Shift+E)
2. Scroll down to "PRAGMITE RESULTS" section

**Expected:**
- ✅ Shows quality score
- ✅ Shows categorized issues:
  - Critical Issues (if any)
  - Major Issues
  - Minor Issues
  - High Complexity
- ✅ Click on any issue → jumps to that line in code

---

### 4️⃣ **Code Lens (Method Annotations)**
**Steps:**
1. Open `Calculator.java`
2. Look above method declarations

**Expected:**
```java
✓ O(1) | CC: 2                    // add() method - green
→ O(n) | CC: 3                    // sum() method - blue
⚠️ O(n²) | CC: 5                   // multiplyMatrices() - orange background
$(warning) 3 issues               // processData() - has code smells
```

---

### 5️⃣ **Inline Decorations (Colored Hints)**
**Steps:**
1. Open `Calculator.java`
2. Look at the end of method signatures

**Expected:**
- Line 23: `public int add(...)` → shows ` ✓ O(1)` in green
- Line 28: `public int sum(...)` → shows ` → O(n)` in blue
- Line 37: `public int[][] multiplyMatrices(...)` → shows ` ⚠️ O(n²)` with orange background

---

### 6️⃣ **Diagnostics (Hover Messages)**
**Steps:**
1. Hover over yellow underline on line 6: `import java.io.IOException`

**Expected:**
```
[UNUSED_IMPORT] Unused import: java.io.IOException

Source: Pragmite
```

---

### 7️⃣ **Quick Fixes (Lightbulb 💡)**
**Steps:**
1. Click on yellow underline or press `Ctrl+.` on line 6
2. Quick fix menu appears

**Expected:**
```
💡 Quick Fix
   🗑️ Remove unused import
   🙈 Ignore this issue
```

**Action:** Click "Remove unused import" → line deleted ✅

---

### 8️⃣ **Workspace Analysis**
**Steps:**
1. Click status bar `🔬 Pragmite` icon
   OR
2. Press `Ctrl+Shift+P` → type "Pragmite: Analyze Entire Workspace"

**Expected:**
- ✅ Progress notification appears
- ✅ After ~500ms: "Analysis complete! Score: X/100, Found 30 code smells in 2 files"
- ✅ Tree view fully populated
- ✅ All files have diagnostics

---

### 9️⃣ **Quality Report (HTML)**
**Steps:**
1. Press `Ctrl+Shift+P`
2. Type: "Pragmite: Show Quality Report"
3. Press Enter

**Expected:**
- ✅ New tab opens with HTML report
- ✅ Shows quality score with colored grade
- ✅ Shows metrics cards (Files, Lines, Code Smells, etc.)
- ✅ Table of code smells by severity
- ✅ Top 20 code smells list

---

### 🔟 **Save & Re-analyze**
**Steps:**
1. Open `Calculator.java`
2. Remove the unused import (line 6)
3. Save file (`Ctrl+S`)

**Expected:**
- ✅ Instant re-analysis (< 300ms)
- ✅ Code smell count decreases by 1
- ✅ Status bar updates
- ✅ Yellow underline removed

---

## 🐛 TROUBLESHOOTING

### Extension not activating?
```bash
# Check if Java is accessible
java -version

# Check extension logs
# Ctrl+Shift+U → Output → Pragmite
```

### No diagnostics showing?
1. Check settings: `File → Preferences → Settings → Pragmite`
2. Ensure `pragmite.enabled = true`
3. Ensure `pragmite.analyzeOnSave = true`

### JAR not found error?
```
Extension looks for: lib/pragmite-core-1.0.0.jar
Check: ~/.vscode/extensions/pragmite-1.0.0/lib/
```

---

## 📊 EXPECTED PERFORMANCE

- **File Analysis**: ~200-400ms per file
- **Workspace Analysis**: ~342ms for 2 files (225 lines)
- **Extension Activation**: < 2 seconds
- **Re-analysis on Save**: < 300ms

---

## ✨ ADVANCED FEATURES TO TEST

### Code Lens Hover
- Hover over `✓ O(1)` → Shows detailed complexity tooltip

### Tree View Click
- Click "String Concat In Loop (6)" → Opens list of 6 occurrences
- Click any occurrence → Jumps to that line

### Quick Fix Variations
- **Unused Import**: Automatic removal
- **Empty Catch**: Suggests logging code
- **Magic Number**: Suggests extracting constant
- **Try-with-resources**: Shows refactoring example

### Configuration Changes
1. `Ctrl+,` → Search "Pragmite"
2. Uncheck "Analyze On Save"
3. Save file → No auto-analysis
4. Check it again → Auto-analysis resumes

---

## 🎉 SUCCESS CRITERIA

- [ ] Extension activates without errors
- [ ] Diagnostics appear on Java files
- [ ] Tree view shows categorized issues
- [ ] Code lens shows complexity annotations
- [ ] Inline decorations are colorful and readable
- [ ] Quick fixes work (at least unused import)
- [ ] Workspace analysis completes successfully
- [ ] Quality report opens and displays correctly
- [ ] Save triggers re-analysis
- [ ] Performance is acceptable (< 500ms for small files)

---

**If all criteria pass: PRAGMITE EXTENSION IS PRODUCTION-READY! 🚀**
