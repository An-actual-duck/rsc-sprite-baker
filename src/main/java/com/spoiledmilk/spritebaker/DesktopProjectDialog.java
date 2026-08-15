package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/** One owned, consolidated New/Open project workflow. */
final class DesktopProjectDialog extends JDialog {
    private final DesktopProjectInput input;
    private final JTextField cache = new JTextField();
    private final JTextField projectDirectory = new JTextField();
    private final JTextField projectName = new JTextField("sprite-project");
    private final JTextField projectFile = new JTextField();
    private final JTextField export = new JTextField();
    private final JTextField npc = new JTextField("72");
    private final JLabel resultingFile = new JLabel(" ");
    private final JTextArea errors = new JTextArea(4, 60);
    private DesktopSession result;

    private DesktopProjectDialog(Window owner, DesktopProjectInput.Mode mode, DesktopPreferences preferences) {
        super(owner, mode == DesktopProjectInput.Mode.CREATE ? "Create New Project" : "Open Existing Project", ModalityType.APPLICATION_MODAL);
        input = new DesktopProjectInput(mode);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        String explanation = mode == DesktopProjectInput.Mode.CREATE
            ? "Create a portable project. Cache and export locations are remembered only on this computer."
            : "Open a portable project and choose this computer's cache and export locations.";
        add(new JLabel(explanation), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;

        cache.setText(text(preferences.lastCache()));
        addPathRow(form, c, row++, "Cache directory", cache, () -> browseDirectory(cache, "Select the folder containing main_file_cache.dat2"));
        addHint(form, c, row++, "Must directly contain main_file_cache.dat2 and main_file_cache.idx255.");

        if (mode == DesktopProjectInput.Mode.CREATE) {
            Path initialProject = preferences.lastProjectDirectory();
            projectDirectory.setText(text(initialProject));
            addPathRow(form, c, row++, "Project location", projectDirectory, () -> browseDirectory(projectDirectory, "Select the folder for the project"));
            addFieldRow(form, c, row++, "Project filename", projectName);
            addHint(form, c, row++, "A .json extension is added automatically. You do not need to create the file first.");
            c.gridx = 1; c.gridy = row++; c.gridwidth = 2;
            resultingFile.setForeground(new Color(70, 70, 70));
            form.add(resultingFile, c);
            c.gridwidth = 1;
        } else {
            addPathRow(form, c, row++, "Project file", projectFile, this::browseProjectFile);
            addHint(form, c, row++, "Select an existing Sprite Baker .json project file.");
        }

        Path initialExport = preferences.lastExport();
        export.setText(text(initialExport));
        addPathRow(form, c, row++, "Export directory", export, () -> browseDirectory(export, "Select the folder for exported sprites"));
        if (mode == DesktopProjectInput.Mode.CREATE) addFieldRow(form, c, row++, "Initial NPC ID", npc);

        c.gridx = 0; c.gridy = row++; c.gridwidth = 3; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        errors.setEditable(false);
        errors.setLineWrap(true);
        errors.setWrapStyleWord(true);
        errors.setForeground(new Color(165, 25, 25));
        errors.setBackground(form.getBackground());
        errors.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        form.add(new JScrollPane(errors, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), c);
        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton primary = new JButton(mode == DesktopProjectInput.Mode.CREATE ? "Create" : "Open");
        primary.addActionListener(e -> submit());
        actions.add(cancel);
        actions.add(primary);
        add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(primary);

        DocumentListener preview = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateResultingFile(); }
            public void removeUpdate(DocumentEvent e) { updateResultingFile(); }
            public void changedUpdate(DocumentEvent e) { updateResultingFile(); }
        };
        projectDirectory.getDocument().addDocumentListener(preview);
        projectName.getDocument().addDocumentListener(preview);
        updateResultingFile();

