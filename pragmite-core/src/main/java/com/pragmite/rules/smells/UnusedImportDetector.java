package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kullanılmayan import kokusu dedektörü.
 * Inner class, annotation, generic, reflection kullanımlarını destekler.
 */
public class UnusedImportDetector implements SmellDetector {

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        // Tüm import'ları topla
        List<ImportDeclaration> imports = cu.getImports();
        if (imports.isEmpty()) return smells;

        // Dosya içeriğinden import satırlarını çıkar
        String contentWithoutImports = removeImportLines(content);

        for (ImportDeclaration imp : imports) {
            if (imp.isAsterisk()) continue; // Wildcard import'ları atla
            if (imp.isStatic()) {
                // Static import - metodları kontrol et
                if (!isStaticImportUsed(contentWithoutImports, imp.getNameAsString())) {
                    addUnusedImportSmell(smells, imp, filePath);
                }
            } else {
                // Normal class import
                String fullClassName = imp.getNameAsString();

                if (!isClassImportUsed(contentWithoutImports, fullClassName)) {
                    addUnusedImportSmell(smells, imp, filePath);
                }
            }
        }

        return smells;
    }

    private boolean isClassImportUsed(String content, String fullClassName) {
        // 1. Simple class name kontrolü
        String simpleClassName = getSimpleClassName(fullClassName);

        // 2. Inner class desteği: Outer.Inner -> hem "Outer" hem "Inner" kontrol et
        String[] parts = fullClassName.split("\\.");

        // Her bir part'ı kontrol et (inner class desteği için)
        for (int i = parts.length - 1; i >= 0; i--) {
            String classNameToCheck = parts[i];

            // Word boundary ile tam eşleşme + context kontrolü
            if (isUsedInContext(content, classNameToCheck)) {
                return true;
            }
        }

        // 3. Fully qualified name kontrolü (reflection için)
        if (content.contains(fullClassName)) {
            return true;
        }

        return false;
    }

    private boolean isUsedInContext(String content, String className) {
        // Çeşitli kullanım context'lerini kontrol et:

        // 1. Normal kullanım: ClassName var, new ClassName()
        Pattern normalUsage = Pattern.compile("\\b" + Pattern.quote(className) + "\\b");
        if (normalUsage.matcher(content).find()) {
            return true;
        }

        // 2. Generic kullanım: List<ClassName>, Map<String, ClassName>
        Pattern genericUsage = Pattern.compile("<[^>]*\\b" + Pattern.quote(className) + "\\b[^>]*>");
        if (genericUsage.matcher(content).find()) {
            return true;
        }

        // 3. Annotation kullanımı: @ClassName, @ClassName(...)
        Pattern annotationUsage = Pattern.compile("@" + Pattern.quote(className) + "\\b");
        if (annotationUsage.matcher(content).find()) {
            return true;
        }

        // 4. Array: ClassName[], ClassName[][]
        Pattern arrayUsage = Pattern.compile("\\b" + Pattern.quote(className) + "\\s*\\[");
        if (arrayUsage.matcher(content).find()) {
            return true;
        }

        // 5. Method reference: ClassName::method
        Pattern methodRefUsage = Pattern.compile("\\b" + Pattern.quote(className) + "\\s*::");
        if (methodRefUsage.matcher(content).find()) {
            return true;
        }

        // 6. Class literal: ClassName.class
        Pattern classLiteral = Pattern.compile("\\b" + Pattern.quote(className) + "\\.class\\b");
        if (classLiteral.matcher(content).find()) {
            return true;
        }

        return false;
    }

    private boolean isStaticImportUsed(String content, String staticImportName) {
        // Static import: import static package.Class.method
        // Son kısım metot veya constant adı
        String memberName = getSimpleClassName(staticImportName);

        // Method call veya constant kullanımı
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(memberName) + "\\b");
        return pattern.matcher(content).find();
    }

    private void addUnusedImportSmell(List<CodeSmell> smells, ImportDeclaration imp, String filePath) {
        int line = imp.getBegin().map(pos -> pos.line).orElse(0);

        CodeSmell smell = new CodeSmell(
            CodeSmellType.UNUSED_IMPORT,
            filePath,
            line,
            String.format("Kullanılmayan import: %s", imp.getNameAsString())
        );
        smell.withSuggestion("Bu import'u kaldırın")
             .withAutoFix(true);

        smells.add(smell);
    }

    private String getSimpleClassName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private String removeImportLines(String content) {
        StringBuilder result = new StringBuilder();
        boolean inImportSection = false;

        for (String line : content.split("\n")) {
            String trimmed = line.trim();

            // Import section'ı atla
            if (trimmed.startsWith("import ")) {
                inImportSection = true;
                continue;
            }

            // Package declaration'dan sonra import section başlar
            if (trimmed.startsWith("package ")) {
                continue;
            }

            // Boş satırlar import section'da olabilir
            if (inImportSection && trimmed.isEmpty()) {
                continue;
            }

            // Import section bitti
            if (inImportSection && !trimmed.isEmpty() && !trimmed.startsWith("import")) {
                inImportSection = false;
            }

            result.append(line).append("\n");
        }

        return result.toString();
    }
}
