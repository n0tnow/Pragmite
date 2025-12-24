# 🔬 Pragmite - Detaylı Matematiksel Analiz Raporu

## 📊 Genel Bakış
Bu rapor, Pragmite projesinde kullanılan tüm metriklerin matematiksel formüllerini, hesaplama yöntemlerini ve threshold değerlerini detaylandırır.

---

## 1️⃣ CYCLOMATIC COMPLEXITY (McCabe Karmaşıklığı)

### 📐 Matematiksel Formül
```
CC = E - N + 2P

Nerede:
- E = Graf üzerindeki kenar sayısı (edge)
- N = Graf üzerindeki düğüm sayısı (node)
- P = Bağlı bileşen sayısı (genellikle 1)

Basitleştirilmiş Formül (Karar Noktası Sayma):
CC = 1 + karar_noktaları_toplamı
```

### 🔢 Karar Noktaları
Her biri karmaşıklığa **+1** ekler:
- `if` ifadesi
- `else if` ifadesi
- `while` döngüsü
- `for` döngüsü
- `for-each` döngüsü
- `do-while` döngüsü
- `switch` içindeki her `case` (default hariç)
- `catch` bloğu
- `&&` (AND) operatörü
- `||` (OR) operatörü
- `? :` (ternary) operatörü

### 📏 Threshold Değerleri
```java
CC = 1-5    → Düşük karmaşıklık (Basit, test edilmesi kolay)
CC = 6-10   → Orta karmaşıklık (Kabul edilebilir)
CC = 11-20  → Yüksek karmaşıklık (Refactoring önerilir) ⚠️
CC > 20     → Çok yüksek karmaşıklık (Acil refactoring gerekli) 🚨
```

### 💻 Uygulama Kodu
**Dosya:** `CyclomaticComplexityCalculator.java` (satır 16-77)

```java
public static int calculate(MethodDeclaration method) {
    int complexity = 1; // Başlangıç değeri

    // Her karar noktası için +1
    complexity += if_statements_count;
    complexity += while_loops_count;
    complexity += for_loops_count;
    complexity += case_statements_count;
    complexity += catch_blocks_count;
    complexity += ternary_operators_count;
    complexity += logical_operators_count; // && ve ||

    return complexity;
}
```

### ⚠️ Mevcut Sorun
**YANLŞ POZİTİF:** Basit metotlara (CC=1-5) bile uyarı verilebiliyor.
**Çözüm:** Threshold değeri 10'dan 15'e çıkarılmalı.

---

## 2️⃣ BIG-O COMPLEXITY (Zaman Karmaşıklığı)

### 📐 Matematiksel Analiz

#### O(1) - Sabit Zaman
```
T(n) = c  (sabit)

Örnek:
int add(int a, int b) { return a + b; }
```

#### O(log n) - Logaritmik Zaman
```
T(n) = c * log₂(n)

Örnek: Binary Search
while (low <= high) {
    mid = (low + high) / 2;  // n'i ikiye böl
    if (arr[mid] == target) return mid;
    else if (arr[mid] < target) low = mid + 1;
    else high = mid - 1;
}
```

**Tespit Yöntemi:**
- Recursion'da n'i 2'ye bölme (`n/2`, `mid = (low+high)/2`)
- TreeMap/TreeSet operasyonları (`get`, `put`, `containsKey`)
- `Collections.binarySearch()`

#### O(n) - Lineer Zaman
```
T(n) = c * n

Örnek: Tek Döngü
for (int i = 0; i < n; i++) {
    // O(1) işlemler
}
```

**Tespit Yöntemi:**
- Tek seviye döngü (for, while, for-each)
- Stream operasyonları (filter, map, reduce)
- Contains, indexOf gibi lineer arama metodları

#### O(n log n) - Linearitmik Zaman
```
T(n) = c * n * log₂(n)

Örnek: Merge Sort, Quick Sort
```

**Tespit Yöntemi:**
- `Collections.sort()`, `Arrays.sort()`, `.sorted()` stream operatörü
- Döngü içinde O(log n) metot çağrısı

#### O(n²) - Quadratic Zaman
```
T(n) = c * n²

Örnek: İç İçe İki Döngü
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) {
        // O(1) işlemler
    }
}
```

**Tespit Yöntemi:**
- 2 seviye iç içe döngü
- Döngü içinde stream başlatma
- Döngü içinde lineer String metodları (toUpperCase, replace, etc.)

