package com.pragmite.rules.smells;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Boş catch bloğu kokusu dedektörü.
 * Comment içeren ama kod içermeyen catch blokları da kontrol edilir.
 */
public class EmptyCatchBlockDetector implements SmellDetector {

    // Kasıtlı olarak ignore edilen durumları belirten keyword'ler
    private static final Set<String> INTENTIONAL_IGNORE_KEYWORDS = Set.of(
        "ignore", "ignored", "suppress", "suppressed",
        "expected", "intentional", "acceptable", "ok",
        "noop", "no-op", "empty on purpose"
    );

    // Suppression annotation pattern'leri
    private static final Pattern SUPPRESS_PATTERN = Pattern.compile(
        "@SuppressWarnings|@Suppress|@Ignore",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<CodeSmell> detect(CompilationUnit cu, String filePath, String content) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(CatchClause cc, Void arg) {
                super.visit(cc, arg);

                // Catch bloğu boş mu kontrol et
                if (cc.getBody().getStatements().isEmpty()) {
                    int line = cc.getBegin().map(pos -> pos.line).orElse(0);

                    // Comment kontrolü yap
                    boolean hasIntentionalIgnoreComment = hasIntentionalComment(cc);
                    boolean hasSuppressAnnotation = hasSuppressAnnotation(cc);

                    // Kasıtlı olarak ignore edildiğine dair kanıt var mı?
                    if (!hasIntentionalIgnoreComment && !hasSuppressAnnotation) {
                        // Gerçekten boş - kod kokusu!
                        CodeSmell smell = new CodeSmell(
                            CodeSmellType.EMPTY_CATCH_BLOCK,
                            filePath,
                            line,
                            String.format("Boş catch bloğu: %s", cc.getParameter().getType())
                        );
                        smell.withSuggestion("En azından exception'ı logla veya yeniden fırlat. Kasıtlı ise açıklayıcı comment ekleyin.")
                             .withAutoFix(false);

                        smells.add(smell);
                    } else {
                        // Comment var ama hala şüpheli - INFO seviyesinde uyar
                        // Bu durumda smell ekleme, ama gerekirse ekleyebiliriz
                        // Örnek: INFO seviyesi bir uyarı
                    }
                }
            }
        }, null);

        return smells;
    }

    /**
     * Catch clause'da kasıtlı ignore belirten comment var mı?
     */
    private boolean hasIntentionalComment(CatchClause cc) {
        // Catch bloğundaki comment'leri kontrol et
        Optional<Comment> comment = cc.getBody().getComment();
        if (comment.isPresent()) {
            String commentText = comment.get().getContent().toLowerCase();
            for (String keyword : INTENTIONAL_IGNORE_KEYWORDS) {
                if (commentText.contains(keyword)) {
                    return true;
                }
            }
        }

        // Body içindeki tüm comment'leri kontrol et
        List<Comment> allComments = cc.getBody().getAllContainedComments();
        for (Comment c : allComments) {
            String commentText = c.getContent().toLowerCase();
            for (String keyword : INTENTIONAL_IGNORE_KEYWORDS) {
                if (commentText.contains(keyword)) {
                    return true;
                }
            }
        }

        // Catch clause'un parameter ismini kontrol et
        // Örnek: catch (Exception ignored) veya catch (IOException expected)
        String paramName = cc.getParameter().getNameAsString().toLowerCase();
        for (String keyword : INTENTIONAL_IGNORE_KEYWORDS) {
            if (paramName.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Suppression annotation var mı?
     */
    private boolean hasSuppressAnnotation(CatchClause cc) {
        // Parent try-catch statement'ın annotation'larını kontrol et
        return cc.getParentNode()
            .flatMap(parent -> parent.getParentNode())
            .map(grandParent -> {
                String nodeStr = grandParent.toString();
                return SUPPRESS_PATTERN.matcher(nodeStr).find();
            })
            .orElse(false);
    }
}
