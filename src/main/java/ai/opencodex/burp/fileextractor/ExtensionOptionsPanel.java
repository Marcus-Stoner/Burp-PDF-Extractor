package ai.opencodex.burp.fileextractor;

import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.settings.SettingsPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

final class ExtensionOptionsPanel implements SettingsPanel {

    private final ExtensionConfig config;
    private final ArtifactCache cache;
    private final Logging logging;
    private final JPanel panel;
    private final JCheckBox autoOpenCheckbox;
    private final JCheckBox highlightCheckbox;
    private final JTextField cacheLimitField;
    private final JTextField mimeListField;
    private final JComboBox<HighlightColor> highlightColorCombo;

    ExtensionOptionsPanel(ExtensionConfig config, ArtifactCache cache, Logging logging) {
        this.config = config;
        this.cache = cache;
        this.logging = logging;
        this.panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        autoOpenCheckbox = new JCheckBox("Automatically open detected artifacts");
        autoOpenCheckbox.setSelected(config.autoOpenEnabled());
        form.add(autoOpenCheckbox, gbc);

        gbc.gridy++;
        highlightCheckbox = new JCheckBox("Highlight supported artifacts in Proxy/Repeater");
        highlightCheckbox.setSelected(config.highlightEnabled());
        form.add(highlightCheckbox, gbc);

        gbc.gridy++;
        form.add(new JLabel("Highlight color:"), gbc);

        HighlightColor[] colors = Arrays.stream(HighlightColor.values())
                .filter(color -> color != HighlightColor.NONE)
                .toArray(HighlightColor[]::new);
        highlightColorCombo = new JComboBox<>(new DefaultComboBoxModel<>(colors));
        highlightColorCombo.setSelectedItem(config.pdfHighlightColor());
        highlightColorCombo.setEnabled(config.highlightEnabled());
        highlightColorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof HighlightColor color) {
                    setText(formatHighlightColor(color));
                }
                return component;
            }
        });
        highlightCheckbox.addActionListener(e -> highlightColorCombo.setEnabled(highlightCheckbox.isSelected()));
        gbc.gridx = 1;
        form.add(highlightColorCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Cache limit (MB):"), gbc);

        cacheLimitField = new JTextField(String.valueOf(config.cacheLimitBytes() / (1024 * 1024L)), 10);
        gbc.gridx = 1;
        form.add(cacheLimitField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Allowed MIME types (comma separated):"), gbc);

        mimeListField = new JTextField(String.join(", ", config.allowedMimeTypes()));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(mimeListField, gbc);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applyChanges());

        panel.add(form, BorderLayout.NORTH);
        panel.add(applyButton, BorderLayout.SOUTH);
    }

    @Override
    public JComponent uiComponent() {
        return panel;
    }

    private void applyChanges() {
        try {
            boolean autoOpen = autoOpenCheckbox.isSelected();

            long cacheLimitMb = Long.parseLong(cacheLimitField.getText().trim());
            long cacheLimitBytes = cacheLimitMb * 1024L * 1024L;

            List<String> mimeTypes = Arrays.stream(mimeListField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            config.setAutoOpenEnabled(autoOpen);
            config.setCacheLimitBytes(cacheLimitBytes);
            config.setAllowedMimeTypes(mimeTypes);
            config.setHighlightEnabled(highlightCheckbox.isSelected());
            HighlightColor selectedColor = (HighlightColor) highlightColorCombo.getSelectedItem();
            if (selectedColor != null) {
                config.setPdfHighlightColor(selectedColor);
            }
            cache.setCapacity(cacheLimitBytes);

            logging.logToOutput("Burp File Extractor settings updated");
        } catch (NumberFormatException ex) {
            logging.logToError("Invalid cache limit. Please enter a numeric value in megabytes.");
        } catch (IllegalArgumentException ex) {
            logging.logToError(ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            cacheLimitField.setText(String.valueOf(config.cacheLimitBytes() / (1024 * 1024L)));
            mimeListField.setText(String.join(", ", config.allowedMimeTypes()));
            autoOpenCheckbox.setSelected(config.autoOpenEnabled());
            highlightCheckbox.setSelected(config.highlightEnabled());
            highlightColorCombo.setSelectedItem(config.pdfHighlightColor());
            highlightColorCombo.setEnabled(config.highlightEnabled());
        });
    }

    private String formatHighlightColor(HighlightColor color) {
        return switch (color) {
            case CYAN -> "Cyan";
            case GREEN -> "Green";
            case MAGENTA -> "Magenta";
            case PINK -> "Pink";
            case RED -> "Red";
            case YELLOW -> "Yellow";
            case ORANGE -> "Orange";
            case BLUE -> "Blue";
            case NONE -> "None";
            default -> capitalize(color.name());
        };
    }

    private String capitalize(String input) {
        String lower = input.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
