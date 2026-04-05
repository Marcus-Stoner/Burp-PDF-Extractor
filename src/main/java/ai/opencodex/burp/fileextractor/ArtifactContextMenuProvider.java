package ai.opencodex.burp.fileextractor;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

final class ArtifactContextMenuProvider implements ContextMenuItemsProvider {

    private final ArtifactRegistry registry;
    private final ArtifactLauncher launcher;
    private final Logging logging;

    ArtifactContextMenuProvider(ArtifactRegistry registry, ArtifactLauncher launcher, Logging logging) {
        this.registry = registry;
        this.launcher = launcher;
        this.logging = logging;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        Optional<HttpRequestResponse> selection = extractSelection(event);
        if (selection.isEmpty()) {
            return List.of();
        }

        Optional<ArtifactRegistry.ResolvedArtifact> resolved = registry.resolve(selection.get());
        if (resolved.isEmpty()) {
            return List.of();
        }

        ArtifactRegistry.ResolvedArtifact artifact = resolved.get();

        JMenu root = new JMenu("Burp File Extractor");
        JMenuItem openItem = new JMenuItem("Open via Default App");
        openItem.addActionListener(e -> openArtifact(artifact));
        root.add(openItem);

        JMenuItem openWith = new JMenuItem("Open With...");
        openWith.addActionListener(e -> openWith(artifact));
        root.add(openWith);

        JMenuItem saveAs = new JMenuItem("Save As...");
        saveAs.addActionListener(e -> saveAs(artifact));
        root.add(saveAs);

        return new ArrayList<>(List.of(root));
    }

    private void openArtifact(ArtifactRegistry.ResolvedArtifact artifact) {
        try {
            launcher.launchWithDefaultApp(artifact.snapshot());
        } catch (IOException ex) {
            logging.logToError("Failed to open artifact: " + ex.getMessage());
        }
    }

    private void openWith(ArtifactRegistry.ResolvedArtifact artifact) {
        try {
            launcher.openWithChooser(artifact.snapshot(), null);
        } catch (IOException ex) {
            logging.logToError("Failed to open artifact with chosen application: " + ex.getMessage());
        }
    }

    private void saveAs(ArtifactRegistry.ResolvedArtifact artifact) {
        try {
            launcher.saveAs(artifact.snapshot(), null);
        } catch (IOException ex) {
            logging.logToError("Failed to save artifact: " + ex.getMessage());
        }
    }

    private Optional<HttpRequestResponse> extractSelection(ContextMenuEvent event) {
        Optional<MessageEditorHttpRequestResponse> editorSelection = event.messageEditorRequestResponse();
        if (editorSelection.isPresent()) {
            return Optional.of(editorSelection.get().requestResponse());
        }
        return event.selectedRequestResponses().stream().findFirst();
    }
}
