package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnusedImportDetectorTest {

    private UnusedImportDetector detector;

    @BeforeEach
    void setUp() {
        detector = new UnusedImportDetector();
    }

    @Test
    void shouldDetectUnusedImport() {
        String code = """
            package com.example;
            import java.util.ArrayList;
            import java.util.HashMap;

            public class Test {
                private ArrayList<String> list = new ArrayList<>();
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(1, smells.size());
        assertTrue(smells.get(0).getMessage().contains("HashMap"));
    }

    @Test
    void shouldNotDetectUsedImport() {
        String code = """
            package com.example;
            import java.util.ArrayList;
            import java.util.List;

            public class Test {
                private List<String> list = new ArrayList<>();
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size());
    }

    @Test
    void shouldDetectInnerClassUsage() {
        String code = """
            package com.example;
            import java.util.Map.Entry;

            public class Test {
                public void process(Entry<String, String> entry) {
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize inner class usage");
    }

    @Test
    void shouldDetectAnnotationUsage() {
        String code = """
            package com.example;
            import org.junit.jupiter.api.Test;

            public class MyTest {
                @Test
                public void testSomething() {
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "MyTest.java", code);

        assertEquals(0, smells.size(), "Should recognize annotation usage");
    }

    @Test
    void shouldDetectGenericUsage() {
        String code = """
            package com.example;
            import java.util.ArrayList;
            import java.util.HashMap;

            public class Test {
                private HashMap<String, ArrayList<Integer>> map;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize generic usage");
    }

    @Test
    void shouldDetectMethodReferenceUsage() {
        String code = """
            package com.example;
            import java.util.Arrays;
            import java.util.List;

            public class Test {
                public void process() {
                    List<String> list = Arrays.asList("a", "b");
                    list.forEach(System.out::println);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize method reference");
    }

    @Test
    void shouldDetectArrayUsage() {
        String code = """
            package com.example;
            import java.util.Date;

            public class Test {
                private Date[] dates;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize array usage");
    }

    @Test
    void shouldDetectClassLiteralUsage() {
        String code = """
            package com.example;
            import java.util.ArrayList;

            public class Test {
                public Class<?> getType() {
                    return ArrayList.class;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize class literal usage");
    }

    @Test
    void shouldIgnoreWildcardImports() {
        String code = """
            package com.example;
            import java.util.*;

            public class Test {
                private List<String> list;
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should skip wildcard imports");
    }

    @Test
    void shouldDetectStaticImportUsage() {
        String code = """
            package com.example;
            import static java.lang.Math.PI;
            import static java.lang.Math.sqrt;

            public class Test {
                public double calculate() {
                    return PI * sqrt(2.0);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Test.java", code);

        assertEquals(0, smells.size(), "Should recognize static import usage");
    }
}
