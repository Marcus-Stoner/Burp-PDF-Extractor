package ai.opencodex.burp.fileextractor;

import java.util.Arrays;
import java.util.Objects;

final class ArtifactSnapshot {

    private final byte[] bytes;
    private final String mimeType;
    private final String suggestedFilename;
    private final int size;

    ArtifactSnapshot(byte[] bytes, String mimeType, String suggestedFilename) {
        Objects.requireNonNull(bytes, "bytes");
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.size = bytes.length;
        this.mimeType = Objects.requireNonNullElse(mimeType, "application/octet-stream");
        this.suggestedFilename = Objects.requireNonNullElse(suggestedFilename, "artifact.bin");
    }

    byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    String mimeType() {
        return mimeType;
    }

    String suggestedFilename() {
        return suggestedFilename;
    }

    int size() {
        return size;
    }
}
