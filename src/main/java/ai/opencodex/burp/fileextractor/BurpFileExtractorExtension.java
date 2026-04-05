package ai.opencodex.burp.fileextractor;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

/**
 * Entry point wired by the Montoya service loader. Keeps initialization light and
 * delegates to {@link ExtensionCoordinator} so future components stay isolated from
 * Burp's API surface.
 */
public final class BurpFileExtractorExtension implements BurpExtension {

    static final String EXTENSION_NAME = "Burp File Extractor";

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName(EXTENSION_NAME);

        ExtensionConfig config = new ExtensionConfig();
        ArtifactCache cache = ArtifactCache.withCapacity(config.cacheLimitBytes());
        ArtifactDetector detector = new ArtifactDetector();
        ArtifactLauncher launcher = new ArtifactLauncher();

        ExtensionCoordinator coordinator = new ExtensionCoordinator(api, config, cache, detector, launcher);
        coordinator.bootstrap();
    }
}
