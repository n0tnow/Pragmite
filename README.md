# Pragmite

# Java Refactoring & Complexity Assistant

> Java kod tabanları için: **(1)** statik analiz + Büyük-O (O(1), O(n), O(n log n), O(n²) …) tahmini, **(2)** güvenli otomatik refactoring, **(3)** performans profili ve kanıta dayalı iyileştirme, **(4)** Pragmatic Programmer ilkeleriyle (DRY, Orthogonality, YAGNI, Tracer Bullets) etiketlenmiş bulgular, **(5)** VS Code entegrasyonu ve Spring Boot tabanlı web arayüzü.

---

## İçindekiler
- Amaç ve Özet
- Temel Özellikler
- Çalışma Prensibi (Uçtan Uca Akış)
- Mimari ve Modüller
- Kurulum ve Gereksinimler
- Kullanım (VS Code, Web UI, CLI/CI)
- Analiz Kuralları (Kod Kokuları) ve Otomatik Düzeltmeler
- Büyük-O (Karmaşıklık) Etiketleri
- Performans Ölçümü ve Öneriler (JFR & JMH)
- Skorlama Modeli (DRY, Orthogonality, Correctness, Perf, Pragmatic Score)
- Yapılandırma (Config)
- Güvenlik Kafesi (Build/Test/Revert) ve Kalite Kapıları
- Sınırlılıklar ve Kapsam Dışı
- Yol Haritası (Öneri)
- Katkı ve Lisans

---

## Amaç ve Özet
Bu proje, Java projelerinde **kod kalitesini** ve **çalışma zamanı verimliliğini** ölçülebilir biçimde artırmayı hedefleyen bir yardımcıdır. Kod yazarken (VS Code içinde) ve sonrasında (Web UI, CLI/CI) çalışan araç; kodun yapısından **Büyük-O** tahmini çıkarır, **kod kokularını** saptar, **güvenli otomatik refactoring** uygular, **JFR** ile gerçek çalışma zamanı profili ve **JMH** ile mikro-benchmark sonuçlarını toplayarak **kanıta dayalı** performans önerileri sunar. Tüm bulguları **Pragmatic Programmer** ilkeleri (DRY, Orthogonality, YAGNI, Tracer Bullets) ile etiketler ve **skor panosunda** gösterir.

---

## Temel Özellikler
- Statik analiz: Döngüler, stream zincirleri ve koleksiyon işlemleri üzerinden **zaman/alan karmaşıklığı** etiketi (Büyük-O) üretimi; **kod kokuları** tespiti.
- Güvenli otomatik refactoring (tek tık): Introduce Constant, try-with-resources, unused import/parameter temizliği, döngü içi String→StringBuilder, diamond operator, risksiz inline/extract variable.
- Performans: **JFR** ile hotspot profili (total/self time, çağrı sayısı), **JMH** şablon üretimi ve önce/sonra kıyası; kanıta dayalı performans önerileri.
- UI: **VS Code** eklentisi (LSP/Code Actions) ve **Spring Boot** tabanlı web dashboard (bulgu kartları, diff önizleme, Apply Fix, skor grafikleri, flamegraph/call tree).
- Skorlar: **DRY**, **Orthogonality**, **Correctness**, **Perf** ve birleşik **Pragmatic Score**.
- Kodu bozmama: Fix sonrası **build/test**, başarısızlıkta **otomatik revert**; CI’da **kalite kapıları**.

---

## Çalışma Prensibi (Uçtan Uca Akış)
1. Analiz başlatılır; AST + sembol çözümleme ile döngüler/streamler/koleksiyon çağrıları ve bilinen maliyetleri taranır.
2. Fonksiyon başlarına **Büyük-O etiketi** eklenir; kod kokuları listelenir.
3. Uygunsa **tek tık** otomatik refactor önerileri gösterilir; kullanıcı onaylarsa patch uygulanır.
4. Her değişiklikten sonra **derleme ve (varsa) testler** çalışır; hata olursa **revert** edilir.
5. İsteğe bağlı profil çalıştırılır (**JFR**); sıcak metotlar ve çağrı ağaçları toplanır.
6. Sıcak noktalarda **JMH** şablonları üretilir; önce/sonra kıyas sonucu **performans önerileri** doğrulanır.
7. Web UI’da tüm bulgular kartlar hâlinde, **diff önizleme**, **Apply Fix** ve **skor panosu** ile görüntülenir.

