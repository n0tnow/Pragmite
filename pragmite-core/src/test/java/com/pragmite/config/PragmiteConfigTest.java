package com.pragmite.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PragmiteConfigTest {

    @Test
    void testDefaultConfiguration() {
        PragmiteConfig config = PragmiteConfig.loadConfig();

        assertNotNull(config);
        assertEquals(30, config.getLongMethodThreshold());
        assertEquals(4, config.getLongParameterListThreshold());
        assertEquals(10, config.getComplexityThreshold());
        assertTrue(config.isEnableCaching());
        assertTrue(config.isCreateBackups());
    }

    @Test
    void testLoadFromYamlFile(@TempDir Path tempDir) throws Exception {
        String yamlContent = """
            thresholds:
              long_method: 50
              complexity: 15

            refactoring:
              auto_apply: true
              create_backups: false

            performance:
              enable_caching: false
              thread_count: 8
            """;

        Path configFile = tempDir.resolve(".pragmite.yaml");
        Files.writeString(configFile, yamlContent);

        PragmiteConfig config = PragmiteConfig.loadConfig(tempDir);

        assertEquals(50, config.getLongMethodThreshold());
        assertEquals(15, config.getComplexityThreshold());
        assertTrue(config.isAutoApplyRefactorings());
        assertFalse(config.isCreateBackups());
        assertFalse(config.isEnableCaching());
        assertEquals(8, config.getParallelThreadCount());
    }

    @Test
    void testConfigWithMissingValues(@TempDir Path tempDir) throws Exception {
        String yamlContent = """
            thresholds:
              long_method: 100
            """;

        Path configFile = tempDir.resolve(".pragmite.yaml");
        Files.writeString(configFile, yamlContent);

        PragmiteConfig config = PragmiteConfig.loadConfig(tempDir);

        // Should have custom value
        assertEquals(100, config.getLongMethodThreshold());

        // Should have defaults for missing values
        assertEquals(4, config.getLongParameterListThreshold());
        assertEquals(10, config.getComplexityThreshold());
    }

    @Test
    void testConfigToString() {
        PragmiteConfig config = PragmiteConfig.loadConfig();
        String configStr = config.toString();

        assertNotNull(configStr);
        assertTrue(configStr.contains("PragmiteConfig"));
        assertTrue(configStr.contains("longMethod"));
    }
}