        setPreferredSize(new Dimension(820, mode == DesktopProjectInput.Mode.CREATE ? 500 : 420));
        pack();
        setMinimumSize(new Dimension(680, 360));
        setLocationRelativeTo(owner);
    }

    static DesktopSession showCreate(Window owner, DesktopPreferences preferences) {
        DesktopProjectDialog dialog = new DesktopProjectDialog(owner, DesktopProjectInput.Mode.CREATE, preferences);
        dialog.setVisible(true);
        return dialog.result;
    }

    static DesktopSession showOpen(Window owner, DesktopPreferences preferences) {
        DesktopProjectDialog dialog = new DesktopProjectDialog(owner, DesktopProjectInput.Mode.OPEN, preferences);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void submit() {
        copyFieldsToInput();
        DesktopProjectInput.Validation validation = input.validate();
        if (!validation.valid()) {
            showErrors(validation.errors);
            return;
        }
        if (input.mode == DesktopProjectInput.Mode.CREATE && Files.exists(validation.projectFile)) {
            int choice = JOptionPane.showConfirmDialog(this,
                "The exact project file already exists:\n" + validation.projectFile + "\n\nReplace it?",
                "Replace Existing Project?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        try {
            result = input.mode == DesktopProjectInput.Mode.CREATE
                ? DesktopWorkflow.create(validation.cacheDirectory, validation.projectFile, validation.exportDirectory, validation.npcId)
                : DesktopWorkflow.open(validation.cacheDirectory, validation.projectFile, validation.exportDirectory);
            dispose();
        } catch (Exception e) {
            showErrors(java.util.List.of(rootMessage(e)));
        }
    }

    private void copyFieldsToInput() {
        input.cacheDirectory = cache.getText();
        input.projectDirectory = projectDirectory.getText();
        input.projectName = projectName.getText();
        input.projectFile = projectFile.getText();
        input.exportDirectory = export.getText();
        input.npcId = npc.getText();
    }

    private void showErrors(java.util.List<String> messages) {
        StringBuilder text = new StringBuilder();
        for (String message : messages) text.append("• ").append(message).append('\n');
        errors.setText(text.toString());
        errors.setCaretPosition(0);
    }

    private void updateResultingFile() {
        try {
            if (projectDirectory.getText().isBlank() || projectName.getText().isBlank()) {
                resultingFile.setText("Resulting project file will appear here.");
            } else {
                Path path = DesktopProjectInput.buildProjectFile(Path.of(projectDirectory.getText().trim()), projectName.getText());
                resultingFile.setText("Resulting file: " + path);
            }
        } catch (RuntimeException e) {
            resultingFile.setText("Resulting file: invalid project location or filename");
        }
    }

    private void browseDirectory(JTextField field, String title) {
        Path initial = safePath(field.getText());
        Path selected = DesktopMain.chooseDirectory(this, title, initial);
        if (selected != null) field.setText(selected.toAbsolutePath().normalize().toString());
    }

    private void browseProjectFile() {
        Path initial = safePath(projectFile.getText());
        if (initial == null) initial = safePath(projectDirectory.getText());
        JFileChooser chooser = new JFileChooser(initial == null ? null : (Files.isDirectory(initial) ? initial.toFile() : initial.getParent().toFile()));
        chooser.setDialogTitle("Open Sprite Baker project");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Sprite Baker project (*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) projectFile.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
    }

    private static void addPathRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field, Runnable browse) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(field, c);
        JButton button = new JButton("Browse…");
        button.addActionListener(e -> browse.run());
        c.gridx = 2; c.weightx = 0;
        panel.add(button, c);
    }

    private static void addFieldRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1;
        panel.add(field, c);
        c.gridwidth = 1;
    }

    private static void addHint(JPanel panel, GridBagConstraints c, int row, String message) {
        JLabel hint = new JLabel(message);
        hint.setForeground(new Color(80, 80, 80));
        c.gridx = 1; c.gridy = row; c.gridwidth = 2; c.weightx = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(hint, c);
        c.gridwidth = 1;
    }

    private static Path safePath(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Path.of(value.trim()).toAbsolutePath().normalize(); }
        catch (RuntimeException ignored) { return null; }
    }

    private static String text(Path path) {
        return path == null ? "" : path.toString();
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }
}
