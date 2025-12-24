package com.pragmite.cache;

import com.pragmite.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for analysis results.
 * Caches results based on file content hash to avoid re-analyzing unchanged files.
 *
 * Features:
 * - Content-based hashing (SHA-256, not MD5 for better collision resistance)
 * - Thread-safe concurrent access
 * - Automatic invalidation on file modification
 * - LRU eviction policy with configurable max size
 * - Memory-efficient (stores only essential data)
 */
public class AnalysisCache {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisCache.class);
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final int MAX_ALLOWED_SIZE = 10000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int maxSize;
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public AnalysisCache(boolean enabled) {
        this(enabled, DEFAULT_MAX_SIZE);
    }

    public AnalysisCache(boolean enabled, int maxSize) {
        this.enabled = enabled;
        this.maxSize = Math.min(maxSize, MAX_ALLOWED_SIZE);
        logger.info("Analysis cache initialized (enabled: {}, maxSize: {})", enabled, this.maxSize);
    }

    /**
     * Gets cached result for a file if available and still valid.
     */
    public AnalysisResult get(String filePath, String content) {
        if (!enabled) return null;

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                misses++;
                return null;
            }

            String contentHash = computeHash(content);
            CacheEntry entry = cache.get(filePath);

            if (entry != null && entry.contentHash.equals(contentHash)) {
                FileTime lastModified = Files.getLastModifiedTime(path);
                if (lastModified.equals(entry.lastModified)) {
                    // Update access time for LRU
                    entry.updateAccessTime();
                    hits++;
                    logger.debug("Cache hit for: {} (hit rate: {:.1f}%)",
                               filePath, getHitRate() * 100);
                    return entry.result;
                }
            }

            misses++;
            logger.debug("Cache miss for: {} (hit rate: {:.1f}%)",
                       filePath, getHitRate() * 100);
            return null;

        } catch (IOException e) {
            logger.warn("Error checking cache for {}: {}", filePath, e.getMessage());
            misses++;
            return null;
        }
    }

    /**
     * Stores analysis result in cache.
     */
    public void put(String filePath, String content, AnalysisResult result) {
        if (!enabled) return;

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return;

            // Check if we need to evict entries
            if (cache.size() >= maxSize) {
                evictLRU();
            }

            String contentHash = computeHash(content);
            FileTime lastModified = Files.getLastModifiedTime(path);

            cache.put(filePath, new CacheEntry(contentHash, lastModified, result));
            logger.debug("Cached result for: {} (size: {}/{})", filePath, cache.size(), maxSize);

        } catch (IOException e) {
            logger.warn("Error caching result for {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Evicts the least recently used entry from cache.
     */
    private void evictLRU() {
        if (cache.isEmpty()) return;

        String oldestKey = null;
        long oldestAccessTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccessTime < oldestAccessTime) {
                oldestAccessTime = entry.getValue().lastAccessTime;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            evictions++;
            logger.debug("Evicted LRU entry: {} (total evictions: {})", oldestKey, evictions);
        }
    }

    /**
     * Invalidates cache entry for a specific file.
     */
    public void invalidate(String filePath) {
        if (cache.remove(filePath) != null) {
            logger.debug("Invalidated cache for: {}", filePath);
        }
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("Cache cleared ({} entries removed)", size);
    }

    /**
     * Gets cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), enabled, maxSize, hits, misses, evictions, getHitRate());
    }

    /**
     * Calculates current hit rate.
     */
    private double getHitRate() {
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    private String computeHash(String content) {
        try {
            // Use SHA-256 instead of MD5 for better collision resistance
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return content.hashCode() + ""; // Fallback to simple hash
        }
    }

    private static class CacheEntry {
        final String contentHash;
        final FileTime lastModified;
        final AnalysisResult result;
        long lastAccessTime;

        CacheEntry(String contentHash, FileTime lastModified, AnalysisResult result) {
            this.contentHash = contentHash;
            this.lastModified = lastModified;
            this.result = result;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    public static class CacheStats {
        private final int size;
        private final boolean enabled;
        private final int maxSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;

        public CacheStats(int size, boolean enabled, int maxSize, long hits, long misses, long evictions, double hitRate) {
            this.size = size;
            this.enabled = enabled;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        public int getSize() {
            return size;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getEvictions() {
            return evictions;
        }

        public double getHitRate() {
            return hitRate;
        }

        @Override
        public String toString() {
            if (!enabled) {
                return "Cache: disabled";
            }
            return String.format("Cache: enabled, %d/%d entries, hit rate: %.1f%% (%d hits, %d misses), %d evictions",
                               size, maxSize, hitRate * 100, hits, misses, evictions);
        }
    }
}