---

## Mimari ve Modüller
- analyzer/ : AST + sembol çözümleyici; karmaşıklık tahmini ve kokular.
- rules/ : Analiz kuralları ve QuickFix’ler (Introduce Constant, TWR, vb.).
- engine/ : Değişiklik uygulayıcı; build/test; otomatik revert; diff üretici.
- runtime/ : JFR profil toplayıcı; JMH şablon üretici/koşucu.
- scoring/ : DRY, Orthogonality, Correctness, Perf ve Pragmatic Score hesaplayıcıları.
- ui-web/ : Spring Boot tabanlı web arayüzü (bulgu kartları, grafikler, flamegraph).
- connectors/ : VS Code (LSP/Code Actions), CLI/CI entegrasyonları.
- persistence/ (opsiyonel) : Dosya/JSON veya DB (Oracle/PostgreSQL) ile sonuç/metryk arşivi.

---

## Kurulum ve Gereksinimler
- JDK 21 (gerekli)
- Maven veya Gradle
- Node.js (web arayüzü için; basit frontend derlemesi)
- VS Code (eklenti kurulumu için)
- İsteğe bağlı: Oracle/PostgreSQL (sonuçların kalıcı arşivi için)

Örnek derleme (Gradle):
$ ./gradlew clean build

Web UI başlatma:
$ ./gradlew :ui-web:bootRun

VS Code eklentisi (LSP istemcisi) paketleme:
$ cd connectors/vscode-extension && npm install && npm run package

CLI yardımı:
$ ./gradlew :cli:run --args="--help"

---

## Kullanım
- VS Code:
  - Komut paleti: Analyze Project, Show Complexity Map, Apply Safe Refactors.
  - Editörde hover/CodeLens: Fonksiyon başında “O(n log n) • space O(n)”.
  - Code Action: Apply Fix, Show Diff.
- Web UI (Spring Boot):
  - Giriş ekranı → Proje seçimi → Analiz çalıştır.
  - Bulgu kartları (etiket, risk, etki, kaynak satır) → Diff önizleme → Apply Fix.
  - Skor panosu: DRY/Orthogonality/Correctness/Perf ve önce/sonra grafikleri.
  - Performans sekmesi: Hotspot tablosu, flamegraph/call tree.
- CLI/CI:
  - analyze, autofix --dry-run, profile --jfr, bench --jmh komutları.
  - CI kalite kapıları (eşik altı değerlerde pipeline durdurma).

---

## Analiz Kuralları (Kod Kokuları) ve Otomatik Düzeltmeler
- Kokular:
  - Duplicated code (DRY ihlali) — yakın kopya tespiti.
  - Long method / Large class.
  - Long parameter list / Data clumps.
  - Magic number / magic string.
  - Dead code, unused import, unused parameter.
  - Döngü içi String birleştirme (sıcak yolda maliyetli).
  - Yanlış try-finally (TWR adayı), gereksiz synchronized.
  - (Opsiyonel uyarı) Yanlış Optional kullanımı, equals/hashCode uyumsuzluğu.
- Otomatik refactor (güvenli alt küme):
  - Introduce Constant (magic literal → private static final).
  - Try-with-resources (AutoCloseable güvenliyse).
  - Unused import/parameter temizliği.
  - Döngü içi String → StringBuilder (özellikle sıcak yol).
  - Diamond operator; uygun yerde var (Java sürümüne bağlı).
  - Inline/Extract variable (yalnız tek kullanım ve yan etkisizse).
- Performans önerileri (kanıtla):
  - List.contains döngüsü → HashSet’e dönüştür (O(n²) → O(n)).
  - Sık regex → Pattern.compile cache.
  - Sıcak patikada ağır stream zinciri → eşdeğer döngü (JMH ile ≥%10 kazanç şartı).
  - Boxing/Unboxing azaltma; computeIfAbsent kullanımı; buffer boyutu iyileştirme.

---

## Büyük-O (Karmaşıklık) Etiketleri
- Döngüler: for/while/foreach ve iç içe döngüler → O(n), O(n·m), O(n²) …
- Streams: map/filter ≈ O(n), distinct ≈ O(n) (amortize), sorted ≈ O(n log n) → zincirde baskın terim etiketi.
- Koleksiyonlar (ortalama):
  - ArrayList add amortize O(1); random access O(1); contains O(n).
  - HashMap/HashSet get/put/contains amortize O(1); kötü durumda daha yüksek olabilir.
  - TreeMap/TreeSet temel işlemler O(log n).
