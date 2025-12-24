package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpeculativeGeneralityDetectorTest {

    private SpeculativeGeneralityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SpeculativeGeneralityDetector();
    }

    @Test
    void shouldDetectUnnecessaryAbstractClass() {
        String code = """
            public abstract class BaseProcessor {
                public abstract void process();
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "BaseProcessor.java", code);

        assertTrue(smells.size() > 0, "Should detect unnecessary abstract class");
        assertTrue(smells.stream().anyMatch(s ->
            s.getType() == CodeSmellType.SPECULATIVE_GENERALITY &&
            s.getMessage().contains("abstract")));
    }

    @Test
    void shouldDetectMarkerInterface() {
        String code = """
            public interface Serializable {
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Serializable.java", code);

        assertTrue(smells.size() > 0, "Should detect marker interface");
        assertTrue(smells.stream().anyMatch(s ->
            s.getMessage().contains("marker interface")));
    }

    @Test
    void shouldDetectUnusedParameter() {
        String code = """
            public class Calculator {
                public int add(int a, int b, int unused) {
                    return a + b;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Calculator.java", code);

        assertTrue(smells.size() > 0, "Should detect unused parameter");
        assertTrue(smells.stream().anyMatch(s ->
            s.getMessage().contains("unused")));
    }

    @Test
    void shouldNotDetectIntentionallyUnusedParameter() {
        String code = """
            public class Handler {
                public void handle(String message, int _reserved) {
                    System.out.println(message);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Handler.java", code);

        // Should not flag _reserved (intentionally unused, marked with _)
        assertFalse(smells.stream().anyMatch(s ->
            s.getMessage().contains("_reserved")));
    }

    @Test
    void shouldDetectOverlyGenericMethodName() {
        String code = """
            public class Service {
                public void handleData() {
                    int x = 1;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Service.java", code);

        assertTrue(smells.size() > 0, "Should detect overly generic method name");
        assertTrue(smells.stream().anyMatch(s ->
            s.getMessage().contains("handleData") &&
            s.getMessage().contains("generic")));
    }

    @Test
    void shouldNotDetectGoodAbstractClass() {
        String code = """
            public abstract class Animal {
                protected String name;

                public abstract void makeSound();

                public String getName() {
                    return name;
                }

                public void sleep() {
                    System.out.println("Sleeping");
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Animal.java", code);

        assertEquals(0, smells.size(), "Should not detect well-designed abstract class with shared behavior");
    }
}
