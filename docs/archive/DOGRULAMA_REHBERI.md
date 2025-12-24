# ✅ PRAGMITE v1.0.1 DOĞRULAMA REHBERİ

## 🎯 Hızlı Test Adımları

### 1. Extension Kurulumu Doğrulama (30 saniye)

```bash
# Terminal'de:
code --list-extensions | grep pragmite
# Beklenen çıktı: pragmite.pragmite@1.0.1
```

**VSCode'da:**
1. `Ctrl+Shift+X` → Extensions paneli
2. "Pragmite" ara
3. Görünmeli: **Pragmite - Java Code Quality Analyzer v1.0.1**
4. "Disable" butonu varsa → Extension aktif ✅

---

### 2. Live Dashboard Doğrulama (1 dakika)

**Otomatik Açılış:**
1. VSCode'u reload edin: `Ctrl+Shift+P` → `Developer: Reload Window`
2. 2-3 saniye sonra notification görmelisiniz:
   ```
   🌐 Pragmite Dashboard is live at http://localhost:3745
   [Open Dashboard] [Dismiss]
   ```
3. "Open Dashboard" tıklayın
4. Browser'da dashboard açılmalı

**Manuel Açılış:**
1. `Ctrl+Shift+P`
2. `Pragmite: Open Live Dashboard` yazın
3. Enter
4. Browser'da açılmalı: `http://localhost:3745`

**Dashboard Kontrolleri:**
- [ ] Mor-pembe gradient arka plan ✅
- [ ] "🔬 Pragmite Live Dashboard" başlığı ✅
- [ ] "CANLI" yazısı yanıp sönüyor ✅
- [ ] "Veri Yükleniyor..." mesajı (henüz analiz yapılmadıysa) ✅

---

### 3. Analiz ve Dashboard Güncelleme (2 dakika)

**Test Projesi Aç:**
```
File → Open Folder → C:\Pragmite\pragmite-test-project
```

**Workspace Analiz Et:**
```
Ctrl+Shift+P → "Pragmite: Analyze Entire Workspace" → Enter
```

**Beklenen Sonuç (3 saniye içinde):**
1. Notification: "Analysis complete! Score: 72/100 (C), Found 30 code smells in 2 files"
2. Dashboard otomatik güncellenir
3. Status bar: `🔬 30 issues, 3 high complexity`

**Dashboard'da Görülmesi Gerekenler:**

✅ **İstatistik Kartları** (8 adet):
- Kalite Skoru: 72/100 (C Sınıfı) - Dairesel progress animasyonlu
- Dosya Sayısı: 2
- Kod Satırı: 225
- Code Smell: 30
- Critical: 2
- Major: 8
- Minor: 20
- Analiz Süresi: ~300-500ms

✅ **En Sık Code Smell Tipleri** (Top 10):
- STRING_CONCAT_IN_LOOP: 6 adet
- MAGIC_NUMBER: 5 adet
- UNUSED_VARIABLE: 3 adet
- (vb...)

Renkli bar chartlar görünmeli ✅

✅ **Detaylı Code Smell Listesi**:
6 kolonlu tablo:
| Tip | Severity | Dosya | Satır | Açıklama | 💡 Çözüm |
|-----|----------|-------|-------|----------|---------|
| STRING_CONCAT_IN_LOOP | MAJOR (turuncu) | Calculator.java | 109 | String concat... | StringBuilder kullan |
| EMPTY_CATCH_BLOCK | CRITICAL (kırmızı) | Calculator.java | 101 | Boş catch... | Exception'ı logla |

**Çözüm kolonu** yeşil italik olmalı ✅

---

### 4. Hover Tooltips Doğrulama (1 dakika)

**Calculator.java Aç:**
```
src/main/java/com/example/Calculator.java
```

**Satır 5'e git** (Unused import):
1. Sarı çizginin üzerine mouse ile gel
2. 1-2 saniye bekle
3. Tooltip açılmalı:

```
[UNUSED_IMPORT] Unused import: java.io.IOException

💡 Çözüm: Kullanılmayan import'u silin
```

"💡 Çözüm:" yazısı görünüyorsa ✅

**Satır 37'ye git** (O(n²) complexity):
1. Metot imzasına gel
2. Tooltip:

```
Yüksek karmaşıklık (O_N_SQUARED) - 'multiplyMatrices' metodu
İç içe 3 döngü var

💡 Çözüm: İç içe döngüleri azalt, daha verimli veri yapıları
kullan (HashMap, HashSet)
```

Karmaşıklığa özel öneri görünüyorsa ✅

---

### 5. HTML Report Doğrulama (1 dakika)

**Raporu Aç:**
```
Ctrl+Shift+P → "Pragmite: Show Quality Report" → Enter
```

**Kontrol Listesi:**
- [ ] Yeni tab açıldı ✅
- [ ] Başlık: "🔬 Pragmite Kod Kalite Raporu" ✅
- [ ] Türkçe metinler (Dosya Sayısı, Toplam Satır, vb.) ✅
- [ ] Özet tablosu var ✅
- [ ] "📋 En Sık Code Smell'ler" tablosu var ✅
- [ ] "🔍 Detaylı Code Smell Listesi" tablosu var ✅

**Detaylı Tablo Kontrolleri:**
- [ ] 6 kolon var (Tip, Severity, Dosya, Satır, Açıklama, 💡 Çözüm) ✅
- [ ] Severity badges renkli:
  - CRITICAL: Kırmızı arka plan
  - MAJOR: Turuncu arka plan
  - MINOR: Mavi arka plan
