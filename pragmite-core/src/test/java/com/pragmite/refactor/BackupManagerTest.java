package com.pragmite.refactor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupManagerTest {

    @TempDir
    Path tempDir;

    private BackupManager backupManager;
    private Path testFile;
    private Path backupRoot;

    @BeforeEach
    void setUp() throws IOException {
        backupRoot = tempDir.resolve(".pragmite/backups");
        backupManager = new BackupManager(backupRoot);

        // Create a test file
        testFile = tempDir.resolve("test.java");
        Files.writeString(testFile, "public class Test { }");
    }

    @AfterEach
    void tearDown() {
        // Cleanup is automatic with @TempDir
    }

    @Test
    void testCreateBackupForSingleFile() throws IOException {
        String backupId = backupManager.createBackup(testFile.toString());

        assertNotNull(backupId);
        assertTrue(backupId.startsWith("backup-"));

        // Verify backup directory was created
        Path backupDir = backupRoot.resolve(backupId);
        assertTrue(Files.exists(backupDir));
        assertTrue(Files.isDirectory(backupDir));

        // Verify file was backed up (excluding metadata files)
        List<Path> backedUpFiles = new ArrayList<>();
        Files.walk(backupDir)
            .filter(Files::isRegularFile)
            .filter(p -> !p.getFileName().toString().equals("backup-paths.txt"))
            .forEach(backedUpFiles::add);

        assertEquals(1, backedUpFiles.size());
    }

    @Test
    void testCreateBackupForNonExistentFile() {
        String nonExistentFile = tempDir.resolve("nonexistent.java").toString();

        IOException exception = assertThrows(IOException.class, () -> {
            backupManager.createBackup(nonExistentFile);
        });

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    void testCreateBackupForRefactoringPlan() throws IOException {
        // Create multiple test files
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "public class File1 { }");
        Files.writeString(file2, "public class File2 { }");

        // Create refactoring plan
        List<String> affectedFiles = new ArrayList<>();
        affectedFiles.add(file1.toString());
        affectedFiles.add(file2.toString());

        TestRefactoringPlan plan = new TestRefactoringPlan(affectedFiles);

        // Create backup
        String backupId = backupManager.createBackup(plan);

        assertNotNull(backupId);

        // Verify both files were backed up
        Path backupDir = backupRoot.resolve(backupId);
        assertTrue(Files.exists(backupDir));

        List<Path> backedUpFiles = new ArrayList<>();
        Files.walk(backupDir)
            .filter(Files::isRegularFile)
            .filter(p -> !p.getFileName().toString().equals("backup-metadata.txt"))
            .filter(p -> !p.getFileName().toString().equals("backup-paths.txt"))
            .forEach(backedUpFiles::add);

        assertEquals(2, backedUpFiles.size());

        // Verify metadata file was created
        Path metadataFile = backupDir.resolve("backup-metadata.txt");
        assertTrue(Files.exists(metadataFile));
        String metadata = Files.readString(metadataFile);
        assertTrue(metadata.contains("Backup created:"));
        assertTrue(metadata.contains("Affected files:"));
    }

    @Test
    void testRestore() throws IOException {
        // Create backup
        String originalContent = "public class Test { }";
        Files.writeString(testFile, originalContent);
        String backupId = backupManager.createBackup(testFile.toString());

        // Modify the file
        String modifiedContent = "public class TestModified { }";
        Files.writeString(testFile, modifiedContent);
        assertEquals(modifiedContent, Files.readString(testFile));

        // Restore from backup
        backupManager.restore(backupId);

        // Verify file was restored
        assertEquals(originalContent, Files.readString(testFile));
    }

    @Test
    void testRestoreNonExistentBackup() {
        IOException exception = assertThrows(IOException.class, () -> {
            backupManager.restore("nonexistent-backup");
        });

        assertTrue(exception.getMessage().contains("Backup not found"));
    }

    @Test
    void testListBackups() throws IOException, InterruptedException {
        // Initially no backups
        List<String> backups = backupManager.listBackups();
        assertEquals(0, backups.size());

        // Create some backups
        String backupId1 = backupManager.createBackup(testFile.toString());
        Thread.sleep(10); // Ensure different timestamps
        String backupId2 = backupManager.createBackup(testFile.toString());

        // List backups
        backups = backupManager.listBackups();
        assertEquals(2, backups.size());

        // Verify they're sorted descending (newest first)
        assertTrue(backups.contains(backupId1));
        assertTrue(backups.contains(backupId2));
        assertTrue(backups.get(0).compareTo(backups.get(1)) > 0);
    }

    @Test
    void testDeleteBackup() throws IOException {
        // Create backup
        String backupId = backupManager.createBackup(testFile.toString());

        // Verify it exists
        List<String> backups = backupManager.listBackups();
        assertEquals(1, backups.size());

        // Delete backup
        backupManager.deleteBackup(backupId);

        // Verify it's deleted
        backups = backupManager.listBackups();
        assertEquals(0, backups.size());
        assertFalse(Files.exists(backupRoot.resolve(backupId)));
    }

    @Test
    void testDeleteNonExistentBackup() {
        IOException exception = assertThrows(IOException.class, () -> {
            backupManager.deleteBackup("nonexistent-backup");
        });

        assertTrue(exception.getMessage().contains("Backup not found"));
    }

    @Test
    void testCleanupOldBackups() throws IOException, InterruptedException {
        // Create 5 backups
        List<String> backupIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String backupId = backupManager.createBackup(testFile.toString());
            backupIds.add(backupId);
            Thread.sleep(10); // Ensure different timestamps
        }

        // Verify all 5 exist
        assertEquals(5, backupManager.listBackups().size());

        // Keep only 2 most recent
        backupManager.cleanupOldBackups(2);

        // Verify only 2 remain
        List<String> remainingBackups = backupManager.listBackups();
        assertEquals(2, remainingBackups.size());

        // Verify the 2 most recent backups remain (last 2 created)
        assertTrue(remainingBackups.contains(backupIds.get(3)));
        assertTrue(remainingBackups.contains(backupIds.get(4)));

        // Verify the oldest 3 were deleted
        assertFalse(remainingBackups.contains(backupIds.get(0)));
        assertFalse(remainingBackups.contains(backupIds.get(1)));
        assertFalse(remainingBackups.contains(backupIds.get(2)));
    }

    @Test
    void testCleanupOldBackupsWhenCountMatchesKeepCount() throws IOException {
        // Create 3 backups
        for (int i = 0; i < 3; i++) {
            backupManager.createBackup(testFile.toString());
        }

        // Keep 3 backups (same as current count)
        backupManager.cleanupOldBackups(3);

        // Verify all 3 still exist
        assertEquals(3, backupManager.listBackups().size());
    }

    @Test
    void testCleanupOldBackupsWhenCountLessThanKeepCount() throws IOException {
        // Create 2 backups
        backupManager.createBackup(testFile.toString());
        backupManager.createBackup(testFile.toString());

        // Try to keep 5 backups (more than current)
        backupManager.cleanupOldBackups(5);

        // Verify both still exist
        assertEquals(2, backupManager.listBackups().size());
    }

    @Test
    void testMultipleFilesRestore() throws IOException {
        // Create multiple files
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        String content1 = "public class File1 { }";
        String content2 = "public class File2 { }";
        Files.writeString(file1, content1);
        Files.writeString(file2, content2);

        // Create refactoring plan
        List<String> affectedFiles = new ArrayList<>();
        affectedFiles.add(file1.toString());
        affectedFiles.add(file2.toString());
        TestRefactoringPlan plan = new TestRefactoringPlan(affectedFiles);

        // Create backup
        String backupId = backupManager.createBackup(plan);

        // Modify both files
        Files.writeString(file1, "modified1");
        Files.writeString(file2, "modified2");

        // Restore from backup
        backupManager.restore(backupId);

        // Verify both files were restored
        assertEquals(content1, Files.readString(file1));
        assertEquals(content2, Files.readString(file2));
    }

    @Test
    void testBackupWithMissingFileInPlan() throws IOException {
        // Create one existing file and one non-existent file
        Path existingFile = tempDir.resolve("Existing.java");
        Files.writeString(existingFile, "public class Existing { }");

        Path nonExistentFile = tempDir.resolve("NonExistent.java");

        // Create refactoring plan with both files
        List<String> affectedFiles = new ArrayList<>();
        affectedFiles.add(existingFile.toString());
        affectedFiles.add(nonExistentFile.toString());

        TestRefactoringPlan plan = new TestRefactoringPlan(affectedFiles);

        // Create backup - should succeed but skip non-existent file
        String backupId = backupManager.createBackup(plan);

        assertNotNull(backupId);

        // Verify only the existing file was backed up
        Path backupDir = backupRoot.resolve(backupId);
        List<Path> backedUpFiles = new ArrayList<>();
        Files.walk(backupDir)
            .filter(Files::isRegularFile)
            .filter(p -> !p.getFileName().toString().equals("backup-metadata.txt"))
            .filter(p -> !p.getFileName().toString().equals("backup-paths.txt"))
            .forEach(backedUpFiles::add);

        assertEquals(1, backedUpFiles.size());
    }

    /**
     * Test implementation of RefactoringPlan.
     */
    private static class TestRefactoringPlan extends RefactoringPlan {
        private final List<String> affectedFiles;

        public TestRefactoringPlan(List<String> affectedFiles) {
            this.affectedFiles = affectedFiles;
        }

        @Override
        public List<String> getAffectedFiles() {
            return affectedFiles;
        }

        @Override
        public String toString() {
            return "TestRefactoringPlan{" +
                   "affectedFiles=" + affectedFiles.size() +
                   '}';
        }
    }
}
