package com.pragmite.rules.smells;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.pragmite.model.CodeSmell;
import com.pragmite.model.CodeSmellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LazyClassDetectorTest {

    private LazyClassDetector detector;

    @BeforeEach
    void setUp() {
        detector = new LazyClassDetector();
    }

    @Test
    void shouldDetectLazyClass() {
        String code = """
            public class SimpleWrapper {
                private String value;

                public void setValue(String value) {
                    this.value = value;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "SimpleWrapper.java", code);

        assertTrue(smells.size() > 0, "Should detect lazy class");
        assertTrue(smells.stream().anyMatch(s ->
            s.getType() == CodeSmellType.LAZY_CLASS &&
            s.getMessage().contains("SimpleWrapper")));
    }

    @Test
    void shouldNotDetectClassWithSubstantialLogic() {
        String code = """
            public class UserService {
                private UserRepository repository;

                public User createUser(String name, String email) {
                    validateEmail(email);
                    User user = new User(name, email);
                    return repository.save(user);
                }

                private void validateEmail(String email) {
                    if (!email.contains("@")) {
                        throw new IllegalArgumentException("Invalid email");
                    }
                }

                public void deleteUser(Long id) {
                    repository.deleteById(id);
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "UserService.java", code);

        assertEquals(0, smells.size(), "Should not detect class with substantial logic");
    }

    @Test
    void shouldNotDetectInterfaces() {
        String code = """
            public interface UserService {
                void createUser(String name);
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "UserService.java", code);

        assertEquals(0, smells.size(), "Should not detect interfaces");
    }

    @Test
    void shouldDetectMinimalClass() {
        String code = """
            public class Counter {
                private int count = 0;

                public void increment() {
                    count++;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu, "Counter.java", code);

        assertTrue(smells.size() > 0, "Should detect minimal class with trivial logic");
    }
}