#### O(n³) - Cubic Zaman
```
T(n) = c * n³

Örnek: Üç İç İçe Döngü
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) {
        for (int k = 0; k < n; k++) {
            // O(1) işlemler
        }
    }
}
```

**Tespit Yöntemi:**
- 3 seviye iç içe döngü
- İç içe stream + döngü kombinasyonları

#### O(2ⁿ) - Exponential Zaman
```
T(n) = c * 2ⁿ

Örnek: Fibonacci (Naive Recursion)
int fib(int n) {
    if (n <= 1) return n;
    return fib(n-1) + fib(n-2);  // 2 recursive call
}
```

**Tespit Yöntemi:**
- İki veya daha fazla recursive çağrı (her çağrı yürütülüyor)

### 💻 Uygulama Kodu
**Dosya:** `ComplexityAnalyzer.java` (satır 50-131)

```java
BigOComplexity finalComplexity = O_1;

// 1. Döngü analizi
if (maxDepth == 1) complexity = O_N;
if (maxDepth == 2) complexity = O_N_SQUARED;
if (maxDepth == 3) complexity = O_N_CUBED;

// 2. Stream analizi
if (has_sorted()) complexity = O_N_LOG_N;
if (has_linear_methods()) complexity = O_N;

// 3. İç içe işlemler
if (loop_depth > 0 && has_stream) {
    complexity = multiply(loop_complexity, stream_complexity);
}

// 4. Dominant karmaşıklık
finalComplexity = dominant(all_complexities);
```

### 📏 Dominant Kuralı
```
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ)

Birden fazla karmaşıklık varsa en büyüğü alınır.
```

### ⚠️ Mevcut Sorun
**YANLIŞ POZİTİF:** `buildString()` gibi StringBuilder kullanılan metotlara bile "STRING_CONCAT_IN_LOOP" uyarısı veriliyor.

---

## 3️⃣ CHIDAMBER & KEMERER (CK) METRICS

### 📐 WMC (Weighted Methods per Class)

**Formül:**
```
WMC = Σ CC(mᵢ)

Nerede:
- CC(mᵢ) = i'inci metodun cyclomatic complexity değeri
- Σ = Sınıftaki tüm metodlar için toplam
```

**Örnek Hesaplama:**
```java
class Example {
    void method1() { ... }  // CC = 3
    void method2() { ... }  // CC = 7
    void method3() { ... }  // CC = 2
}

WMC = 3 + 7 + 2 = 12
```

**Threshold:**
```
WMC < 10    → İyi
WMC 10-30   → Kabul edilebilir
WMC > 30    → Yüksek karmaşıklık, sınıf çok fazla iş yapıyor ⚠️
```

**Kod:** `CKMetricsCalculator.java:84-97`

---

### 📐 DIT (Depth of Inheritance Tree)

**Formül:**
```
DIT = Kök sınıfa olan maksimum mesafe

Örnek:
Object → Parent → Child → GrandChild
DIT(GrandChild) = 3
```

**Hesaplama Algoritması:**
```java
int calculateDIT(String className) {
    int depth = 0;
    String current = className;

    while (hasParent(current)) {
        current = getParent(current);
        depth++;
    }

    return depth;
}
```

**Threshold:**
```
DIT = 0-2   → İyi (sığ kalıtım)
DIT = 3-4   → Kabul edilebilir
DIT > 5     → Derin kalıtım, anlaşılması zor ⚠️
```

**Kod:** `CKMetricsCalculator.java:104-122`

---

### 📐 NOC (Number of Children)

**Formül:**
```
NOC = Doğrudan alt sınıf sayısı

Örnek:
Parent
  ├── Child1
  ├── Child2
  └── Child3

NOC(Parent) = 3
```

**Threshold:**
```
NOC = 0-3   → İyi
NOC = 4-7   → Kabul edilebilir
NOC > 7     → Çok fazla alt sınıf, tasarım gözden geçirilmeli ⚠️
```

**Kod:** `CKMetricsCalculator.java:129-132`

---

### 📐 CBO (Coupling Between Objects)

**Formül:**
```
CBO = |{Cᵢ : C bağımlı}|

Nerede:
- Cᵢ = Sınıfın bağımlı olduğu benzersiz sınıflar
- Bağımlılık kaynakları:
  * Method çağrıları
  * Field erişimleri
  * Tip referansları (field, parametre, return type)
```

