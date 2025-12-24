# 🚀 VSCode Marketplace'e Yayınlama Rehberi

## Neden Şu An Marketplace'de Göremiyorsunuz?

Extension **local olarak kurulu** (VSIX dosyasından). Marketplace'de görmek için **yayınlamanız** gerekiyor.

---

## 📋 Marketplace'e Yayınlama Adımları

### Adım 1: Azure DevOps Hesabı Oluşturun

1. **Azure DevOps**'a gidin: https://dev.azure.com
2. Microsoft hesabınızla giriş yapın
3. Yeni bir organization oluşturun (örn: "pragmite-org")

### Adım 2: Personal Access Token (PAT) Oluşturun

1. Azure DevOps'ta sağ üst köşedeki **User Settings** → **Personal Access Tokens**
2. **+ New Token** tıklayın
3. Token ayarları:
   - **Name**: `vscode-marketplace`
   - **Organization**: `All accessible organizations`
   - **Expiration**: 90 gün (veya özel)
   - **Scopes**: **Custom defined** seçin
   - **Marketplace**: `Acquire`, `Manage` seçin
4. **Create** tıklayın
5. **Token'ı kopyalayın** - bir daha göremezsiniz!

### Adım 3: Visual Studio Marketplace Publisher Oluşturun

1. https://marketplace.visualstudio.com/manage adresine gidin
2. Microsoft hesabınızla giriş yapın
3. **Create Publisher** tıklayın
4. Publisher bilgileri:
   - **ID**: `pragmite` (küçük harf, tire kullanabilirsiniz)
   - **Name**: `Pragmite`
   - **Email**: Sizin email adresiniz
5. **Create** tıklayın

### Adım 4: vsce ile Publisher'a Login Olun

Terminal'de:

```bash
cd /c/Pragmite/pragmite-vscode-extension

# vsce ile login
npx vsce login pragmite

# Prompt gelince PAT token'ınızı yapıştırın
```

**Örnek**:
```
Personal Access Token for publisher 'pragmite': ***************
The Personal Access Token verification succeeded for the publisher 'pragmite'.
```

### Adım 5: Extension'ı Yayınlayın

```bash
# Yayınlama
npx vsce publish

# Veya minor version bump ile
npx vsce publish minor

# Veya major version bump ile
npx vsce publish major
```

**Çıktı**:
```
Publishing pragmite.pragmite@1.0.2...
Successfully published pragmite.pragmite@1.0.2!
Your extension will live at https://marketplace.visualstudio.com/items?itemName=pragmite.pragmite
```

### Adım 6: Marketplace'de Kontrol Edin

1. https://marketplace.visualstudio.com adresine gidin
2. Arama kutusuna "Pragmite" yazın
3. Extension'ınızı göreceksiniz!

**VSCode'da görme**:
```
1. VSCode'u açın
2. Ctrl+Shift+X (Extensions)
3. Arama: "Pragmite"
4. Artık Marketplace'de görünüyor!
```

---

## ⚠️ Yayınlamadan Önce Kontroller

### 1. README.md Oluşturun

```bash
cd /c/Pragmite/pragmite-vscode-extension
```

`README.md` dosyası oluşturun (Marketplace sayfasında görünür):

```markdown
# Pragmite - Java Code Quality Analyzer

Real-time Java code quality analysis with Big-O complexity detection, 31 code smell detectors, and Live Dashboard.

## Features

- **31 Code Smell Detectors**: Detect common anti-patterns
- **Big-O Complexity Analysis**: Identify performance bottlenecks
- **Live Dashboard**: Real-time monitoring at http://localhost:3745
- **Instant Updates**: SSE-based instant refresh
- **VSCode Integration**: Click to jump to issues

## Installation

1. Install the extension from VSCode Marketplace
2. Open a Java project
3. Extension activates automatically
4. Dashboard opens at http://localhost:3745

## Usage

- **Analyze File**: `Ctrl+Shift+P` → "Pragmite: Analyze Current File"
- **Analyze Workspace**: `Ctrl+Shift+P` → "Pragmite: Analyze Entire Workspace"
- **Open Dashboard**: `Ctrl+Shift+P` → "Pragmite: Open Live Dashboard"
- **View Report**: `Ctrl+Shift+P` → "Pragmite: Show Quality Report"

## Requirements

- Java 11 or higher
- VSCode 1.106.0 or higher

## Extension Settings

- `pragmite.enabled`: Enable/disable Pragmite analysis
- `pragmite.analyzeOnSave`: Analyze file on save (default: true)
- `pragmite.javaPath`: Path to Java executable

## License

MIT
```

