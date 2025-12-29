# 🚀 PRAGMITE EXTENSION - QUICK START

## ✅ Extension Successfully Reinstalled!

The extension has been fixed and reinstalled with all required files:
- ✅ Compiled JavaScript files (out/extension.js)
- ✅ JAR file (lib/pragmite-core-1.6.3.jar)
- ✅ Extension version: 1.6.3

---

## 🔄 NEXT STEP: Reload VSCode

**CRITICAL**: VSCode needs to reload to activate the extension!

### Option 1: Reload Window (Fastest)
1. Press `Ctrl+Shift+P`
2. Type: `Developer: Reload Window`
3. Press Enter
4. Wait 2-3 seconds for activation

### Option 2: Restart VSCode
1. Close VSCode completely
2. Reopen VSCode
3. Open: `File → Open Folder → C:\Pragmite\pragmite-test-project`

---

## 🧪 VERIFY ACTIVATION (30 seconds)

### Step 1: Open Test File
1. In VSCode Explorer, navigate to:
   ```
   src/main/java/com/example/Calculator.java
   ```
2. Open the file

### Step 2: Check Status Bar (Bottom Right)
Look for: `🔬 Pragmite`

**If you see it**: ✅ Extension is active!
**If you don't see it**: Try manual activation (Step 3)

### Step 3: Manual Activation (if needed)
1. Press `Ctrl+Shift+P`
2. Type: `Pragmite: Analyze Current File`
3. Press Enter

You should see a notification: "Analyzing file..."

### Step 4: Check Output Logs
1. Press `Ctrl+Shift+U` (Opens Output panel)
2. In the dropdown (top right), select: **Pragmite**
3. You should see:
   ```
   [Pragmite] Pragmite extension is now active!
   [Pragmite] Starting analysis: Calculator.java
   [Pragmite] Analysis complete: 11 methods analyzed
   ```

---

## 🎯 EXPECTED RESULTS

Once activated, you should see:

### 1. **Diagnostics** (Yellow/Red Underlines)
- Line 5: Unused import → Yellow underline
- Line 99: Magic number (42) → Yellow underline
- Line 101: Empty catch block → Red underline

### 2. **Code Lens** (Method Annotations)
Above methods, you'll see:
```java
$(check) O(1) | CC: 1                    // add() method
$(arrow-right) O(n) | CC: 2              // sum() method
$(warning) O(n²) | CC: 3                 // multiplyMatrices()
```

### 3. **Inline Decorations** (End of Lines)
```java
public int add(int a, int b) {           ✓ O(1)
public int sum(List<Integer> nums) {     → O(n)
public int[][] multiplyMatrices(...) {   ⚠️ O(n²)
```

### 4. **Tree View** (Explorer Sidebar)
Scroll down to see:
```
📁 EXPLORER
└─ 🔬 PRAGMITE RESULTS
   ├─ Quality Score: 72/100
   ├─ Critical Issues (2)
   ├─ Major Issues (8)
   └─ Minor Issues (20)
```

### 5. **Status Bar** (Bottom Right)
```
🔬 Pragmite: 30 issues, 3 high complexity
```

---

## 🐛 TROUBLESHOOTING

### Problem: Status bar icon not showing
**Solution**: Check Output logs for errors
```
Ctrl+Shift+U → Select "Pragmite" dropdown
```

### Problem: "Java not found" error
**Solution**: Verify Java installation
```bash
java -version
# Should show: openjdk version "21.0.7"
```

### Problem: No diagnostics appearing
**Solution**:
1. Check settings: `Ctrl+,` → Search "Pragmite"
2. Ensure `pragmite.enabled = true`
3. Ensure `pragmite.analyzeOnSave = true`

### Problem: JAR not found error
**Solution**: Verify JAR location
```bash
ls ~/.vscode/extensions/pragmite.pragmite-1.0.0/lib/
# Should show: pragmite-core-1.0.0.jar (9.0MB)
```

---

## 🎉 SUCCESS CHECKLIST

After reload, verify:
- [ ] Status bar shows `🔬 Pragmite`
- [ ] Output logs show "Pragmite extension is now active!"
- [ ] Opening Calculator.java shows diagnostics (yellow underlines)
- [ ] Code Lens annotations appear above methods
- [ ] Tree view shows "PRAGMITE RESULTS" panel
- [ ] Command Palette shows 3 Pragmite commands

**If all checked**: Extension is working perfectly! 🎊

---

## 📖 NEXT STEPS

Once verified, explore features:

1. **Run Workspace Analysis**:
   - Click status bar `🔬 Pragmite` icon
   - Wait ~500ms
   - See notification: "Found 30 code smells in 2 files"

2. **View Quality Report**:
   - Press `Ctrl+Shift+P`
   - Type: `Pragmite: Show Quality Report`
   - HTML report opens in new tab

3. **Try Quick Fixes**:
   - Click yellow underline on line 5 (unused import)
   - Press `Ctrl+.` or click lightbulb 💡
   - Select "Remove unused import"
   - Import deleted automatically!

4. **Test Re-analysis**:
   - Make a change in Calculator.java
   - Save file (`Ctrl+S`)
   - Watch instant re-analysis (< 300ms)

---

## 📞 NEED HELP?

If extension still not working after reload, provide:
1. Output logs (Ctrl+Shift+U → Pragmite)
2. VSCode Developer Tools console (Help → Toggle Developer Tools → Console tab)
3. Screenshot of Extensions panel (Ctrl+Shift+X)

---

**Now reload VSCode and test! 🚀**
