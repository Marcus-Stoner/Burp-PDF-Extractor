package ai.opencodex.burp.fileextractor;

import burp.api.montoya.logging.Logging;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

final class ArtifactSuiteTab {

    private final ArtifactRegistry registry;
    private final ArtifactLauncher launcher;
    private final Logging logging;

    private final JPanel panel;
    private final ArtifactTableModel tableModel;
    private final JTable table;

    ArtifactSuiteTab(ArtifactRegistry registry, ArtifactLauncher launcher, Logging logging) {
        this.registry = registry;
        this.launcher = launcher;
        this.logging = logging;

        this.panel = new JPanel(new BorderLayout(10, 10));
        this.tableModel = new ArtifactTableModel();
        this.table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(900, 300));

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Detected PDF Artifacts"), BorderLayout.WEST);

        JPanel buttons = new JPanel();
        JButton refreshButton = new JButton(new AbstractAction("Refresh") {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        JButton openSelectedButton = new JButton(new AbstractAction("Open Selected") {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelected();
            }
        });

        JButton saveSelectedButton = new JButton(new AbstractAction("Save Selected as ZIP") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSelected();
            }
        });

        buttons.add(refreshButton);
        buttons.add(openSelectedButton);
        buttons.add(saveSelectedButton);
        header.add(buttons, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        registry.addListener(record -> SwingUtilities.invokeLater(this::refresh));
        refresh();
    }

    JComponent component() {
        return panel;
    }

    private void refresh() {
        tableModel.setRows(registry.allRecords());
    }

    private void openSelected() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(panel, "Select at least one artifact to open.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (int viewRow : selectedRows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            ArtifactRegistry.ArtifactRecord record = tableModel.getRecord(modelRow);
            registry.resolve(record.digest()).ifPresent(resolved -> {
                try {
                    launcher.launchWithDefaultApp(resolved.snapshot());
                } catch (IOException ex) {
                    logging.logToError("Failed to open artifact: " + ex.getMessage());
                }
            });
        }
    }

    private void saveSelected() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(panel, "Select at least one artifact to save.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save selected artifacts as Zip");
        chooser.setSelectedFile(new File("artifacts.zip"));
        int result = chooser.showSaveDialog(panel);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        if (target.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(panel, "File exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        Map<String, Integer> nameCounts = new HashMap<>();

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target))) {
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                ArtifactRegistry.ArtifactRecord record = tableModel.getRecord(modelRow);
                Optional<ArtifactRegistry.ResolvedArtifact> resolved = registry.resolve(record.digest());
                if (resolved.isEmpty()) {
                    continue;
                }

                ArtifactSnapshot snapshot = resolved.get().snapshot();
                String entryName = uniqueName(record.suggestedFilename(), nameCounts);
                ZipEntry entry = new ZipEntry(entryName);
                zip.putNextEntry(entry);
                zip.write(snapshot.bytes());
                zip.closeEntry();
            }
        } catch (IOException ex) {
            logging.logToError("Failed to save ZIP: " + ex.getMessage());
            JOptionPane.showMessageDialog(panel, "Failed to save ZIP: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logging.logToOutput("Saved selected artifacts to " + target.getAbsolutePath());
    }

    private String uniqueName(String baseName, Map<String, Integer> counts) {
        String sanitized = sanitizeFilename(baseName);
        int count = counts.getOrDefault(sanitized, 0);
        counts.put(sanitized, count + 1);
        if (count == 0) {
            return sanitized;
        }
        int dot = sanitized.lastIndexOf('.');
        if (dot > 0) {
            return sanitized.substring(0, dot) + "(" + count + ")" + sanitized.substring(dot);
        }
        return sanitized + "(" + count + ")";
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "artifact.pdf";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static final class ArtifactTableModel extends AbstractTableModel {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        private final List<ArtifactRegistry.ArtifactRecord> records = new ArrayList<>();

        void setRows(List<ArtifactRegistry.ArtifactRecord> newRecords) {
            records.clear();
            records.addAll(newRecords);
            fireTableDataChanged();
        }

        ArtifactRegistry.ArtifactRecord getRecord(int rowIndex) {
            return records.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Filename";
                case 1 -> "MIME";
                case 2 -> "Size";
                case 3 -> "URL";
                case 4 -> "Captured";
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ArtifactRegistry.ArtifactRecord record = records.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> record.suggestedFilename();
                case 1 -> record.mimeType();
                case 2 -> humanReadableSize(record.size());
                case 3 -> record.url();
                case 4 -> FORMATTER.format(record.capturedAt());
                default -> "";
            };
        }

        private String humanReadableSize(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            }
            double kb = bytes / 1024.0;
            if (kb < 1024) {
                return String.format(Locale.getDefault(), "%.1f KB", kb);
            }
            double mb = kb / 1024.0;
            return String.format(Locale.getDefault(), "%.2f MB", mb);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
