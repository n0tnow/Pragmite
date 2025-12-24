package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data Class (Anemic Domain Model) dedektörü.
 * Sadece getter/setter içeren, davranış barındırmayan sınıfları tespit eder.
 */
public class DataClassDetector implements SmellDetector {

    private static final int MIN_FIELDS = 3;
    private static final double MAX_ACCESSOR_RATIO = 0.9;

    // Getter/setter pattern'ları
    private static final Set<String> ACCESSOR_PREFIXES = Set.of(
        "get", "set", "is", "has", "can"
    );

    // İzin verilen standart metodlar
    private static final Set<String> ALLOWED_METHODS = Set.of(
        "toString", "hashCode", "equals", "compareTo", "clone",
        "builder", "toBuilder"
    );

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                super.visit(cid, arg);

                if (cid.isInterface()) return;

                // Alan sayısı
                List<FieldDeclaration> fields = cid.getFields();
                int fieldCount = fields.stream()
                    .mapToInt(f -> f.getVariables().size())
                    .sum();

                if (fieldCount < MIN_FIELDS) return;

                // Metot analizi
                List<MethodDeclaration> methods = cid.getMethods();
                int totalMethods = methods.size();

                if (totalMethods == 0) return;

                int accessorCount = 0;
                int businessMethodCount = 0;

                for (MethodDeclaration md : methods) {
                    String name = md.getNameAsString();

                    if (isAccessorMethod(name) || ALLOWED_METHODS.contains(name)) {
                        accessorCount++;
                    } else if (!md.isPrivate()) {
                        // Private olmayan ve accessor olmayan metodlar iş mantığı olabilir
                        businessMethodCount++;
                    }
                }

                // Accessor oranı çok yüksekse ve iş mantığı metodu yoksa
                double accessorRatio = (double) accessorCount / totalMethods;

                if (accessorRatio >= MAX_ACCESSOR_RATIO && businessMethodCount == 0 && fieldCount >= MIN_FIELDS) {
                    int line = cid.getBegin().map(pos -> pos.line).orElse(0);

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.DATA_CLASS,
                        filePath,
                        line,
                        String.format("Sınıf '%s' sadece veri tutuyor, davranış yok: %d alan, %d metot (%%%d accessor)",
                            cid.getNameAsString(), fieldCount, totalMethods, (int)(accessorRatio * 100))
                    );
                    smell.withAffectedElement(cid.getNameAsString())
                         .withSuggestion("İlgili iş mantığını bu sınıfa taşıyın veya Record kullanın (Java 16+)")
                         .withAutoFix(false);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }

    private boolean isAccessorMethod(String name) {
        for (String prefix : ACCESSOR_PREFIXES) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                char nextChar = name.charAt(prefix.length());
                if (Character.isUpperCase(nextChar)) {
                    return true;
                }
            }
        }
        return false;
    }
}
