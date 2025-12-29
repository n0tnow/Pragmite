# 🔧 PRAGMITE EXTENSION ACTIVATION GUIDE

## ✅ SORUN ÇÖZÜLDÜ! (Fixed - 2025-12-02)

Extension yeniden build edildi ve eksik dosyalar eklendi:
- ✅ Compiled JavaScript files (`out/` directory)
- ✅ JAR file (`lib/pragmite-core-1.6.3.jar`)
- ✅ Extension successfully reinstalled

**SADECE ŞUNU YAPIN**: VSCode'u reload edin (Ctrl+Shift+P → "Developer: Reload Window")

Daha detaylı başlangıç rehberi için: [QUICK_START.md](QUICK_START.md)

---

## Eski Sorun: Extension Neden Çalışmıyordu?

Extension ilk paketlenmede eksik dosyalarla paketlenmişti. `.vscodeignore` dosyası `out/` ve `lib/` klasörlerini yanlışlıkla hariç tutmuştu.

Extension otomatik çalışmıyorsa bu adımları takip edin:

## 📋 Adım 1: VSCode'u Reload Edin

### Yöntem A: Command Palette
1. `Ctrl+Shift+P` tuşlarına basın
2. "Developer: Reload Window" yazın
3. Enter'a basın
4. VSCode yeniden yüklenecek

### Yöntem B: VSCode'u Yeniden Başlatın
1. VSCode'u tamamen kapatın
2. Tekrar açın
3. Test projesini açın: `File → Open Folder → C:\Pragmite\pragmite-test-project`

---

## 📋 Adım 2: Extension'ın Yüklendiğini Kontrol Edin

1. `Ctrl+Shift+X` ile Extensions panelini açın
2. Arama kutusuna "Pragmite" yazın
3. Listede görünmeli:
   ```
   Pragmite - Java Code Quality Analyzer
   v1.0.0
   ```
4. Eğer "Disable" butonu varsa → Extension aktif
5. Eğer "Enable" butonu varsa → Tıklayın

---

## 📋 Adım 3: Extension Loglarını Kontrol Edin

1. `Ctrl+Shift+U` ile Output panelini açın
2. Sağ üstteki dropdown'dan "Pragmite" seçin
3. Hata mesajları var mı kontrol edin

**Beklenen çıktı:**
```
Pragmite extension is now active!
Starting analysis: ...
Found X Java files
Analysis complete. Found Y code smells
```

---

## 📋 Adım 4: Manuel Analiz Çalıştırın

Eğer otomatik çalışmıyorsa, manuel olarak çalıştırın:

### Yöntem 1: Command Palette
1. `Ctrl+Shift+P`
2. "Pragmite: Analyze Current File" yazın
3. Enter

### Yöntem 2: Status Bar
1. Sağ alt köşede `🔬 Pragmite` ikonunu arayın
2. Tıklayın → Workspace analysis başlar

---

## 🐛 Sorun Giderme

### Problem: "Extension not found" hatası
**Çözüm:**
```bash
# Extension'ı tekrar yükleyin
cd C:\Pragmite\pragmite-vscode-extension
code --install-extension pragmite-1.0.0.vsix --force
```

### Problem: Java hatası
**Çözüm:**
```bash
# Java versiyonunu kontrol edin
java -version
# Java 21+ olmalı

# Eğer Java yok veya eski versiyon:
# Java 21 indirin: https://adoptium.net/
```

### Problem: JAR not found
**Çözüm:**
Extension içinde JAR olmalı:
```
C:\Users\[USER]\.vscode\extensions\pragmite.pragmite-1.0.0\lib\pragmite-core-1.0.0.jar
```

Kontrol edin:
```bash
ls "C:\Users\$env:USERNAME\.vscode\extensions\pragmite.pragmite-1.0.0\lib\"
```

### Problem: "Cannot find module" hatası
**Çözüm:**
Extension'ı yeniden compile edin:
```bash
cd C:\Pragmite\pragmite-vscode-extension
npm run compile
npx vsce package
code --install-extension pragmite-1.0.0.vsix --force
```

---

## ✅ Extension Çalıştığını Nasıl Anlarım?

Extension çalışıyorsa şunları görmelisiniz:

1. **Status Bar** (sağ alt):
   ```
   🔬 Pragmite
   ```

2. **Explorer Panel** (sol sidebar):
   ```
   📁 EXPLORER
   └─ 🔬 PRAGMITE RESULTS
   ```

3. **Output Panel** (`Ctrl+Shift+U`):
   ```
   [Pragmite] Pragmite extension is now active!
   ```

4. **Java dosyası açtığınızda**:
   - Sarı/kırmızı çizgiler (diagnostics)
   - Metot üstünde complexity annotations
   - Problems panel'de issues

---

## 🚀 Extension Development Mode'da Test (Advanced)

Eğer hala çalışmıyorsa, development mode'da çalıştırın:

1. VSCode'da extension projesini açın:
   ```
   File → Open Folder → C:\Pragmite\pragmite-vscode-extension
   ```

2. `F5` tuşuna basın
   - Yeni bir "Extension Development Host" penceresi açılır

3. Bu yeni pencerede test projesini açın:
   ```
   File → Open Folder → C:\Pragmite\pragmite-test-project
   ```

4. Calculator.java'yı açın
   - Şimdi çalışmalı
   - Debug console'da logları görebilirsiniz

---

## 💡 Hızlı Test Komutu

Extension çalışıp çalışmadığını test etmek için:

1. VSCode'da `Ctrl+Shift+P`
2. "Pragmite" yazın
3. 3 komut görmelisiniz:
   ```
   Pragmite: Analyze Current File
   Pragmite: Analyze Entire Workspace
   Pragmite: Show Quality Report
   ```

Eğer bu komutlar görünüyorsa extension aktif ama analiz yapmıyor demektir.

---

## 📞 Hala Çalışmıyor mu?

Bana şunları gönderin:

1. Extension Output logs:
   ```
   Ctrl+Shift+U → Pragmite dropdown
   ```

2. VSCode logs:
   ```
   Help → Toggle Developer Tools → Console tab
   ```

3. Extension listesi:
   ```bash
   code --list-extensions | grep pragmite
   ```

4. Java version:
   ```bash
   java -version
   ```
