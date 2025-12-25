package com.pragmite.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent cache manager for incremental analysis.
 * Stores file hashes and analysis timestamps to skip unchanged files.
 */
public class CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private static final String CACHE_FILE_NAME = ".pragmite-cache.json";
    private static final int MAX_CACHE_AGE_DAYS = 30;

    private final Path cacheFilePath;
    private final Gson gson;
    private Map<String, CachedFileInfo> cache;

    public CacheManager(Path projectRoot) {
        this.cacheFilePath = projectRoot.resolve(CACHE_FILE_NAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new HashMap<>();
        loadCache();
    }

    /**
     * Loads cache from disk.
     */
    private void loadCache() {
        if (!Files.exists(cacheFilePath)) {
            logger.debug("Cache file not found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(cacheFilePath, StandardCharsets.UTF_8);
            cache = gson.fromJson(json, new TypeToken<Map<String, CachedFileInfo>>(){}.getType());
            if (cache == null) {
                cache = new HashMap<>();
            }
            pruneOldEntries();
            logger.info("Loaded cache with {} entries", cache.size());
        } catch (IOException e) {
            logger.warn("Failed to load cache: {}", e.getMessage());
            cache = new HashMap<>();
        }
    }

    /**
     * Saves cache to disk.
     */
    public void saveCache() {
        try {
            String json = gson.toJson(cache);
            Files.writeString(cacheFilePath, json, StandardCharsets.UTF_8);
            logger.debug("Saved cache with {} entries", cache.size());
        } catch (IOException e) {
            logger.error("Failed to save cache: {}", e.getMessage());
        }
    }

    /**
     * Checks if a file has changed since last analysis.
     */
    public boolean hasChanged(Path filePath) throws IOException {
        String fileHash = calculateFileHash(filePath);
        String key = filePath.toString();

        CachedFileInfo cached = cache.get(key);
        if (cached == null) {
            return true; // File not in cache
        }

        return !fileHash.equals(cached.hash);
    }

    /**
     * Marks a file as analyzed with current hash.
     */
    public void markAsAnalyzed(Path filePath) throws IOException {
        String fileHash = calculateFileHash(filePath);
        String key = filePath.toString();

        CachedFileInfo info = new CachedFileInfo();
        info.hash = fileHash;
        info.lastAnalyzed = LocalDateTime.now();

        cache.put(key, info);
    }

    /**
     * Calculates SHA-256 hash of file content.
     */
    private String calculateFileHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Removes entries older than MAX_CACHE_AGE_DAYS.
     */
    private void pruneOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(MAX_CACHE_AGE_DAYS);
        int sizeBefore = cache.size();

        cache.entrySet().removeIf(entry ->
            entry.getValue().lastAnalyzed.isBefore(cutoff)
        );

        int removed = sizeBefore - cache.size();
        if (removed > 0) {
            logger.info("Pruned {} old cache entries", removed);
        }
    }

    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        cache.clear();
        try {
            Files.deleteIfExists(cacheFilePath);
            logger.info("Cache cleared");
        } catch (IOException e) {
            logger.error("Failed to delete cache file: {}", e.getMessage());
        }
    }

    /**
     * Gets cache statistics.
     */
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.totalEntries = cache.size();
        stats.cacheFilePath = cacheFilePath.toString();
        return stats;
    }

    /**
     * Cached file information.
     */
    private static class CachedFileInfo {
        String hash;
        LocalDateTime lastAnalyzed;
    }

    /**
     * Cache statistics.
     */
    public static class CacheStats {
        public int totalEntries;
        public String cacheFilePath;

        @Override
        public String toString() {
            return String.format("Cache: %d entries, location: %s", totalEntries, cacheFilePath);
        }
    }
}
