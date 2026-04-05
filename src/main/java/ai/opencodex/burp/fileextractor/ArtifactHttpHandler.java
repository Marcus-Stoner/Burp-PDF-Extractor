package ai.opencodex.burp.fileextractor;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.logging.Logging;
import java.io.IOException;
import java.util.Optional;

final class ArtifactHttpHandler implements HttpHandler {

    private final ArtifactDetector detector;
    private final ArtifactRegistry registry;
    private final ArtifactLauncher launcher;
    private final ExtensionConfig config;
    private final Logging logging;

    ArtifactHttpHandler(ArtifactDetector detector,
                        ArtifactRegistry registry,
                        ArtifactLauncher launcher,
                        ExtensionConfig config,
                        Logging logging) {
        this.detector = detector;
        this.registry = registry;
        this.launcher = launcher;
        this.config = config;
        this.logging = logging;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        try {
            byte[] body = response.body().getBytes();
            if (body.length == 0) {
                return ResponseReceivedAction.continueWith(response);
            }

            String contentType = response.headerValue("Content-Type");
            String filenameHint = ArtifactFilenameResolver.deriveFilename(response, contentType);

            Optional<ArtifactSnapshot> detection = detector.detect(body, contentType, filenameHint);
            if (detection.isEmpty()) {
                return ResponseReceivedAction.continueWith(response);
            }

            String digest = ArtifactDigest.sha256(body);
            ArtifactSnapshot snapshot = detection.get();

            if (!config.isMimeAllowed(snapshot.mimeType())) {
                return ResponseReceivedAction.continueWith(response);
            }

            registry.store(response, digest, snapshot, response.initiatingRequest() != null ? response.initiatingRequest().url() : "");

            if (config.autoOpenEnabled()) {
                try {
                    launcher.launchWithDefaultApp(snapshot);
                } catch (IOException ex) {
                    logging.logToError("Auto-open failed: " + ex.getMessage());
                }
            }

            if (config.highlightEnabled()) {
                HighlightColor color = config.pdfHighlightColor();
                if (color != null) {
                    Annotations annotations = response.annotations();
                    if (annotations == null) {
                        annotations = Annotations.annotations(color);
                    } else {
                        annotations = annotations.withHighlightColor(color);
                    }
                    return ResponseReceivedAction.continueWith(response, annotations);
                }
            }
        } catch (Exception ex) {
            logging.logToError("Artifact detection failed: " + ex.getMessage());
        }

        return ResponseReceivedAction.continueWith(response);
    }
}
