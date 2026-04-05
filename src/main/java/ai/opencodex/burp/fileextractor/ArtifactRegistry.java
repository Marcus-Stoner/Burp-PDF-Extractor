package ai.opencodex.burp.fileextractor;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.HttpRequestResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class ArtifactRegistry {

    private final ArtifactCache cache;
    private final Map<String, ArtifactRecord> records = new ConcurrentHashMap<>();
    private final List<ArtifactListener> listeners = new CopyOnWriteArrayList<>();

    ArtifactRegistry(ArtifactCache cache) {
        this.cache = cache;
    }

    void store(HttpResponseReceived response, String digest, ArtifactSnapshot snapshot, String url) {
        cache.put(digest, snapshot);

        ArtifactRecord record = new ArtifactRecord(
                digest,
                snapshot.mimeType(),
                snapshot.suggestedFilename(),
                snapshot.size(),
                url,
                response.toolSource().toolType(),
                Instant.now()
        );

        records.put(digest, record);

        for (ArtifactListener listener : listeners) {
            try {
                listener.onArtifactStored(record);
            } catch (Exception ignored) {
            }
        }
    }

    Optional<ResolvedArtifact> resolve(HttpRequestResponse message) {
        return digestFor(message)
                .flatMap(digest -> Optional.ofNullable(records.get(digest))
                        .flatMap(record -> cache.get(digest)
                                .map(snapshot -> new ResolvedArtifact(record, snapshot))));
    }

    boolean hasArtifactFor(HttpRequestResponse message) {
        return digestFor(message).map(records::containsKey).orElse(false);
    }

    Optional<ResolvedArtifact> resolve(String digest) {
        if (digest == null) {
            return Optional.empty();
        }
        ArtifactRecord record = records.get(digest);
        if (record == null) {
            return Optional.empty();
        }
        return cache.get(digest).map(snapshot -> new ResolvedArtifact(record, snapshot));
    }

    Optional<ArtifactSnapshot> snapshot(String digest) {
        if (digest == null) {
            return Optional.empty();
        }
        return cache.get(digest);
    }

    List<ArtifactRecord> allRecords() {
        List<ArtifactRecord> list = new ArrayList<>(records.values());
        list.sort((a, b) -> b.capturedAt().compareTo(a.capturedAt()));
        return Collections.unmodifiableList(list);
    }

    void addListener(ArtifactListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private Optional<String> digestFor(HttpRequestResponse message) {
        if (message == null || !message.hasResponse()) {
            return Optional.empty();
        }
        byte[] body = message.response().body().getBytes();
        if (body.length == 0) {
            return Optional.empty();
        }
        return Optional.of(ArtifactDigest.sha256(body));
    }

    static final class ArtifactRecord {
        private final String digest;
        private final String mimeType;
        private final String suggestedFilename;
        private final long size;
        private final String url;
        private final ToolType toolType;
        private final Instant capturedAt;

        ArtifactRecord(String digest, String mimeType, String suggestedFilename, long size, String url, ToolType toolType, Instant capturedAt) {
            this.digest = digest;
            this.mimeType = mimeType;
            this.suggestedFilename = suggestedFilename;
            this.size = size;
            this.url = url;
            this.toolType = toolType;
            this.capturedAt = capturedAt;
        }

        String digest() {
            return digest;
        }

        String mimeType() {
            return mimeType;
        }

        String suggestedFilename() {
            return suggestedFilename;
        }

        long size() {
            return size;
        }

        String url() {
            return url;
        }

        ToolType toolType() {
            return toolType;
        }

        Instant capturedAt() {
            return capturedAt;
        }
    }

    static final class ResolvedArtifact {
        private final ArtifactRecord record;
        private final ArtifactSnapshot snapshot;

        ResolvedArtifact(ArtifactRecord record, ArtifactSnapshot snapshot) {
            this.record = record;
            this.snapshot = snapshot;
        }

        ArtifactRecord record() {
            return record;
        }

        ArtifactSnapshot snapshot() {
            return snapshot;
        }
    }

    interface ArtifactListener {
        void onArtifactStored(ArtifactRecord record);
    }
}
