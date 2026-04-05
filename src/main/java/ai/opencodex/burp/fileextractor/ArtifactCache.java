package ai.opencodex.burp.fileextractor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class ArtifactCache {

    private long capacityBytes;
    private final Map<String, CacheEntry> entries;
    private long currentBytes;

    private ArtifactCache(long capacityBytes) {
        if (capacityBytes <= 0) {
            throw new IllegalArgumentException("capacityBytes must be positive");
        }
        this.capacityBytes = capacityBytes;
        this.entries = new LinkedHashMap<>(16, 0.75f, true);
    }

    static ArtifactCache defaultCache() {
        return new ArtifactCache(512L * 1024L * 1024L);
    }

    static ArtifactCache withCapacity(long capacityBytes) {
        return new ArtifactCache(capacityBytes);
    }

    synchronized boolean isEmpty() {
        return entries.isEmpty();
    }

    synchronized Optional<ArtifactSnapshot> get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        entry.lastAccessed = Instant.now();
        return Optional.of(entry.snapshot);
    }

    synchronized void put(String key, ArtifactSnapshot snapshot) {
        CacheEntry existing = entries.remove(key);
        if (existing != null) {
            currentBytes -= existing.size;
        }

        long incomingSize = snapshot.size();
        evictIfNecessary(incomingSize);

        entries.put(key, new CacheEntry(incomingSize, snapshot));
        currentBytes += incomingSize;
    }

    synchronized void setCapacity(long newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("newCapacity must be positive");
        }
        this.capacityBytes = newCapacity;
        evictIfNecessary(0);
    }

    private void evictIfNecessary(long incomingSize) {
        while (!entries.isEmpty() && currentBytes + incomingSize > capacityBytes) {
            Map.Entry<String, CacheEntry> eldest = entries.entrySet().iterator().next();
            currentBytes -= eldest.getValue().size;
            entries.remove(eldest.getKey());
        }
    }

    private static final class CacheEntry {
        private final long size;
        private final ArtifactSnapshot snapshot;
        private Instant lastAccessed;

        CacheEntry(long size, ArtifactSnapshot snapshot) {
            this.size = size;
            this.snapshot = snapshot;
            this.lastAccessed = Instant.now();
        }
    }
}