**Örnek:**
```java
class OrderService {
    private CustomerRepository repo;     // +1 CBO
    private EmailService emailService;   // +1 CBO

    public Order createOrder(Product p) {  // +1 CBO (Product)
        Customer c = repo.findById(1);     // zaten sayıldı
        emailService.send(c.getEmail());   // zaten sayıldı
        return new Order();                 // +1 CBO (Order)
    }
}

CBO(OrderService) = 4
```

**Threshold:**
```
CBO < 5     → Düşük bağımlılık (İyi)
CBO 5-10    → Orta bağımlılık (Kabul edilebilir)
CBO > 10    → Yüksek bağımlılık (Refactoring gerekli) ⚠️
```

**Kod:** `CKMetricsCalculator.java:140-187`

---

### 📐 RFC (Response For a Class)

**Formül:**
```
RFC = |RS|

Nerede:
RS = {Mᵢ} ∪ {Rᵢ}
- Mᵢ = Sınıftaki tüm metodlar
- Rᵢ = Sınıf tarafından çağrılan tüm dış metodlar
```

**Örnek:**
```java
class Calculator {
    int add(int a, int b) { return a + b; }        // M1
    int multiply(int a, int b) { return a * b; }   // M2

    void print() {
        System.out.println("Result");              // R1: println()
        String.valueOf(123);                       // R2: valueOf()
    }
}

RFC = |{add, multiply, print, println, valueOf}| = 5
```

**Threshold:**
```
RFC < 20    → Düşük davranışsal karmaşıklık
RFC 20-50   → Orta karmaşıklık
RFC > 50    → Yüksek karmaşıklık, test etmesi zor ⚠️
```

**Kod:** `CKMetricsCalculator.java:195-221`

---

### 📐 LCOM (Lack of Cohesion in Methods)

**Formül:**
```
LCOM = P - Q

Nerede:
- P = Field paylaşmayan metot çiftleri sayısı
- Q = Field paylaşan metot çiftleri sayısı
- LCOM < 0 ise LCOM = 0

Alternatif:
LCOM = max(P - Q, 0)
```

**Detaylı Hesaplama:**
```
1. Her metot için eriştiği field'ları belirle
2. Tüm metot çiftlerini karşılaştır
3. Her çift için:
   - Ortak field varsa → Q += 1
   - Ortak field yoksa → P += 1
4. LCOM = P - Q (minimum 0)
```

**Örnek:**
```java
class User {
    private String name;
    private int age;
    private String address;

    String getName() { return name; }           // {name}
    void setName(String n) { name = n; }        // {name}
    int getAge() { return age; }                // {age}
    String getAddress() { return address; }     // {address}
}

Metot Çiftleri:
- (getName, setName):    Ortak {name} → Q
- (getName, getAge):     Ortak yok → P
- (getName, getAddress): Ortak yok → P
- (setName, getAge):     Ortak yok → P
- (setName, getAddress): Ortak yok → P
- (getAge, getAddress):  Ortak yok → P

P = 5, Q = 1
LCOM = 5 - 1 = 4 (Düşük cohesion, sınıf bölünmeli)
```

**Threshold:**
```
LCOM = 0      → Yüksek cohesion (İdeal)
LCOM = 1-5    → Kabul edilebilir cohesion
LCOM > 5      → Düşük cohesion, sınıf birden fazla sorumluluk taşıyor ⚠️
```

**Kod:** `CKMetricsCalculator.java:234-301`

---

## 4️⃣ HALSTEAD METRICS

### 📐 Temel Sayımlar

**Operatör ve Operand Tanımları:**
```
Operatörler: +, -, *, /, %, ==, !=, <, >, <=, >=, &&, ||, !,
             =, +=, -=, if, while, for, return, new, (), [], ., etc.

Operandlar: Değişkenler, sabitler, literal değerler, metot isimleri
```

**Temel Metrikler:**
```
n1 = Benzersiz operatör sayısı (distinct operators)
n2 = Benzersiz operand sayısı (distinct operands)
N1 = Toplam operatör sayısı (total operators)
N2 = Toplam operand sayısı (total operands)
```

### 📐 Türetilen Metrikler

#### Program Vocabulary (Kelime Dağarcığı)
```
n = n1 + n2
```

#### Program Length (Program Uzunluğu)
```
N = N1 + N2
```

#### Calculated Program Length (Hesaplanan Uzunluk)
```
N̂ = n1 * log₂(n1) + n2 * log₂(n2)
```

#### Volume (Hacim)
```
V = N * log₂(n)

Fiziksel anlam: Programı anlamak için gereken "bilgi" miktarı
```

