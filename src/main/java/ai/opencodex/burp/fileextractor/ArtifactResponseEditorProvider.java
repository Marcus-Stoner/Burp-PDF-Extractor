package ai.opencodex.burp.fileextractor;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;
import java.awt.BorderLayout;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

final class ArtifactResponseEditorProvider implements HttpResponseEditorProvider {

    private final ArtifactRegistry registry;
    private final ArtifactLauncher launcher;
    private final Logging logging;

    ArtifactResponseEditorProvider(ArtifactRegistry registry, ArtifactLauncher launcher, Logging logging) {
        this.registry = registry;
        this.launcher = launcher;
        this.logging = logging;
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext context) {
        return new ArtifactResponseEditor();
    }

    private final class ArtifactResponseEditor implements ExtensionProvidedHttpResponseEditor {

        private final JPanel panel;
        private final JLabel titleLabel;
        private final JLabel detailLabel;
        private final JButton openButton;
        private final JButton openWithButton;
        private final JButton saveAsButton;

        private HttpRequestResponse currentMessage;
        private ArtifactRegistry.ResolvedArtifact currentArtifact;

        ArtifactResponseEditor() {
            panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

            titleLabel = new JLabel("No artifact detected");
            titleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            detailLabel = new JLabel("Select a response containing a supported artifact.");
            detailLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

            JPanel buttonRow = new JPanel();
            buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));

            openButton = new JButton("Open");
            openButton.setEnabled(false);
            openButton.addActionListener(e -> {
                if (currentArtifact != null) {
                    launch(currentArtifact);
                }
            });

            openWithButton = new JButton("Open With...");
            openWithButton.setEnabled(false);
            openWithButton.addActionListener(e -> {
                if (currentArtifact != null) {
                    openWith(currentArtifact);
                }
            });

            saveAsButton = new JButton("Save As...");
            saveAsButton.setEnabled(false);
            saveAsButton.addActionListener(e -> {
                if (currentArtifact != null) {
                    saveAs(currentArtifact);
                }
            });

            buttonRow.add(openButton);
            buttonRow.add(Box.createHorizontalStrut(8));
            buttonRow.add(openWithButton);
            buttonRow.add(Box.createHorizontalStrut(8));
            buttonRow.add(saveAsButton);
            buttonRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);

            content.add(titleLabel);
            content.add(Box.createVerticalStrut(4));
            content.add(detailLabel);
            content.add(Box.createVerticalStrut(8));
            content.add(buttonRow);

            panel.add(content, BorderLayout.NORTH);
        }

        @Override
        public boolean isEnabledFor(HttpRequestResponse message) {
            return registry.hasArtifactFor(message);
        }

        @Override
        public String caption() {
            return "Artifacts";
        }

        @Override
        public java.awt.Component uiComponent() {
            return panel;
        }

        @Override
        public Selection selectedData() {
            return Selection.selection(ByteArray.byteArray(""));
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public burp.api.montoya.http.message.responses.HttpResponse getResponse() {
            return currentMessage != null ? currentMessage.response() : null;
        }

        @Override
        public void setRequestResponse(HttpRequestResponse message) {
            this.currentMessage = message;
            Optional<ArtifactRegistry.ResolvedArtifact> resolved = registry.resolve(message);
            currentArtifact = resolved.orElse(null);
            updateUi(resolved);
        }

        private void updateUi(Optional<ArtifactRegistry.ResolvedArtifact> resolved) {
            Runnable task = () -> {
                if (resolved.isEmpty()) {
                    titleLabel.setText("No artifact detected");
                    detailLabel.setText("Select a response containing a supported artifact.");
                    openButton.setEnabled(false);
                    openWithButton.setEnabled(false);
                    saveAsButton.setEnabled(false);
                    return;
                }

                ArtifactRegistry.ArtifactRecord record = resolved.get().record();
                titleLabel.setText(record.suggestedFilename());
                detailLabel.setText(buildDetailText(record));
                openButton.setEnabled(true);
                openWithButton.setEnabled(true);
                saveAsButton.setEnabled(true);
            };

            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }

        private String buildDetailText(ArtifactRegistry.ArtifactRecord record) {
            NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
            String size = formatter.format(record.size());
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(record.capturedAt());
            return String.format("%s • %s bytes • captured %s", record.mimeType(), size, timestamp);
        }

        private void launch(ArtifactRegistry.ResolvedArtifact artifact) {
            try {
                launcher.launchWithDefaultApp(artifact.snapshot());
            } catch (Exception ex) {
                logging.logToError("Failed to open artifact: " + ex.getMessage());
            }
        }

        private void openWith(ArtifactRegistry.ResolvedArtifact artifact) {
            try {
                launcher.openWithChooser(artifact.snapshot(), panel);
            } catch (Exception ex) {
                logging.logToError("Failed to open artifact with chosen application: " + ex.getMessage());
            }
        }

        private void saveAs(ArtifactRegistry.ResolvedArtifact artifact) {
            try {
                launcher.saveAs(artifact.snapshot(), panel);
            } catch (Exception ex) {
                logging.logToError("Failed to save artifact: " + ex.getMessage());
            }
        }
    }
}
