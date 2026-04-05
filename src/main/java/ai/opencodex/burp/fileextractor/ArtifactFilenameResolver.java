package ai.opencodex.burp.fileextractor;

import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.requests.HttpRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ArtifactFilenameResolver {

    private static final Pattern CONTENT_DISPOSITION_FILENAME = Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    private ArtifactFilenameResolver() {
    }

    static String deriveFilename(HttpResponseReceived response, String fallbackExtension) {
        String contentDisposition = response.headerValue("Content-Disposition");
        if (contentDisposition != null) {
            Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(contentDisposition);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        HttpRequest initiatingRequest = response.initiatingRequest();
        if (initiatingRequest != null) {
            String candidate = extractBasename(initiatingRequest.url());
            if (!candidate.isBlank()) {
                return candidate;
            }
        }

        String extension = fallbackExtension;
        if (extension != null && extension.contains("/")) {
            extension = extension.substring(extension.indexOf('/') + 1);
        }
        if (extension != null && extension.contains(";")) {
            extension = extension.substring(0, extension.indexOf(';'));
        }

        if (extension == null || extension.isBlank()) {
            extension = "bin";
        }

        return "artifact." + extension.toLowerCase(Locale.ROOT);
    }

    private static String extractBasename(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "";
            }
            String filename = Paths.get(path).getFileName().toString();
            return filename == null ? "" : filename;
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