#### Difficulty (Zorluk)
```
D = (n1 / 2) * (N2 / n2)

Fiziksel anlam:
- n1/2: Operatör çeşitliliği (daha fazla operatör = daha zor)
- N2/n2: Operand kullanım yoğunluğu (tekrar kullanım)
```

#### Effort (Çaba)
```
E = D * V

Fiziksel anlam: Programı yazma/anlama için gereken mental efor
```

#### Time to Program (Programlama Süresi)
```
T = E / 18 saniye

Not: 18, Halstead'in deneysel çalışmalarından gelen sabit
```

#### Delivered Bugs (Tahmini Hata Sayısı)
```
B = V / 3000

Not: Deneysel olarak her 3000 "bit" bilgi için ~1 hata bulunmuş
```

### 💻 Örnek Hesaplama

```java
int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n + 1);
}
```

**Operatörler:**
```
Benzersiz: if, <=, return, (), -, +    → n1 = 6
Toplam:    if, <=, return, return, (), (), -, +, (), (), -, +    → N1 = 12
```

**Operandlar:**
```
Benzersiz: fibonacci, n, 1    → n2 = 3
Toplam:    fibonacci, n, n, 1, n, fibonacci, n, 1, fibonacci, n, 1    → N2 = 11
```

**Hesaplamalar:**
```
n = 6 + 3 = 9
N = 12 + 11 = 23
V = 23 * log₂(9) = 23 * 3.17 = 72.91
D = (6/2) * (11/3) = 3 * 3.67 = 11.01
E = 11.01 * 72.91 = 802.85
T = 802.85 / 18 = 44.6 saniye
B = 72.91 / 3000 = 0.024 hata
```

**Kod:** `HalsteadMetricsCalculator.java:32-288`

---

## 5️⃣ MAINTAINABILITY INDEX

### 📐 Microsoft Formülü

```
MI = 171 - 5.2 * ln(V) - 0.23 * CC - 16.2 * ln(LOC)

Nerede:
- V   = Halstead Volume
- CC  = Cyclomatic Complexity
- LOC = Lines of Code (fiziksel satır sayısı)
- ln  = Doğal logaritma
```

### 📐 Normalize Edilmiş MI (0-100)

```
MI_norm = max(0, (MI / 171) * 100)
```

### 📏 Kategoriler

```
MI = 85-100   → Yüksek bakım kolaylığı (Yeşil) 🟢
MI = 65-84    → Orta bakım kolaylığı (Sarı) 🟡
MI = 0-64     → Düşük bakım kolaylığı (Kırmızı) 🔴
MI < 0        → Kritik durum ⚫
```

### 💻 Örnek Hesaplama

```java
void processData(String input) {  // 30 LOC, CC=8, V=250
    // ... karmaşık kod ...
}
```

**Hesaplama:**
```
MI = 171 - 5.2 * ln(250) - 0.23 * 8 - 16.2 * ln(30)
MI = 171 - 5.2 * 5.52 - 1.84 - 16.2 * 3.40
MI = 171 - 28.70 - 1.84 - 55.08
MI = 85.38 (Yüksek bakım kolaylığı - Sınırda)
```

**Kod:** `MaintainabilityIndexCalculator.java:36-115`

---

## 6️⃣ QUALITY SCORE (Pragmite Özel)

### 📐 Ana Formül

```
Pragmatic Score = w₁*DRY + w₂*ORTHO + w₃*CORRECT + w₄*PERF

Ağırlıklar:
w₁ = 0.30  (DRY - Don't Repeat Yourself)
w₂ = 0.30  (Orthogonality - Bağımsızlık)
w₃ = 0.25  (Correctness - Doğruluk)
w₄ = 0.15  (Performance - Performans)

Toplam = 1.00
```

### 📐 Alt Skorlar

#### DRY Score
```
DRY_Score = 100 - (DRY_violations * 10)

DRY İhlalleri:
- Duplicate code
- Copy-paste smell
- Magic strings/numbers (aynı değerlerin tekrarı)
```

#### Orthogonality Score
```
ORTHO_Score = 100 - (ORTHO_violations * 8)

Orthogonality İhlalleri:
- God Class
- Feature Envy
- Inappropriate Intimacy
- High Coupling (CBO > 10)
```

#### Correctness Score
```
CORRECT_Score = 100 - (CORRECT_violations * 15)

Correctness İhlalleri:
- Empty catch blocks
- Missing null checks
- Magic numbers (anlaşılmazlık)
- Unused variables/imports
```