- Fonksiyon başında etiketi ve “kaynak satırını” gösterecek CodeLens/hover.

Not: Özyineleme/böl-ve-fethet tespiti **Faz 2**’de eklenecektir (lineer recursion, master-teoremi kalıpları, öneriler: tail-recursion→loop, memoization).

---

## Performans Ölçümü ve Öneriler (JFR & JMH)
- JFR (Java Flight Recorder):
  - Düşük overhead ile gerçek çalışma zamanı profili (total/self time, çağrı sayısı, tahsisatlar).
  - Hotspot tablosu, call tree/flamegraph; önce/sonra karşılaştırması.
- JMH (Java Microbenchmark Harness):
  - Sıcak metotlara otomatik benchmark sınıfı üretimi; ısınma, tekrar, istatistik.
  - Önce/sonra kıyası; güvenilir hızlanma kanıtı.
- Uygulama ilkesi:
  - Performans refactor’ları, **JMH ile anlamlı kazanç** (varsayılan ≥%10) elde edilmeden otomatik uygulanmaz.
  - Tüm değişiklikler build/test’ten geçer; aksi halde revert edilir.

---

## Skorlama Modeli
- DRY Score = 1 − duplikasyon oranı.
- Orthogonality Score = 1 − normalize edilmiş bağımlılık/cycle endeksi (düşük coupling, yüksek cohesion hedefi).
- Correctness Score = 1 − (ağırlıklı ihlal yoğunluğu / KLoC).
- Perf Score = İyileşen hotspot yüzdesi ve/veya JMH hızlanma oranı.
- Pragmatic Score = ayarlanabilir ağırlıklarla birleşik puan (örnek: 0.3 DRY + 0.3 Ortho + 0.25 Correct + 0.15 Perf).
- Skor panosu: modül bazlı ısı haritaları, trend grafikleri, önce/sonra kartları.

---

## Yapılandırma (Config)
- Kuralların aç/kapa durumu (enable/disable) ve ciddiyet seviyesi.
- Büyük-O tahmininde varsayımlar (örn. hash işlemleri amortize O(1) kabulü).
- Skor ağırlıkları ve CI eşikleri (ör. DRY ≥ %85, Ortho ≥ %70).
- JFR profil profili (event set, süre) ve JMH ayarları (fork/warmup/iterations).
- Arşiv: JSON/dosya veya DB (Oracle/PostgreSQL) bağlantısı.

---

## Güvenlik Kafesi ve Kalite Kapıları
- Her otomatik refactor sonrası:
  - Derleme ve mevcut testlerin koşturulması.
  - Başarısızlıkta otomatik revert ve raporlama.
- CI’da kalite kapıları:
  - Skor eşiklerinin altına düşerse pipeline fail.
  - Performans fix’leri yalnız JMH kanıtı varsa kabul.

---

## Sınırlılıklar ve Kapsam Dışı
- Büyük mimarî yeniden tasarım (class split, domain re-design) otomatik yapılmaz.
- Derin öğrenme tabanlı kod dönüştürme yoktur (isteğe bağlı fazda hafif ML eklenebilir).
- En kötü durum karmaşıklığı (ör. hash çakışmaları) varsayılan olarak amortize kabul edilir; bu varsayımlar yapılandırılabilir.

---

## Yol Haritası (Öneri)
- Faz 1 (MVP, 6–7 hafta):
  - Büyük-O (döngüler/streams/koleksiyonlar), temel kokular, güvenli auto-fix seti.
  - VS Code LSP; Web UI v1 (bulgu kartı, diff, Apply Fix, skor panosu).
  - JFR profil + JMH şablonları; performans önerilerinin ilk seti.
- Faz 2 (3–4 hafta):
  - Özyineleme/böl-ve-fethet tanıma ve öneriler (tail-recursion→loop, memoization).
  - Hafif ML: öneri sıralayıcı; JFR zaman serisinde regresyon alarmı.
  - Kural/fix kataloğunun genişletilmesi.

---

## Katkı ve Lisans
- Katkı: Issue/PR açmadan önce kurallar kataloğunu ve kod tarz rehberini okuyun; her yeni kural için örnekler ve negatif testler beklenir.
- Lisans: Açık kaynak (Apache-2.0 önerilir). Kurumsal özellikler için ayrı paket planlanabilir.

---
