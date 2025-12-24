package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.*;

/**
 * Try-with-resources kullanılmayan AutoCloseable kaynakları tespit eder.
 */
public class MissingTryWithResourcesDetector implements SmellDetector {

    // Yaygın AutoCloseable sınıfları
    private static final Set<String> CLOSEABLE_TYPES = Set.of(
        // I/O
        "InputStream", "OutputStream", "Reader", "Writer",
        "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
        "BufferedReader", "BufferedWriter", "BufferedInputStream", "BufferedOutputStream",
        "PrintWriter", "PrintStream", "Scanner",
        "InputStreamReader", "OutputStreamWriter",
        "ObjectInputStream", "ObjectOutputStream",
        "DataInputStream", "DataOutputStream",
        "RandomAccessFile", "FileChannel",
        // Database
        "Connection", "Statement", "PreparedStatement", "CallableStatement",
        "ResultSet", "RowSet",
        // Network
        "Socket", "ServerSocket", "DatagramSocket",
        "HttpURLConnection", "URLConnection",
        // Other
        "ZipFile", "JarFile", "ZipInputStream", "ZipOutputStream",
        "Formatter", "Stream"
    );

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration md, Void arg) {
                super.visit(md, arg);

                // Try-with-resources içindeki değişkenleri topla
                Set<String> resourceVariables = new HashSet<>();

                md.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(TryStmt ts, Void arg) {
                        // Try-with-resources kaynakları
                        ts.getResources().forEach(resource -> {
                            if (resource instanceof VariableDeclarationExpr) {
                                VariableDeclarationExpr vde = (VariableDeclarationExpr) resource;
                                vde.getVariables().forEach(v -> resourceVariables.add(v.getNameAsString()));
                            }
                        });
                        super.visit(ts, arg);
                    }
                }, null);

                // Closeable nesnelerin oluşturulduğu yerleri kontrol et
                md.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(ObjectCreationExpr oce, Void arg) {
                        super.visit(oce, arg);

                        String typeName = oce.getType().getNameAsString();

                        if (isCloseableType(typeName)) {
                            // Bu nesne bir değişkene atanıyor mu?
                            oce.getParentNode().ifPresent(parent -> {
                                if (parent instanceof VariableDeclarationExpr) {
                                    // Değişken adını kontrol et
                                    // Bu basit bir yaklaşım - daha gelişmiş analiz gerekebilir
                                } else {
                                    // Direkt kullanım - try-with-resources değilse sorun
                                    int line = oce.getBegin().map(p -> p.line).orElse(0);

                                    // Try-with-resources içinde mi?
                                    boolean inTryResources = isInTryResources(oce);

                                    if (!inTryResources) {
                                        CodeSmell smell = new CodeSmell(
                                            CodeSmellType.MISSING_TRY_WITH_RESOURCES,
                                            filePath,
                                            line,
                                            String.format("'%s' nesnesi try-with-resources ile kapatılmalı", typeName)
                                        );
                                        smell.withSuggestion("try-with-resources kullanarak kaynağı otomatik kapatın")
                                             .withAutoFix(true);

                                        smells.add(smell);
                                    }
                                }
                            });
                        }
                    }
                }, null);

                // close() çağrılarını kontrol et - manuel kapatma
                md.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodCallExpr mce, Void arg) {
                        super.visit(mce, arg);

                        if (mce.getNameAsString().equals("close")) {
                            // close() çağrısı finally bloğunda mı?
                            boolean inFinally = isInFinallyBlock(mce);

                            if (!inFinally) {
                                // Uyarı: close() finally dışında çağrılıyor
                                // Bu her zaman hata değil ama dikkat edilmeli
                                // Line number: mce.getBegin().map(p -> p.line).orElse(0)
                            }
                        }
                    }
                }, null);
            }
        }, null);

        return smells;
    }

    private boolean isCloseableType(String typeName) {
        return CLOSEABLE_TYPES.contains(typeName) ||
               typeName.endsWith("Stream") ||
               typeName.endsWith("Reader") ||
               typeName.endsWith("Writer") ||
               typeName.endsWith("Connection") ||
               typeName.endsWith("Channel");
    }

    private boolean isInTryResources(ObjectCreationExpr oce) {
        return oce.getParentNode()
            .flatMap(p -> p.getParentNode())
            .filter(gp -> gp instanceof TryStmt)
            .map(gp -> (TryStmt) gp)
            .map(ts -> !ts.getResources().isEmpty())
            .orElse(false);
    }

    private boolean isInFinallyBlock(MethodCallExpr mce) {
        var node = mce.getParentNode();
        while (node.isPresent()) {
            if (node.get() instanceof TryStmt) {
                TryStmt ts = (TryStmt) node.get();
                // Finally bloğu içinde mi kontrol et
                return ts.getFinallyBlock()
                    .map(fb -> fb.findAll(MethodCallExpr.class).contains(mce))
                    .orElse(false);
            }
            node = node.get().getParentNode();
        }
        return false;
    }
}