#### Performance Score
```
PERF_Score = 100 - (PERF_violations * 12 + high_complexity_methods * 6)

Performance İhlalleri:
- String concatenation in loop
- O(n²) veya daha kötü karmaşıklık
- Inefficient collections
```

### 📏 Grade Sistemi

```
Score ≥ 90  → A (Mükemmel)
Score ≥ 80  → B (İyi)
Score ≥ 70  → C (Orta)
Score ≥ 60  → D (Zayıf)
Score < 60  → F (Başarısız)
```

**Kod:** `ScoreCalculator.java:13-163` ve `QualityScore.java:6-89`

---

## 🔍 YANLIŞ POZİTİF ANALİZİ

### Tespit Edilen Problemler

#### 1. STRING_CONCAT_IN_LOOP - Yanlış Alarm
**Durum:** Calculator.java:32
```java
// ✅ DOĞRU KOD - StringBuilder kullanılıyor
public int sum(List<Integer> nums) {
    int total = 0;
    for (int num : nums) {
        total += num;  // ❌ YANLIŞ: String concatenation diye işaretleniyor
    }
    return total;
}
```

**Sorun:** Detector, `+=` operatörünü görünce String concat sanıyor, ama bu int toplama!

**Düzeltme:** `StringConcatInLoopDetector.java` - Sadece String tipindeki değişkenleri kontrol etmeli.

---

#### 2. LAZY_CLASS - Agresif Tespit
**Durum:** UserService.java:67
```java
// ✅ NORMAL KOD - Data class/DTO normal
class User {
    private String name;
    private int age;
    // ... getters/setters ...
}
```

**Sorun:** DTO/Model sınıfları genelde az metot içerir, bu normaldir.

**Düzeltme:** Threshold yükseltilmeli veya DTO pattern tanınmalı.

---

#### 3. MAGIC_NUMBER - Aşırı Duyarlı
**Durum:** Döngü sınırları, HTTP status kodları
```java
// ✅ KABUL EDİLEBİLİR
for (int i = 0; i < 10; i++) { ... }  // 10 küçük sayı, açık

if (statusCode == 200) return "OK";    // HTTP standart, herkes bilir
```

**Sorun:** Her magic number tespit edilmeli ama küçük değerler (0-10) ve standart sabitler (200, 404) muaf olabilir.

**Düzeltme:** Whitelist ekle: 0, 1, 2, 10, 100, 200, 404, 500 vb.

---

## 📊 ÖNERİLEN THRESHOLD DEĞİŞİKLİKLERİ

| Metrik | Mevcut Threshold | Önerilen | Açıklama |
|--------|------------------|----------|----------|
| **Cyclomatic Complexity** | >10 | >15 | CC=11-15 kabul edilebilir |
| **Long Method (LOC)** | >30 | >50 | Modern IDE'lerde 30-50 satır normal |
| **Long Parameter List** | >4 | >5 | 5 parametre kabul edilebilir |
| **LCOM** | >5 | >8 | Biraz daha toleranslı olmalı |
| **Magic Number Ignore** | Yok | 0,1,2,10,100 | Yaygın değerler muaf |
| **Lazy Class (min LOC)** | 25 | 50 | DTO'lar genelde küçük |

---

## 🎯 SONUÇ VE ÖNERİLER

### ✅ Güçlü Yönler
1. **Matematiksel Temel Sağlam:** Tüm metrikler bilimsel kaynaklara dayanıyor
2. **Kapsamlı Analiz:** CK, Halstead, MI gibi birden fazla metrik kullanılıyor
3. **Doğru Formüller:** Hesaplamalar standart formüllere uygun

### ⚠️ İyileştirme Gereken Alanlar
1. **Yanlış Pozitifler:** Threshold'lar çok agresif, düzeltilmeli
2. **Context Awareness:** Kod tipine göre (DTO, Util, Service) farklı kurallar olmalı
3. **Whitelist Desteği:** Bilinen pattern'ler ve standartlar muaf tutulmalı

### 🔧 Aksiyon Maddeleri
1. ✅ String concat detector'ı tip kontrolü ekleyerek düzelt
2. ✅ Magic number için whitelist (0-10, HTTP codes) ekle
3. ✅ Lazy class için DTO/Model annotation desteği ekle
4. ✅ Threshold'ları yukarıdaki tabloya göre ayarla
5. ✅ Test coverage ekle (unit tests for edge cases)

---

**Rapor Tarihi:** 2025-12-24
**Versiyon:** 1.0.0
**Hazırlayan:** Pragmite Code Analysis System
