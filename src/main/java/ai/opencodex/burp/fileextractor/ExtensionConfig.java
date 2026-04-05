package ai.opencodex.burp.fileextractor;

import burp.api.montoya.core.HighlightColor;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class ExtensionConfig {

    private volatile boolean autoOpenEnabled;
    private volatile long cacheLimitBytes;
    private volatile boolean highlightEnabled;
    private volatile HighlightColor pdfHighlightColor;
    private final Set<String> allowedMimeTypes = ConcurrentHashMap.newKeySet();

    ExtensionConfig() {
        this.autoOpenEnabled = false;
        this.cacheLimitBytes = 512L * 1024L * 1024L;
        this.highlightEnabled = false;
        this.pdfHighlightColor = HighlightColor.CYAN;
        allowedMimeTypes.add("application/pdf");
    }

    boolean autoOpenEnabled() {
        return autoOpenEnabled;
    }

    void setAutoOpenEnabled(boolean autoOpenEnabled) {
        this.autoOpenEnabled = autoOpenEnabled;
    }

    long cacheLimitBytes() {
        return cacheLimitBytes;
    }

    void setCacheLimitBytes(long cacheLimitBytes) {
        if (cacheLimitBytes <= 0) {
            throw new IllegalArgumentException("cacheLimitBytes must be positive");
        }
        this.cacheLimitBytes = cacheLimitBytes;
    }

    boolean highlightEnabled() {
        return highlightEnabled;
    }

    void setHighlightEnabled(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
    }

    HighlightColor pdfHighlightColor() {
        return pdfHighlightColor;
    }

    void setPdfHighlightColor(HighlightColor pdfHighlightColor) {
        if (pdfHighlightColor == null) {
            throw new IllegalArgumentException("Highlight color cannot be null");
        }
        this.pdfHighlightColor = pdfHighlightColor;
    }

    Set<String> allowedMimeTypes() {
        return Collections.unmodifiableSet(allowedMimeTypes);
    }

    void setAllowedMimeTypes(Collection<String> mimeTypes) {
        allowedMimeTypes.clear();
        if (mimeTypes == null) {
            return;
        }
        for (String mime : mimeTypes) {
            if (mime == null) {
                continue;
            }
            String trimmed = mime.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                allowedMimeTypes.add(trimmed);
            }
        }
    }

    boolean isMimeAllowed(String mimeType) {
        if (allowedMimeTypes.isEmpty()) {
            return true;
        }
        if (mimeType == null) {
            return false;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        int semicolon = normalized.indexOf(';');
        if (semicolon > -1) {
            normalized = normalized.substring(0, semicolon);
        }
        normalized = normalized.trim();
        return allowedMimeTypes.contains(normalized);
    }
}
