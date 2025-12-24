# VSCode Marketplace Publishing Guide - Pragmite v1.0.7

## 🎯 Prerequisites

1. **Microsoft Account:** You need a Microsoft account
2. **Azure DevOps Account:** Create at https://dev.azure.com
3. **Personal Access Token (PAT):** Required for `vsce` authentication

---

## 📋 Step-by-Step Publishing

### Step 1: Create Azure DevOps Organization

1. Go to https://dev.azure.com
2. Click "Start free"
3. Create a new organization (e.g., "pragmite-org")
4. Name: `pragmite` or your preferred name

### Step 2: Create Personal Access Token (PAT)

1. In Azure DevOps, click your profile icon (top right)
2. Click **"Personal access tokens"**
3. Click **"+ New Token"**
4. Configure:
   - **Name:** `vsce-publisher-token`
   - **Organization:** All accessible organizations
   - **Expiration:** 90 days (or custom)
   - **Scopes:**
     - Select **"Custom defined"**
     - Check **"Marketplace" → "Manage"**
5. Click **"Create"**
6. **IMPORTANT:** Copy the token immediately (it won't be shown again!)

```
Example PAT format: a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7
```

### Step 3: Create Publisher on Marketplace

1. Go to https://marketplace.visualstudio.com/manage
2. Sign in with your Microsoft account
3. Click **"Create publisher"**
4. Fill in:
   - **Publisher ID:** `pragmite` (must be unique, lowercase, no spaces)
   - **Display Name:** `Pragmite`
   - **Email:** Your verified email
   - **Description:** "Java code quality analysis tools"
5. Click **"Create"**

**IMPORTANT:** The Publisher ID in `package.json` must match:
```json
{
  "publisher": "pragmite"
}
```

### Step 4: Login to vsce

Open terminal in `pragmite-vscode-extension` folder:

```bash
cd /c/Pragmite/pragmite-vscode-extension

# Login with your PAT
npx vsce login pragmite
```

When prompted, paste your Personal Access Token.

### Step 5: Publish Extension

```bash
# Make sure you're in the extension directory
cd /c/Pragmite/pragmite-vscode-extension

# Publish to marketplace
npx vsce publish
```

The command will:
1. Validate package.json
2. Package the extension (if not already packaged)
3. Upload to Visual Studio Marketplace
4. Display the extension URL

**Alternative: Publish from VSIX file**
```bash
npx vsce publish --packagePath pragmite-1.0.7.vsix
```

### Step 6: Verify Publication

1. Go to https://marketplace.visualstudio.com/items?itemName=pragmite.pragmite
2. Verify:
   - ✅ Extension appears in search
   - ✅ Version is 1.0.7
   - ✅ Description is correct
   - ✅ README displays properly
   - ✅ Changelog shows recent updates

### Step 7: Install from Marketplace

In VSCode:
1. Press `Ctrl+Shift+X` (Extensions)
2. Search: **"Pragmite"**
3. Click **"Install"**
4. Reload window: `Ctrl+Shift+P` → "Developer: Reload Window"

---

## 🔄 Updating Extension (Future Versions)

For future updates:

```bash
# 1. Update version in package.json
# 2. Update CHANGELOG.md
# 3. Rebuild and package
npm run compile
npx vsce package

# 4. Publish
npx vsce publish
```

Or publish with automatic version bump:
```bash
npx vsce publish patch  # 1.0.7 → 1.0.8
npx vsce publish minor  # 1.0.7 → 1.1.0
npx vsce publish major  # 1.0.7 → 2.0.0
```

---

## 📦 Files Required for Publishing

Ensure these files exist:
- ✅ `package.json` (with correct publisher ID)
- ✅ `README.md` (will be shown on marketplace)
- ✅ `CHANGELOG.md` (version history)
- ✅ `LICENSE` (MIT license)
- ✅ `.vscodeignore` (excludes unnecessary files from package)

---

## 🐛 Troubleshooting

### Error: "Publisher 'pragmite' not found"
**Solution:** Create the publisher on https://marketplace.visualstudio.com/manage first

### Error: "Failed to authenticate"
**Solution:**
1. Generate a new PAT with "Marketplace (Manage)" scope
2. Run `npx vsce login pragmite` again

### Error: "Version already exists"
**Solution:**
1. Update version in `package.json`
2. Run `npx vsce package` again

### Warning: "ENOENT: no such file or directory, open 'README.md'"
**Solution:** Create a README.md in the extension root (already exists ✅)

### Extension not appearing in search
**Solution:**
1. Wait 5-10 minutes (indexing delay)
2. Clear VSCode extension cache
3. Check marketplace website directly

---

## 📝 Package.json Configuration

Current configuration (v1.0.7):

```json
{
  "name": "pragmite",
  "displayName": "Pragmite - Java Code Quality Analyzer",
  "description": "Real-time Java code quality analysis with Big-O complexity detection, 31 code smell detectors, and Live Dashboard",
  "version": "1.0.7",
  "publisher": "pragmite",
  "engines": {
    "vscode": "^1.106.0"
  },
  "categories": [
    "Linters",
    "Programming Languages",
    "Formatters"
  ],
  "keywords": [
    "java",
    "code quality",
    "static analysis",
    "complexity",
    "code smells",
    "refactoring"
  ]
}
```

---

## 🎨 Marketplace Assets (Optional)

Add these to improve marketplace appearance:

1. **Icon:** `icon.png` (128x128 px)
   ```json
   {
     "icon": "icon.png"
   }
   ```

2. **Gallery Banner:** Background color
   ```json
   {
     "galleryBanner": {
       "color": "#1e293b",
       "theme": "dark"
     }
   }
   ```

3. **Repository Link:** Already configured ✅
   ```json
   {
     "repository": {
       "type": "git",
       "url": "https://github.com/n0tnow/Pragmite"
     }
   }
   ```

---

## ✅ Post-Publication Checklist

After publishing:
- [ ] Verify extension appears in marketplace
- [ ] Test installation from marketplace
- [ ] Check README renders correctly
- [ ] Verify changelog shows v1.0.7
- [ ] Test all commands work after marketplace install
- [ ] Update GitHub Release Notes with marketplace link
- [ ] Share on social media / forums (optional)

---

## 🔗 Important Links

- **Marketplace Management:** https://marketplace.visualstudio.com/manage
- **Azure DevOps:** https://dev.azure.com
- **Your Extension URL:** https://marketplace.visualstudio.com/items?itemName=pragmite.pragmite
- **Publisher Guide:** https://code.visualstudio.com/api/working-with-extensions/publishing-extension
- **vsce Documentation:** https://github.com/microsoft/vsce

---

## 🚀 Quick Publish Command (After Initial Setup)

```bash
# One-liner to publish (after vsce login)
cd /c/Pragmite/pragmite-vscode-extension && npx vsce publish
```

---

**Current Version:** 1.0.7
**Status:** Ready to publish ✅
**Package Location:** `pragmite-vscode-extension/pragmite-1.0.7.vsix`