- [ ] **💡 Çözüm** kolonu yeşil renkte ✅
- [ ] En az 30 satır code smell var ✅

---

### 6. Real-Time Update Doğrulama (2 dakika)

**Test:**
1. Dashboard'u tarayıcıda açık bırak
2. VSCode'da Calculator.java'yı aç
3. Satır 5'teki unused import'u sil: `import java.io.IOException;`
4. `Ctrl+S` ile kaydet
5. 3 saniye bekle

**Beklenen Sonuç:**
- Dashboard otomatik güncellenir
- Code Smell sayısı: 30 → 29
- Detaylı listede o satır kaybolur

Eğer dashboard güncelleniyorsa ✅ Real-time çalışıyor!

---

### 7. Farklı VSCode'da Test (5 dakika)

**VSIX Dosyasını Kopyala:**
```bash
# Dosya konumu:
C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.1.vsix

# Başka bilgisayara kopyala veya farklı kullanıcı hesabında test et
```

**Yeni VSCode'da Kurulum:**
```bash
# Terminal'de:
cd /path/to/vsix
code --install-extension pragmite-1.0.1.vsix --force

# VSCode'u reload et:
Ctrl+Shift+P → "Developer: Reload Window"
```

**Test Et:**
1. Herhangi bir Java projesi aç
2. Dashboard notification'ı geldi mi? ✅
3. Dashboard'u aç: `Ctrl+Shift+P` → `Pragmite: Open Live Dashboard`
4. Workspace analiz et
5. Dashboard güncellendiğini gör

---

## 📋 Hızlı Doğrulama Checklist

### Kurulum
- [ ] Extension yüklü: `pragmite.pragmite@1.0.1`
- [ ] Status bar'da `🔬 Pragmite` ikonu var

### Dashboard
- [ ] Otomatik notification geliyor
- [ ] `http://localhost:3745` açılıyor
- [ ] Mor-pembe gradient arka plan
- [ ] "CANLI" yanıp sönüyor
- [ ] Analiz sonrası güncelleniyor (3 saniye)

### Özellikler
- [ ] 8 istatistik kartı görünüyor
- [ ] Kalite skoru dairesel animasyonlu
- [ ] Top 10 code smell chart var
- [ ] Detaylı tablo 6 kolonlu
- [ ] **💡 Çözüm** kolonu yeşil ve dolu

### Tooltips
- [ ] Hover'da "💡 Çözüm:" yazısı var
- [ ] Karmaşıklık önerileri özel (O(n²) için farklı öneri)

### HTML Report
- [ ] Türkçe başlıklar
- [ ] Renkli severity badges
- [ ] 6 kolonlu tablo
- [ ] **💡 Çözüm** kolonu var

---

## 🐛 Sorun Giderme

### Dashboard açılmıyor

**Çözüm 1**: Port kontrolü
```bash
netstat -ano | findstr :3745
# Kullanımda mı kontrol edin
```

**Çözüm 2**: Output logs
```
Ctrl+Shift+U → "Pragmite Dashboard" dropdown seçin
Hata mesajlarını kontrol edin
```

**Çözüm 3**: Manuel port değiştir
```
# Extension portları otomatik değiştirir (3745, 3746, 3747...)
# Notification'da gösterilen portu kullanın
```

### Suggestion'lar görünmüyor

**Çözüm**:
1. Java dosyası açık olmalı
2. Workspace analiz edilmiş olmalı
3. Mouse ile sarı çizgiye gelin
4. 2 saniye bekleyin
5. VSCode'u reload edin

### Real-time update çalışmıyor

**Çözüm**:
1. Browser'da F12 → Console → Hata var mı?
2. Dashboard'u kapatıp yeniden açın
3. VSCode'u reload edin
4. Yeni analiz çalıştırın

---

## ✅ Başarı Kriterleri

Aşağıdakilerin **hepsi** çalışıyorsa v1.0.1 doğrulanmıştır:

1. ✅ Extension kurulu ve aktif
2. ✅ Dashboard `http://localhost:3745`'te açılıyor
3. ✅ Analiz sonrası dashboard 3 saniyede güncelleniyor
4. ✅ 8 istatistik kartı doğru verileri gösteriyor
5. ✅ Top 10 code smell chart görünüyor
6. ✅ Detaylı tablo 6 kolonlu ve **💡 Çözüm** kolonu dolu
7. ✅ Hover tooltips'te "💡 Çözüm:" metni var
8. ✅ HTML rapor Türkçe ve renkli
9. ✅ Real-time update çalışıyor (save ettikten 3 saniye sonra)
10. ✅ Farklı VSCode'da da çalışıyor (VSIX portability)

---

## 🎉 Tamamlandı!

Tüm checkler ✅ ise:
- **v1.0.1 production-ready! 🚀**
- **Farklı VSCode'larda test edilebilir**
- **Dashboard gerçek zamanlı çalışıyor**
- **Kullanıcı deneyimi tam**

Başka bir Claude'a veya ekip üyesine VSIX dosyasını (`pragmite-1.0.1.vsix`) gönderin ve bu rehberi paylaşın!

---

**Son Güncelleme**: 2 Aralık 2025, 01:30
**Doğrulama Süresi**: ~10 dakika
**VSIX Dosyası**: `C:\Pragmite\pragmite-vscode-extension\pragmite-1.0.1.vsix` (8.12MB)
