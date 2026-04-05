package ai.opencodex.burp.fileextractor;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

final class ArtifactLauncher {

    void launchWithDefaultApp(ArtifactSnapshot snapshot) throws IOException {
        ensureDesktopSupport();
        Path tempFile = materialize(snapshot);
        Desktop.getDesktop().open(tempFile.toFile());
    }

    void openWithChooser(ArtifactSnapshot snapshot, java.awt.Component parent) throws IOException {
        Path tempFile = materialize(snapshot);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select application to open artifact");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = parent == null ? chooser.showOpenDialog(null) : chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File application = chooser.getSelectedFile();
        launchWithApplication(application.toPath(), tempFile);
    }

    void saveAs(ArtifactSnapshot snapshot, java.awt.Component parent) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save artifact as");
        chooser.setSelectedFile(new File(snapshot.suggestedFilename()));
        String extension = extractExtension(snapshot.suggestedFilename());
        if (!extension.isEmpty()) {
            chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(Locale.ROOT) + " Files", extension));
        }
        int result = parent == null ? chooser.showSaveDialog(null) : chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Files.write(chooser.getSelectedFile().toPath(), snapshot.bytes());
    }

    private Path materialize(ArtifactSnapshot snapshot) throws IOException {
        Path tempFile = Files.createTempFile("burp-artifact-", deriveExtension(snapshot));
        Files.write(tempFile, snapshot.bytes());
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    private String deriveExtension(ArtifactSnapshot snapshot) {
        String filename = snapshot.suggestedFilename();
        int dot = filename.lastIndexOf('.');
        if (dot > -1 && dot < filename.length() - 1) {
            return filename.substring(dot);
        }
        if (snapshot.mimeType().contains("pdf")) {
            return ".pdf";
        }
        return ".bin";
    }

    private void ensureDesktopSupport() {
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Desktop integration is not supported on this platform");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            throw new IllegalStateException("OPEN action is not supported on this platform");
        }
    }

    private void launchWithApplication(Path application, Path artifact) throws IOException {
        String appPath = application.toAbsolutePath().toString();
        if (isMac()) {
            if (appPath.toLowerCase(Locale.ROOT).endsWith(".app")) {
                new ProcessBuilder("open", "-a", appPath, artifact.toString()).start();
            } else {
                new ProcessBuilder("open", "-a", appPath, artifact.toString()).start();
            }
            return;
        }

        new ProcessBuilder(appPath, artifact.toString()).start();
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > -1 && dot < filename.length() - 1) {
            return filename.substring(dot + 1);
        }
        return "";
    }
}
