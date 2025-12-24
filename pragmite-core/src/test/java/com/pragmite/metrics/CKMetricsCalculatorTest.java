package com.pragmite.metrics;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CKMetricsCalculatorTest {

    private CKMetricsCalculator calculator;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        calculator = new CKMetricsCalculator();
        parser = new JavaParser();
    }

    @Test
    void testWMC_SimpleClass() {
        String code = """
            public class SimpleClass {
                public void method1() {
                    int x = 1;
                }

                public void method2() {
                    if (true) {
                        System.out.println("test");
                    }
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("SimpleClass");
        assertNotNull(ckMetrics);
        // WMC = sum of complexities (method1: 1, method2: 2)
        assertEquals(3, ckMetrics.getWmc());
    }

    @Test
    void testWMC_ComplexClass() {
        String code = """
            public class ComplexClass {
                public void complexMethod() {
                    for (int i = 0; i < 10; i++) {
                        if (i % 2 == 0) {
                            System.out.println(i);
                        } else {
                            System.out.println("odd");
                        }
                    }
                }

                public int anotherMethod(int x) {
                    switch (x) {
                        case 1: return 1;
                        case 2: return 2;
                        case 3: return 3;
                        default: return 0;
                    }
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("ComplexClass");
        assertNotNull(ckMetrics);
        // WMC should be sum of both methods' complexities
        assertTrue(ckMetrics.getWmc() > 5, "WMC should reflect complexity");
    }

    @Test
    void testDIT_NoInheritance() {
        String code = """
            public class StandaloneClass {
                public void method() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("StandaloneClass");
        assertNotNull(ckMetrics);
        assertEquals(0, ckMetrics.getDit());
    }

    @Test
    void testDIT_SimpleInheritance() {
        String code = """
            class Parent {
                public void method() {}
            }

            public class Child extends Parent {
                public void childMethod() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics childMetrics = metrics.get("Child");
        assertNotNull(childMetrics);
        assertEquals(1, childMetrics.getDit());

        CKMetrics parentMetrics = metrics.get("Parent");
        assertNotNull(parentMetrics);
        assertEquals(0, parentMetrics.getDit());
    }

    @Test
    void testDIT_MultiLevelInheritance() {
        String code = """
            class GrandParent {
                public void method() {}
            }

            class Parent extends GrandParent {
                public void method2() {}
            }

            public class Child extends Parent {
                public void method3() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics childMetrics = metrics.get("Child");
        assertNotNull(childMetrics);
        assertEquals(2, childMetrics.getDit());

        CKMetrics parentMetrics = metrics.get("Parent");
        assertEquals(1, parentMetrics.getDit());

        CKMetrics grandParentMetrics = metrics.get("GrandParent");
        assertEquals(0, grandParentMetrics.getDit());
    }

    @Test
    void testNOC_NoChildren() {
        String code = """
            public class Parent {
                public void method() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics parentMetrics = metrics.get("Parent");
        assertEquals(0, parentMetrics.getNoc());
    }

    @Test
    void testNOC_MultipleChildren() {
        String code = """
            public class Parent {
                public void method() {}
            }

            class Child1 extends Parent {
                public void child1Method() {}
            }

            class Child2 extends Parent {
                public void child2Method() {}
            }

            class Child3 extends Parent {
                public void child3Method() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics parentMetrics = metrics.get("Parent");
        assertEquals(3, parentMetrics.getNoc());

        // Children should have 0 children
        assertEquals(0, metrics.get("Child1").getNoc());
        assertEquals(0, metrics.get("Child2").getNoc());
        assertEquals(0, metrics.get("Child3").getNoc());
    }

    @Test
    void testCBO_NoCoupling() {
        String code = """
            public class IsolatedClass {
                private int value;

                public void setValue(int v) {
                    this.value = v;
                }

                public int getValue() {
                    return this.value;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("IsolatedClass");
        // Should have minimal coupling (primitives don't count)
        assertTrue(ckMetrics.getCbo() <= 1, "Isolated class should have low CBO");
    }

    @Test
    void testCBO_HighCoupling() {
        String code = """
            public class CoupledClass {
                private List<String> list;
                private Map<String, Integer> map;
                private Set<Long> set;

                public void process(Database db) {
                    Logger logger = new Logger();
                    Config config = new Config();
                    Helper helper = new Helper();

                    logger.log("Processing");
                    config.get("key");
                    helper.help();
                    db.query();
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("CoupledClass");
        // Should detect multiple dependencies
        assertTrue(ckMetrics.getCbo() >= 5, "Coupled class should have high CBO");
    }

    @Test
    void testRFC_SimpleClass() {
        String code = """
            public class SimpleClass {
                public void method1() {
                    localHelper();
                }

                private void localHelper() {
                    // local method
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("SimpleClass");
        // RFC = number of methods (2)
        assertEquals(2, ckMetrics.getRfc());
    }

    @Test
    void testRFC_WithExternalCalls() {
        String code = """
            public class ClassWithExternalCalls {
                private Logger logger;
                private Database db;

                public void process() {
                    logger.info("Starting");
                    db.connect();
                    db.query("SELECT * FROM users");
                    db.disconnect();
                    logger.info("Done");
                }

                public void validate() {
                    logger.warn("Validating");
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("ClassWithExternalCalls");
        // RFC = own methods (2) + external calls (5+)
        assertTrue(ckMetrics.getRfc() >= 6, "RFC should include external methods");
    }

    @Test
    void testLCOM_HighCohesion() {
        String code = """
            public class CohesiveClass {
                private int value;
                private String name;

                public void setValue(int v) {
                    this.value = v;
                }

                public int getValue() {
                    return this.value;
                }

                public void setName(String n) {
                    this.name = n;
                }

                public String getName() {
                    return this.name;
                }

                public String toString() {
                    return name + ": " + value;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("CohesiveClass");
        // Low LCOM indicates high cohesion (methods share fields)
        assertTrue(ckMetrics.getLcom() <= 10, "Cohesive class should have low LCOM");
    }

    @Test
    void testLCOM_LowCohesion() {
        String code = """
            public class NonCohesiveClass {
                private int field1;
                private int field2;
                private int field3;

                public void method1() {
                    field1 = 1;
                }

                public void method2() {
                    field2 = 2;
                }

                public void method3() {
                    field3 = 3;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("NonCohesiveClass");
        // High LCOM indicates low cohesion (methods don't share fields)
        assertTrue(ckMetrics.getLcom() > 0, "Non-cohesive class should have LCOM > 0");
    }

    @Test
    void testGodClassDetection() {
        String code = """
            public class GodClass {
                private int f1, f2, f3, f4, f5;

                public void method1() {
                    if (true) {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(i);
                        }
                    }
                }

                public void method2() {
                    f1 = 1;
                }

                public void method3() {
                    f2 = 2;
                }

                public void method4() {
                    f3 = 3;
                }

                public void method5() {
                    f4 = 4;
                }

                public void processWithDeps(Logger log, Database db, Config cfg, Cache cache) {
                    log.info("test");
                    db.query("SELECT");
                    cfg.get("key");
                    cache.put("k", "v");
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("GodClass");
        assertNotNull(ckMetrics);

        // Should have high WMC (complex methods)
        assertTrue(ckMetrics.getWmc() > 5, "God class should have high WMC");

        // Should have some coupling
        assertTrue(ckMetrics.getCbo() > 0, "God class should have coupling");
    }

    @Test
    void testQualityScore() {
        String code = """
            public class WellDesignedClass {
                private int value;

                public void setValue(int v) {
                    this.value = v;
                }

                public int getValue() {
                    return value;
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("WellDesignedClass");

        // Well-designed class should have high quality score
        int score = ckMetrics.getQualityScore();
        assertTrue(score >= 85, "Well-designed class should have score >= 85, got: " + score);
    }

    @Test
    void testSkipsInterfaces() {
        String code = """
            public interface MyInterface {
                void method();
            }

            public class MyClass implements MyInterface {
                public void method() {
                    System.out.println("impl");
                }
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        // Should only have metrics for class, not interface
        assertNull(metrics.get("MyInterface"));
        assertNotNull(metrics.get("MyClass"));
    }

    @Test
    void testToString() {
        String code = """
            public class TestClass {
                public void method() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("TestClass");
        String str = ckMetrics.toString();

        // Verify toString contains key information
        assertTrue(str.contains("TestClass"));
        assertTrue(str.contains("WMC"));
        assertTrue(str.contains("DIT"));
        assertTrue(str.contains("NOC"));
        assertTrue(str.contains("CBO"));
        assertTrue(str.contains("RFC"));
        assertTrue(str.contains("LCOM"));
        assertTrue(str.contains("Quality Score"));
    }

    @Test
    void testCompactString() {
        String code = """
            public class TestClass {
                public void method() {}
            }
            """;

        CompilationUnit cu = parseCode(code);
        Map<String, CKMetrics> metrics = calculator.calculateAll(cu, "test.java");

        CKMetrics ckMetrics = metrics.get("TestClass");
        String compact = ckMetrics.toCompactString();

        // Verify compact format
        assertTrue(compact.contains("TestClass"));
        assertTrue(compact.contains("WMC="));
        assertTrue(compact.contains("Score:"));
    }

    // Helper method
    private CompilationUnit parseCode(String code) {
        ParseResult<CompilationUnit> result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Code should parse successfully");
        assertTrue(result.getResult().isPresent(), "Parse result should be present");
        return result.getResult().get();
    }
}
