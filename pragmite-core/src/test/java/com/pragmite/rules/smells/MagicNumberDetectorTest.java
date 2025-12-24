package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MagicNumberDetectorTest {

    private MagicNumberDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MagicNumberDetector();
    }

    @Test
    void shouldDetectSimpleMagicNumber() {
        String code = """
            public class Test {
                public int calculate() {
                    return 42;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertEquals(CodeSmellType.MAGIC_NUMBER, smells.get(0).getType());
        assertTrue(smells.get(0).getMessage().contains("42"));
    }

    @Test
    void shouldDetectHexadecimalNumber() {
        String code = """
            public class Test {
                public int getMask() {
                    return 0xFF00;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertTrue(smells.get(0).getMessage().contains("0xFF00"));
        assertTrue(smells.get(0).getMessage().contains("hexadecimal"));
    }

    @Test
    void shouldDetectBinaryNumber() {
        String code = """
            public class Test {
                public int getFlags() {
                    return 0b10101010;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertTrue(smells.get(0).getMessage().contains("0b10101010"));
        assertTrue(smells.get(0).getMessage().contains("binary"));
    }

    @Test
    void shouldDetectFloatNumber() {
        String code = """
            public class Test {
                public double calculateArea(double radius) {
                    return 3.14159 * radius * radius;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertTrue(smells.get(0).getMessage().contains("3.14159"));
    }

    @Test
    void shouldIgnoreCommonNumbers() {
        String code = """
            public class Test {
                public int[] createArray() {
                    return new int[]{0, 1, 2, -1};
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should ignore 0, 1, 2, -1");
    }

    @Test
    void shouldIgnorePowerOf2ArraySizes() {
        String code = """
            public class Test {
                int[] small = new int[16];
                int[] medium = new int[256];
                int[] large = new int[1024];
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should ignore power-of-2 array sizes");
    }

    @Test
    void shouldIgnoreNumbersInAnnotations() {
        String code = """
            public class Test {
                @Size(min = 5, max = 100)
                private String name;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should ignore numbers in annotations");
    }

    @Test
    void shouldDetectOctalNumber() {
        String code = """
            public class Test {
                public int getPermissions() {
                    return 0755;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertTrue(smells.get(0).getMessage().contains("0755"));
        assertTrue(smells.get(0).getMessage().contains("octal"));
    }

    @Test
    void shouldIgnoreConstantDefinitions() {
        String code = """
            public class Test {
                private static final int MAX_SIZE = 100;
                private static final double PI = 3.14159;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should ignore constant definitions");
    }
}
