# Pragmite Test Project

This is a test project to demonstrate Pragmite VSCode Extension features.

## Expected Issues to be Detected

### Calculator.java
- ✅ Unused import (IOException)
- ✅ Magic numbers (42, 100)
- ✅ Magic strings ("OK", "Not Found", etc.)
- ✅ Empty catch block
- ✅ String concatenation in loop
- ✅ Duplicated code
- ✅ Deeply nested code (5 levels)
- ✅ Long method (processData)
- ✅ High cyclomatic complexity
- ✅ O(n²) complexity (multiplyMatrices)

### UserService.java
- ✅ O(log n) complexity (findUser)
- ✅ O(n log n) complexity (sortUsers)
- ✅ Stream in loop (performance issue)
- ✅ Long parameter list
- ✅ Missing try-with-resources
- ✅ Data class (User)

## How to Test

1. Open this folder in VSCode
2. Pragmite extension should activate automatically
3. Open Calculator.java or UserService.java
4. You should see:
   - **Diagnostics**: Yellow/red underlines on issues
   - **Code Lens**: Complexity annotations above methods
   - **Inline Decorations**: Colored complexity hints
   - **Tree View**: "PRAGMITE RESULTS" in Explorer panel
   - **Status Bar**: "🔬 X issues, Y high complexity"
5. Try Quick Fixes:
   - Hover over unused import → "Remove unused import"
   - Hover over empty catch → "Add exception logging"
6. Run workspace analysis:
   - Click status bar "🔬 Pragmite"
   - Or Ctrl+Shift+P → "Pragmite: Analyze Entire Workspace"
7. View quality report:
   - Ctrl+Shift+P → "Pragmite: Show Quality Report"
