package ai.opencodex.burp.fileextractor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

final class ArtifactDetector {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    Optional<ArtifactSnapshot> detect(byte[] body, String contentType, String filenameHint) {
        if (body == null || body.length == 0) {
            return Optional.empty();
        }

        if (looksLikePdf(body, contentType)) {
            String resolvedMime = contentType == null ? "application/pdf" : contentType;
            return Optional.of(new ArtifactSnapshot(body, resolvedMime, filenameHint));
        }

        return Optional.empty();
    }

    private boolean looksLikePdf(byte[] body, String contentType) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        if (body.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (body[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
