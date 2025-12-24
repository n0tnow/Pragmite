package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;

import java.util.List;

/**
 * Kod kokusu dedektörü arayüzü.
 * Tüm koku tespit sınıfları bu arayüzü implement etmelidir.
 */
public interface SmellDetector {

    /**
     * CompilationUnit üzerinde koku tespiti yapar.
     *
     * @param cu       Parse edilmiş Java dosyası
     * @param filePath Dosya yolu
     * @param content  Ham dosya içeriği
     * @return Bulunan kokular listesi
     */
    List<CodeSmell> detect(CompilationUnit cu, String filePath, String content);

    /**
     * Dedektörün adı.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
