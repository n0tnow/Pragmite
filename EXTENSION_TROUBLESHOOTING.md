# 🔧 Pragmite Extension Görünmüyor - Sorun Giderme

## Sorun: VSCode'da Pragmite extension'ını göremiyorum

### Çözüm 1: Extension Kurulu mu Kontrol Et

Terminal'de:
```bash
code --list-extensions | grep -i pragmite
```

**Beklenen çıktı:**
```
pragmite.pragmite
```

**Çıktı yoksa:**
```bash
cd C:\Pragmite\pragmite-vscode-extension
code --install-extension pragmite-1.0.2.vsix --force
```

### Çözüm 2: VSCode'u Tamamen Kapat ve Aç

1. **TÜM** VSCode pencerelerini kapatın
2. Task Manager'da "Code.exe" süreçlerini kontrol edin
3. Varsa sonlandırın
4. VSCode'u yeniden açın

### Çözüm 3: VSCode Extensions Klasörünü Kontrol Et

Windows'ta extension'lar buraya kurulur:
```
C:\Users\<USERNAME>\.vscode\extensions\
```

Kontrol:
```bash
dir "C:\Users\%USERNAME%\.vscode\extensions" | findstr pragmite
```

**Görmelisiniz:**
```
pragmite.pragmite-1.0.2
```

**Görmüyorsanız:**
```bash
# Extension klasörünü listeleyin
dir "C:\Users\%USERNAME%\.vscode\extensions"

# Pragmite klasörü yoksa tekrar kurun
code --install-extension C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.2.vsix --force
```

### Çözüm 4: Farklı VSCode Instance'ı

Farklı kullanıcı profili veya portable VSCode kullanıyorsanız:

**Portable VSCode için:**
```bash
# Portable VSCode'un yolu
cd "C:\VSCode-Portable"

# Extension'ı portable VSCode'a kur
.\Code.exe --install-extension C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.2.vsix
```

**Insider/Exploration Build için:**
```bash
code-insiders --install-extension C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.2.vsix
```

### Çözüm 5: VSCode Extensions Panelinde Arama

VSCode'da:
```
1. Ctrl+Shift+X
2. Arama kutusuna: "pragmite"
3. Filtreler:
   - @installed pragmite
   - @enabled pragmite
   - @disabled pragmite
```

**Disabled olarak görüyorsanız:**
- Sağ tıklayın → "Enable"

### Çözüm 6: VSCode Output Logs

```
1. Ctrl+Shift+U (Output paneli)
2. Dropdown'dan "Extension Host" seçin
3. Pragmite ile ilgili hata var mı kontrol edin
```

**Örnek hata:**
```
Extension 'pragmite.pragmite' failed to activate
```

**Çözüm:**
```bash
# Extension'ı kaldır
code --uninstall-extension pragmite.pragmite

# Tekrar kur
code --install-extension C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.2.vsix --force

# VSCode'u reload et
Ctrl+Shift+P → "Developer: Reload Window"
```

### Çözüm 7: Java Yüklü mü?

Pragmite, Java gerektirir:

```bash
java -version
```

**Çıktı olmalı:**
```
java version "11.0.x" veya üzeri
```

**Java yoksa:**
1. OpenJDK 11+ yükleyin
2. VSCode'u restart edin

### Çözüm 8: Extension Settings

`settings.json` kontrol edin:

```
Ctrl+Shift+P → "Preferences: Open User Settings (JSON)"
```

Ekleyin:
```json
{
  "pragmite.enabled": true,
  "pragmite.analyzeOnSave": true,
  "pragmite.javaPath": "java"
}
```

### Çözüm 9: Başka Workspace'te Test Et

```bash
# Test projesini açın
code C:\Pragmite\pragmite-test-project

# Extensions panelinde kontrol edin
Ctrl+Shift+X → "Pragmite"
```

### Çözüm 10: Developer Tools ile Debug

```
Ctrl+Shift+P → "Developer: Toggle Developer Tools"
```

**Console tab'ında kontrol:**
```javascript
// Extension yüklenmiş mi?
vscode.extensions.getExtension('pragmite.pragmite')
```

**Çıktı:**
```
Extension {id: 'pragmite.pragmite', ...}
```

**undefined ise:**
- Extension kurulmamış veya aktif değil
- Yeniden kurun

---

## ✅ Başarılı Kurulum Kontrol Listesi

- [ ] `code --list-extensions` → pragmite.pragmite görünüyor
- [ ] VSCode Extensions panelinde "Pragmite" aratınca görünüyor
- [ ] `Ctrl+Shift+P` → "Pragmite" yazınca komutlar çıkıyor
- [ ] Java kurulu: `java -version` çalışıyor
- [ ] Test projesinde çalışıyor
- [ ] Dashboard açılıyor: http://localhost:3745

---

## 📞 Hala Çalışmıyor?

### Son Çare: Temiz Kurulum

```bash
# 1. Extension'ı tamamen kaldır
code --uninstall-extension pragmite.pragmite

# 2. Extension klasörünü manuel sil
rmdir /s "C:\Users\%USERNAME%\.vscode\extensions\pragmite.pragmite-1.0.2"

# 3. VSCode'u tamamen kapat (Task Manager'dan de kontrol et)

# 4. VSCode'u aç

# 5. Yeniden kur
cd C:\Pragmite\pragmite-vscode-extension
code --install-extension pragmite-1.0.2.vsix --force

# 6. VSCode'u reload et
# Ctrl+Shift+P → "Developer: Reload Window"
```

---

## 🎯 Hızlı Test

```bash
# Terminal'de hızlı test:
code --list-extensions | findstr pragmite && echo "✅ Extension kurulu" || echo "❌ Extension kurulu değil"
```

**Çıktı:**
```
pragmite.pragmite
✅ Extension kurulu
```
