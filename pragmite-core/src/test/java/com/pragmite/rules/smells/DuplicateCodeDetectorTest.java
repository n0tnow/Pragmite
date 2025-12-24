package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateCodeDetectorTest {

    private DuplicateCodeDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DuplicateCodeDetector();
    }

    @Test
    void shouldDetectDuplicateCodeBlocks() {
        String code = """
            public class Test {
                public void method1() {
                    int a = 1;
                    int b = 2;
                    int c = 3;
                    int d = 4;
                    int e = 5;
                    int f = 6;
                    System.out.println(a + b + c);
                }

                public void method2() {
                    int x = 1;
                    int y = 2;
                    int z = 3;
                    int w = 4;
                    int v = 5;
                    int u = 6;
                    System.out.println(x + y + z);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertTrue(smells.size() > 0, "Should detect duplicate code");
        assertTrue(smells.stream().anyMatch(s -> s.getType() == CodeSmellType.DUPLICATED_CODE));
    }

    @Test
    void shouldNotDetectSmallSimilarities() {
        String code = """
            public class Test {
                public void method1() {
                    int a = 1;
                    int b = 2;
                }

                public void method2() {
                    int x = 1;
                    int y = 2;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should not detect small code blocks");
    }

    @Test
    void shouldDetectSimilarCodeWithDifferentVariableNames() {
        String code = """
            public class Test {
                public void calculateTotal1() {
                    int sum = 0;
                    for (int i = 0; i < 10; i++) {
                        sum += i;
                    }
                    int average = sum / 10;
                    int max = 100;
                    int min = 0;
                    return;
                }

                public void calculateTotal2() {
                    int total = 0;
                    for (int j = 0; j < 10; j++) {
                        total += j;
                    }
                    int avg = total / 10;
                    int maximum = 100;
                    int minimum = 0;
                    return;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertTrue(smells.size() > 0, "Should detect Type-2 clones (renamed variables)");
    }
}