### 2. LICENSE Dosyası Ekleyin

```bash
# MIT License ekleyin
cat > LICENSE.txt << 'EOF'
MIT License

Copyright (c) 2025 Pragmite

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
EOF
```

### 3. .vscodeignore Kontrol Edin

`.vscodeignore` dosyasında gereksiz dosyaları hariç tutun:

```
.vscode/**
.vscode-test/**
src/**
.gitignore
tsconfig.json
**/.eslintrc.json
**/*.map
**/*.ts
!out/**/*.js
!lib/**
node_modules/**
*.vsix
```

### 4. package.json Metadata Ekleyin

`package.json` dosyasına icon, badges, vb. ekleyin:

```json
{
  "name": "pragmite",
  "displayName": "Pragmite - Java Code Quality Analyzer",
  "description": "Real-time Java code quality analysis with Big-O complexity detection, 31 code smell detectors, and Live Dashboard",
  "version": "1.0.2",
  "publisher": "pragmite",
  "icon": "images/icon.png",
  "galleryBanner": {
    "color": "#000000",
    "theme": "dark"
  },
  "badges": [
    {
      "url": "https://img.shields.io/badge/license-MIT-blue.svg",
      "href": "https://github.com/pragmite/pragmite-vscode/blob/main/LICENSE",
      "description": "License: MIT"
    }
  ],
  "homepage": "https://github.com/pragmite/pragmite-vscode",
  "bugs": {
    "url": "https://github.com/pragmite/pragmite-vscode/issues"
  },
  "repository": {
    "type": "git",
    "url": "https://github.com/pragmite/pragmite-vscode"
  },
  "license": "MIT"
}
```

---

## 🔄 Güncelleme Yayınlama

Yeni versiyon yayınlamak için:

```bash
# package.json'daki version'ı güncelleyin (örn: 1.0.3)
# Veya otomatik bump:

npx vsce publish patch  # 1.0.2 -> 1.0.3
npx vsce publish minor  # 1.0.2 -> 1.1.0
npx vsce publish major  # 1.0.2 -> 2.0.0
```

---

## 📊 İstatistikler Görme

Yayınladıktan sonra:

1. https://marketplace.visualstudio.com/manage/publishers/pragmite
2. İstatistikleri görün:
   - İndirme sayısı
   - Kurulum sayısı
   - Değerlendirmeler

---

## ⚠️ Sorun Giderme

### "Error: Failed to publish"

**Çözüm 1**: PAT token kontrolü
```bash
npx vsce login pragmite
# Token'ı tekrar girin
```

**Çözüm 2**: Versiyon kontrolü
```bash
# package.json'da version'ı artırın
"version": "1.0.3"
```

### "Error: Publisher 'pragmite' not found"

Marketplace'de publisher oluşturun:
https://marketplace.visualstudio.com/manage/createpublisher

---

## 🎯 Özet

1. ✅ Azure DevOps hesabı oluştur
2. ✅ PAT token al
3. ✅ Marketplace publisher oluştur
4. ✅ `vsce login pragmite`
5. ✅ `vsce publish`
6. ✅ Marketplace'de görün!

**Local installation şu an yeterli mi?**
Evet! VSIX dosyası ile başka VSCode'lara kurabilirsiniz:
```bash
code --install-extension pragmite-1.0.2.vsix
```

**Marketplace'e ihtiyaç var mı?**
- Evet: Herkese açık kullanıma sunmak için
- Hayır: Sadece kendiniz veya ekip için kullanacaksanız

---

**Şu an extension çalışıyor mu?** ✅ Evet!
**Marketplace'de görmek zorunlu mu?** ❌ Hayır, isteğe bağlı!

Marketplace'e yayınlamak isterseniz yukarıdaki adımları izleyin.
