package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Magic Number (Sihirli Sayı) kokusu dedektörü.
 * Metot içinde kullanılan açıklanmamış sayısal değerleri tespit eder.
 * Float, Hex, Binary, Octal literal'leri de destekler.
 */
public class MagicNumberDetector implements SmellDetector {

    // Bu değerler genellikle magic number sayılmaz (yaygın kullanılan değerler)
    private static final Set<String> ALLOWED_VALUES = Set.of(
        // Integer - EXPANDED: 0-10 arası kabul edilebilir
        "0", "1", "-1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        // Double/Float
        "0.0", "1.0", "0.5", "2.0",
        "0.0f", "1.0f", "0.5f", "2.0f",
        // Long
        "0L", "1L", "-1L", "0l", "1l", "-1l",
        // Hex
        "0x0", "0x1", "0X0", "0X1",
        // Binary
        "0b0", "0b1", "0B0", "0B1"
    );

    // Yaygın array boyutları ve buffer size'ları - kabul edilebilir
    private static final Set<String> COMMON_SIZES = Set.of(
        "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096"
    );

    // HTTP status kodları ve yaygın sabitler - kabul edilebilir
    private static final Set<String> HTTP_AND_COMMON_CODES = Set.of(
        "100", "200", "201", "204", "301", "302", "304", "400", "401", "403", "404", "500", "502", "503",
        "24", "60", "1000" // Hours, minutes, milliseconds
    );

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            private boolean insideMethod = false;
            private boolean insideFieldInit = false;
            private boolean insideArrayDimension = false;
            private boolean insideAnnotation = false;

            @Override
            public void visit(FieldDeclaration fd, Void arg) {
                // final static alanları içindeki değerler magic number sayılmaz (sabitler)
                if (fd.isStatic() && fd.isFinal()) {
                    return;
                }
                insideFieldInit = true;
                super.visit(fd, arg);
                insideFieldInit = false;
            }

            @Override
            public void visit(MethodDeclaration md, Void arg) {
                insideMethod = true;
                super.visit(md, arg);
                insideMethod = false;
            }

            @Override
            public void visit(MarkerAnnotationExpr n, Void arg) {
                insideAnnotation = true;
                super.visit(n, arg);
                insideAnnotation = false;
            }

            @Override
            public void visit(SingleMemberAnnotationExpr n, Void arg) {
                insideAnnotation = true;
                super.visit(n, arg);
                insideAnnotation = false;
            }

            @Override
            public void visit(NormalAnnotationExpr n, Void arg) {
                insideAnnotation = true;
                super.visit(n, arg);
                insideAnnotation = false;
            }

            @Override
            public void visit(ArrayCreationExpr ace, Void arg) {
                // Array boyutları genellikle magic number sayılmaz
                insideArrayDimension = true;
                super.visit(ace, arg);
                insideArrayDimension = false;
            }

            @Override
            public void visit(IntegerLiteralExpr n, Void arg) {
                super.visit(n, arg);
                String value = n.getValue();
                checkMagicNumber(value, n.getBegin().map(pos -> pos.line).orElse(0), "integer");
            }

            @Override
            public void visit(DoubleLiteralExpr n, Void arg) {
                super.visit(n, arg);
                String value = n.getValue();
                checkMagicNumber(value, n.getBegin().map(pos -> pos.line).orElse(0), "double");
            }

            @Override
            public void visit(LongLiteralExpr n, Void arg) {
                super.visit(n, arg);
                String value = n.getValue();
                checkMagicNumber(value, n.getBegin().map(pos -> pos.line).orElse(0), "long");
            }

            private void checkMagicNumber(String value, int line, String type) {
                // Metot içinde değilse veya annotation içindeyse atla
                if (!insideMethod || insideFieldInit || insideAnnotation) return;

                // İzin verilen değerler
                if (ALLOWED_VALUES.contains(value)) return;

                // HTTP status kodları ve yaygın sabitler
                if (HTTP_AND_COMMON_CODES.contains(value)) return;

                // Array boyutları için yaygın değerler
                if (insideArrayDimension && (COMMON_SIZES.contains(value) || ALLOWED_VALUES.contains(value))) {
                    return;
                }

                // Çok büyük sayılar (>10000) genellikle magic number (bitsel işlemler hariç)
                // Çok küçük ondalıklı sayılar (0.01 gibi) genellikle magic number
                boolean isSuspicious = false;
                try {
                    if (value.contains(".") || value.contains("e") || value.contains("E")) {
                        // Float/Double
                        double d = Double.parseDouble(value.replace("f", "").replace("F", ""));
                        if (d != 0.0 && d != 1.0 && d != 0.5 && d != 2.0) {
                            isSuspicious = true;
                        }
                    } else if (value.startsWith("0x") || value.startsWith("0X")) {
                        // Hexadecimal - likely magic
                        isSuspicious = true;
                    } else if (value.startsWith("0b") || value.startsWith("0B")) {
                        // Binary - should be named constant
                        isSuspicious = true;
                    } else if (value.startsWith("0") && value.length() > 1 && !value.contains(".")) {
                        // Octal - old style, likely magic
                        isSuspicious = true;
                    } else {
                        // Decimal integer
                        long l = Long.parseLong(value.replace("L", "").replace("l", ""));
                        if (l > 10 && !COMMON_SIZES.contains(value) && !HTTP_AND_COMMON_CODES.contains(value)) {
                            isSuspicious = true;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Parse edilemeyen değer - magic number olabilir
                    isSuspicious = true;
                }

                if (isSuspicious) {
                    String typeInfo = "";
                    if (value.startsWith("0x") || value.startsWith("0X")) {
                        typeInfo = " (hexadecimal)";
                    } else if (value.startsWith("0b") || value.startsWith("0B")) {
                        typeInfo = " (binary)";
                    } else if (value.startsWith("0") && value.length() > 1 && !value.contains(".")) {
                        typeInfo = " (octal)";
                    }

                    CodeSmell smell = new CodeSmell(
                        CodeSmellType.MAGIC_NUMBER,
                        filePath,
                        line,
                        String.format("Magic number found: %s%s", value, typeInfo)
                    );
                    smell.withSuggestion("Define this value as a descriptive constant (private static final)")
                         .withAutoFix(true);

                    smells.add(smell);
                }
            }
        }, null);

        return smells;
    }
}
