package com.pragmite.cache;

import com.pragmite.model.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisCacheTest {

    private AnalysisCache cache;

    @BeforeEach
    void setUp() {
        cache = new AnalysisCache(true);
    }

    @Test
    void testCacheDisabled() {
        AnalysisCache disabledCache = new AnalysisCache(false);

        AnalysisResult result = new AnalysisResult();
        disabledCache.put("test.java", "content", result);

        AnalysisResult cached = disabledCache.get("test.java", "content");
        assertNull(cached, "Disabled cache should return null");
    }

    @Test
    void testCacheHit(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("Test.java");
        String content = "public class Test {}";
        Files.writeString(testFile, content);

        AnalysisResult result = new AnalysisResult();
        result.setProjectPath(testFile.toString());

        cache.put(testFile.toString(), content, result);

        AnalysisResult cached = cache.get(testFile.toString(), content);
        assertNotNull(cached, "Should hit cache");
        assertEquals(result, cached);
    }

    @Test
    void testCacheMiss_DifferentContent(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("Test.java");
        String content1 = "public class Test {}";
        String content2 = "public class Test { int x; }";
        Files.writeString(testFile, content1);

        AnalysisResult result = new AnalysisResult();
        cache.put(testFile.toString(), content1, result);

        // Update file
        Files.writeString(testFile, content2);

        AnalysisResult cached = cache.get(testFile.toString(), content2);
        assertNull(cached, "Should miss cache due to content change");
    }

    @Test
    void testInvalidate(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("Test.java");
        String content = "public class Test {}";
        Files.writeString(testFile, content);

        AnalysisResult result = new AnalysisResult();
        cache.put(testFile.toString(), content, result);

        cache.invalidate(testFile.toString());

        AnalysisResult cached = cache.get(testFile.toString(), content);
        assertNull(cached, "Should miss cache after invalidation");
    }

    @Test
    void testClear(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("Test1.java");
        Path file2 = tempDir.resolve("Test2.java");
        Files.writeString(file1, "public class Test1 {}");
        Files.writeString(file2, "public class Test2 {}");

        AnalysisResult result1 = new AnalysisResult();
        AnalysisResult result2 = new AnalysisResult();

        cache.put(file1.toString(), "public class Test1 {}", result1);
        cache.put(file2.toString(), "public class Test2 {}", result2);

        AnalysisCache.CacheStats statsBefore = cache.getStats();
        assertTrue(statsBefore.getSize() > 0);

        cache.clear();

        AnalysisCache.CacheStats statsAfter = cache.getStats();
        assertEquals(0, statsAfter.getSize());
    }

    @Test
    void testCacheStats() {
        AnalysisCache.CacheStats stats = cache.getStats();

        assertNotNull(stats);
        assertTrue(stats.isEnabled());
        assertEquals(0, stats.getSize());

        String statsStr = stats.toString();
        assertTrue(statsStr.contains("enabled"));
        assertTrue(statsStr.contains("0"));
    }

    @Test
    void testNonExistentFile() {
        AnalysisResult result = cache.get("/nonexistent/file.java", "content");
        assertNull(result, "Should return null for non-existent file");
    }
}
