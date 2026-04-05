package ai.opencodex.burp.fileextractor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import java.util.ArrayList;
import java.util.List;

final class ExtensionCoordinator {

    private final MontoyaApi api;
    private final ExtensionConfig config;
    private final ArtifactCache cache;
    private final ArtifactDetector detector;
    private final ArtifactLauncher launcher;
    private final List<Registration> registrations = new ArrayList<>();

    ExtensionCoordinator(MontoyaApi api, ExtensionConfig config, ArtifactCache cache, ArtifactDetector detector, ArtifactLauncher launcher) {
        this.api = api;
        this.config = config;
        this.cache = cache;
        this.detector = detector;
        this.launcher = launcher;
    }

    void bootstrap() {
        ArtifactRegistry registry = new ArtifactRegistry(cache);

        ArtifactHttpHandler httpHandler = new ArtifactHttpHandler(detector, registry, launcher, config, api.logging());
        registrations.add(api.http().registerHttpHandler(httpHandler));

        ArtifactContextMenuProvider menuProvider = new ArtifactContextMenuProvider(registry, launcher, api.logging());
        registrations.add(api.userInterface().registerContextMenuItemsProvider(menuProvider));

        ArtifactResponseEditorProvider editorProvider = new ArtifactResponseEditorProvider(registry, launcher, api.logging());
        registrations.add(api.userInterface().registerHttpResponseEditorProvider(editorProvider));

        ExtensionOptionsPanel optionsPanel = new ExtensionOptionsPanel(config, cache, api.logging());
        registrations.add(api.userInterface().registerSettingsPanel(optionsPanel));

        ArtifactSuiteTab suiteTab = new ArtifactSuiteTab(registry, launcher, api.logging());
        registrations.add(api.userInterface().registerSuiteTab("File Extractor", suiteTab.component()));

        api.logging().logToOutput(BurpFileExtractorExtension.EXTENSION_NAME + " initialized");
    }
}
