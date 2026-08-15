package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Runnable desktop entry point. CLI entry points remain Main and SelectorMain. */
public final class DesktopMain {
    private static AppShell shell;
    private static boolean exiting;

    private DesktopMain(){}

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("RSC Sprite Baker desktop requires a graphical environment. CLI entry points remain available.");
            System.exit(2);
        }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            shell = new AppShell();
            shell.setVisible(true);
        });
    }

    static void runWizard(Component owner, boolean create) {
        Window window = ownerWindow(owner);
        DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
        DesktopSession session = create
            ? DesktopProjectDialog.showCreate(window, preferences)
            : DesktopProjectDialog.showOpen(window, preferences);
        if (session != null) openSession(owner, session);
    }

    static void openRecent(Component owner, DesktopPreferences.RecentProject recent) {
        try {
            DesktopSession session = DesktopWorkflow.open(Path.of(recent.cacheDirectory), Path.of(recent.projectFile), Path.of(recent.exportDirectory));
            openSession(owner, session);
        } catch (Exception e) {
            showError(owner, e);
        }
    }

    static void openSession(Component owner, DesktopSession session) {
        Window ownerWindow = ownerWindow(owner);
        JDialog progress = new JDialog(ownerWindow, "Opening project", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("Loading cache and NPC " + session.project.npcId + "…");
        progress.add(bar);
        progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progress.setSize(440, 90);
        progress.setLocationRelativeTo(ownerWindow);

        SwingWorker<AnimationWorkspace,Void> worker = new SwingWorker<>() {
            protected AnimationWorkspace doInBackground() throws Exception {
                return new AnimationWorkspace(session.cacheDirectory, session.project.npcId);
            }
            protected void done() {
                progress.dispose();
                try {
                    AnimationWorkspace workspace = get();
                    int standing = session.project.standingSequenceId;
                    int walking = session.project.walkingSequenceId;
                    AnimationDiscovery.populateKnown(session.project, workspace);
                    session.dirty = standing != session.project.standingSequenceId || walking != session.project.walkingSequenceId;
                    remember(session);
                    SelectorMain.SelectorFrame frame = new SelectorMain.SelectorFrame(workspace, session);
                    frame.setVisible(true);
                    if (shell != null) shell.setVisible(false);
                    if (ownerWindow instanceof SelectorMain.SelectorFrame) ownerWindow.dispose();
                } catch (Exception e) {
                    showError(ownerWindow, e);
                    showShellIfNoEditor();
                }
            }
        };
        worker.execute();
        progress.setVisible(true);
    }

    static void editorClosed() {
        showShellIfNoEditor();
    }

    static void exitApplication() {
        exiting = true;
        for (Window window : Window.getWindows()) window.dispose();
        System.exit(0);
    }

    private static void showShellIfNoEditor() {
        if (exiting || shell == null || hasVisibleEditor()) return;
        shell.refresh();
        shell.setVisible(true);
        shell.setExtendedState(JFrame.NORMAL);
        shell.toFront();
    }

    private static boolean hasVisibleEditor() {
        for (Window window : Window.getWindows()) {
            if (window instanceof SelectorMain.SelectorFrame && window.isVisible()) return true;
        }
        return false;
    }

    static void remember(DesktopSession session) {
        try {
            DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
            preferences.remember(session);
            preferences.save(DesktopPreferences.defaultFile());
            if (shell != null) shell.refresh();
        } catch (Exception ignored) {}
    }

    static Path chooseDirectory(Component owner, String title, Path initial) {
        Path start = initial;
        if (start != null && !Files.isDirectory(start)) start = start.getParent();
        JFileChooser chooser = new JFileChooser(start == null ? null : start.toFile());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText("Use Selected Folder");
        return chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    static Path chooseProject(Component owner, boolean save) {
        DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
        Path initial = preferences.lastProjectDirectory();
        JFileChooser chooser = new JFileChooser(initial == null ? null : initial.toFile());
        chooser.setDialogTitle(save ? "Save Sprite Baker project as" : "Open Sprite Baker project");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Sprite Baker project (*.json)", "json"));
        int choice = save ? chooser.showSaveDialog(owner) : chooser.showOpenDialog(owner);
        if (choice != JFileChooser.APPROVE_OPTION) return null;
        Path path = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (save && !path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            path = path.resolveSibling(path.getFileName() + ".json");
        }
        if (Files.isDirectory(path)) {
            JOptionPane.showMessageDialog(owner, "Select a project filename, not a directory.", "RSC Sprite Baker", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (save && Files.exists(path) && JOptionPane.showConfirmDialog(owner,
            "The exact project file already exists:\n" + path + "\n\nReplace it?",
            "Replace Existing Project?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return null;
        return path;
    }

    static void showError(Component owner, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        JOptionPane.showMessageDialog(owner, cause.toString(), "RSC Sprite Baker", JOptionPane.ERROR_MESSAGE);
    }

    private static Window ownerWindow(Component owner) {
        if (owner instanceof Window) return (Window)owner;
        Window found = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        return found == null ? shell : found;
    }

    static final class AppShell extends JFrame {
        private final JMenu recent = new JMenu("Open Recent");

        AppShell() {
            super("RSC Sprite Baker");
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { exitApplication(); }
            });
            setJMenuBar(menuBar());
            setLayout(new BorderLayout());
            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
            JLabel title = new JLabel("RSC Sprite Baker", JLabel.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
            content.add(title, BorderLayout.NORTH);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 28));
            JButton create = new JButton("Create New Project");
            create.setPreferredSize(new Dimension(210, 52));
            create.addActionListener(e -> runWizard(this, true));
            JButton open = new JButton("Open Existing Project");
            open.setPreferredSize(new Dimension(210, 52));
            open.addActionListener(e -> runWizard(this, false));
            actions.add(create);
            actions.add(open);
            content.add(actions, BorderLayout.CENTER);
            JLabel note = new JLabel("Projects stay portable; cache and export locations remain local to this computer.", JLabel.CENTER);
            content.add(note, BorderLayout.SOUTH);
            add(content, BorderLayout.CENTER);
            setSize(680, 330);
            setMinimumSize(new Dimension(560, 280));
            setLocationByPlatform(true);
            refresh();
        }

        private JMenuBar menuBar() {
            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("File");
            file.add(item("New Project…", () -> runWizard(this, true)));
            file.add(item("Open Project…", () -> runWizard(this, false)));
            file.add(recent);
            file.addSeparator();
            file.add(item("Exit", DesktopMain::exitApplication));
            bar.add(file);
            return bar;
        }

        void refresh() {
            recent.removeAll();
            DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
            recent.setEnabled(!preferences.recentProjects.isEmpty());
            for (DesktopPreferences.RecentProject entry : preferences.recentProjects) {
                recent.add(item(entry.toString(), () -> openRecent(this, entry)));
            }
        }

        private JMenuItem item(String label, Runnable action) {
            JMenuItem item = new JMenuItem(label);
            item.addActionListener(e -> action.run());
            return item;
        }
    }
}
