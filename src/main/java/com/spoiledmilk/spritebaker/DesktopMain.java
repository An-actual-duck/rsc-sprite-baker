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
import java.util.List;
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

/** Zero-configuration desktop entry point. Advanced project/CLI entry points remain available. */
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
            try {
                shell = new AppShell(DesktopDistribution.discover(args));
            } catch (Exception e) {
                shell = new AppShell(e);
            }
            shell.openInitialView();
        });
    }

    private static void openTransient(Component owner, NpcCatalogEntry entry, DesktopDistribution distribution) {
        try {
            DesktopSession session = DesktopWorkflow.transientSession(distribution.cacheDirectory, distribution.exportDirectory, entry.id);
            openSession(owner, session);
        } catch (Exception e) {
            showError(owner, e);
        }
    }

    /** Retained for the advanced argument-driven selector. Not exposed by the ordinary desktop. */
    static void runWizard(Component owner, boolean create) {
        Window window = ownerWindow(owner);
        DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
        DesktopSession session = create
            ? DesktopProjectDialog.showCreate(window, preferences)
            : DesktopProjectDialog.showOpen(window, preferences);
        if (session != null) openSession(owner, session);
    }

    /** Retained for the advanced project workflow. */
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
        JDialog progress = new JDialog(ownerWindow, "Loading NPC", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("Loading NPC " + session.project.npcId + "…");
        progress.add(bar);
        progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progress.setSize(440, 90);
        progress.setLocationRelativeTo(ownerWindow);

        SwingWorker<AnimationWorkspace,Void> worker = new SwingWorker<>() {
            protected AnimationWorkspace doInBackground() throws Exception {
                AnimationWorkspace workspace = new AnimationWorkspace(session.cacheDirectory, session.project.npcId);
                if (session.transientDesktop) prepareTransientProject(session.project, workspace);
                return workspace;
            }
            protected void done() {
                progress.dispose();
                try {
                    AnimationWorkspace workspace = get();
                    if (session.transientDesktop) session.dirty = false;
                    else {
                        int standing = session.project.standingSequenceId;
                        int walking = session.project.walkingSequenceId;
                        AnimationDiscovery.populateKnown(session.project, workspace);
                        session.dirty = standing != session.project.standingSequenceId || walking != session.project.walkingSequenceId;
                        remember(session);
                    }
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

    private static void prepareTransientProject(SpriteProject project, AnimationWorkspace workspace) throws Exception {
        AnimationDiscovery.populateKnown(project, workspace);
        PoseSelection standing = null;
        PoseSelection left = null;
        PoseSelection right = null;
        if (project.standingSequenceId >= 0) {
            Sequence530 sequence = workspace.cache.loadSequence(project.standingSequenceId);
            standing = new PoseSelection(AnimationTimeline.sample(sequence, 0), "automatic-default");
        }
        if (project.walkingSequenceId >= 0) {
            Sequence530 sequence = workspace.cache.loadSequence(project.walkingSequenceId);
            left = new PoseSelection(AnimationTimeline.sample(sequence, sequence.totalMillis() / 3), "automatic-default");
            right = new PoseSelection(AnimationTimeline.sample(sequence, sequence.totalMillis() * 2 / 3), "automatic-default");
        }
        if (left == null) left = standing;
        if (right == null) right = standing;
        PoseSelection[] movement = {standing, left, right};
        for (int row = 0; row < movement.length; row++) {
            if (movement[row] != null) for (int column = 0; column < 5; column++) project.sheet.suggest(row, column, movement[row]);
        }

        List<CombatCandidate> candidates = AnimationDiscovery.combatCandidates(workspace);
        if (!candidates.isEmpty()) {
            project.combatSequenceId = candidates.get(0).sequenceId;
            Sequence530 combat = workspace.cache.loadSequence(project.combatSequenceId);
            for (int row = 0; row < 3; row++) {
                long time = combat.totalMillis() * row / 3;
                project.sheet.suggest(row, 5, new PoseSelection(AnimationTimeline.sample(combat, time), "automatic-combat-candidate"));
            }
        } else {
            for (int row = 0; row < 3; row++) if (movement[row] != null) project.sheet.suggest(row, 5, movement[row]);
        }
    }

    static void editorClosed(boolean transientDesktop) {
        if (transientDesktop && !exiting && shell != null && shell.startupError == null) shell.browseNpcs();
        else showShellIfNoEditor();
    }

    static void exitApplication() {
        exiting = true;
        for (Window window : Window.getWindows()) window.dispose();
        System.exit(0);
    }

    private static void showShellIfNoEditor() {
        if (exiting || shell == null || hasVisibleEditor()) return;
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
        if (session.transientDesktop) return;
        try {
            DesktopPreferences preferences = DesktopPreferences.load(DesktopPreferences.defaultFile());
            preferences.remember(session);
            preferences.save(DesktopPreferences.defaultFile());
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
        if (save && !path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) path = path.resolveSibling(path.getFileName() + ".json");
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
        private final DesktopDistribution distribution;
        private final Exception startupError;
        private NpcBrowserDialog browser;

        AppShell(DesktopDistribution distribution) {
            this(distribution, null);
        }

        AppShell(Exception error) {
            this(null, error);
        }

        private AppShell(DesktopDistribution distribution, Exception startupError) {
            super("RSC Sprite Baker");
            this.distribution = distribution;
            this.startupError = startupError;
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) { exitApplication(); }
            });
            setJMenuBar(menuBar());
            setLayout(new BorderLayout());
            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setBorder(BorderFactory.createEmptyBorder(38, 48, 38, 48));
            JLabel title = new JLabel("RSC Sprite Baker", JLabel.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
            content.add(title, BorderLayout.NORTH);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 32));
            JButton browse = new JButton(startupError == null ? "Browse NPCs" : "Cache Not Available");
            browse.setPreferredSize(new Dimension(240, 56));
            browse.setEnabled(startupError == null);
            browse.addActionListener(e -> browseNpcs());
            actions.add(browse);
            content.add(actions, BorderLayout.CENTER);
            String message = startupError == null
                ? "Choose an NPC, customize its sprite sheet, and export. No project setup required."
                : "The bundled cache could not be opened. See Help > Startup Details.";
            content.add(new JLabel(message, JLabel.CENTER), BorderLayout.SOUTH);
            add(content, BorderLayout.CENTER);
            setSize(720, 340);
            setMinimumSize(new Dimension(600, 290));
            setLocationByPlatform(true);
        }

        private JMenuBar menuBar() {
            JMenuBar bar = new JMenuBar();
            JMenu npc = new JMenu("NPC");
            JMenuItem browse = item("Browse NPCs…", this::browseNpcs);
            browse.setEnabled(startupError == null);
            npc.add(browse);
            bar.add(npc);
            JMenu help = new JMenu("Help");
            help.add(item("About", this::about));
            if (startupError != null) help.add(item("Startup Details", () -> showError(this, startupError)));
            bar.add(help);
            return bar;
        }

        private void browseNpcs() {
            if (distribution == null) return;
            if (browser != null && browser.isDisplayable()) {
                setVisible(false);
                browser.setVisible(true);
                browser.toFront();
                return;
            }
            browser = new NpcBrowserDialog(this, distribution.cacheDirectory, entry -> openTransient(this, entry, distribution));
            browser.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosed(java.awt.event.WindowEvent e) { showShellIfNoEditor(); }
            });
            setVisible(false);
            browser.setVisible(true);
        }

        private void about() {
            String paths = distribution == null ? "" : "\n\nExports: " + distribution.exportDirectory;
            JOptionPane.showMessageDialog(this,
                "RSC Sprite Baker\n\nBrowse an NPC, customize the sheet, then export PNG + provenance." + paths +
                "\n\nBundled cache licensing and source details are included in the distribution's licenses folder.",
                "About RSC Sprite Baker", JOptionPane.INFORMATION_MESSAGE);
        }

        private JMenuItem item(String label, Runnable action) {
            JMenuItem item = new JMenuItem(label);
            item.addActionListener(e -> action.run());
            return item;
        }

        private void openInitialView() {
            if (startupError != null) {
                setVisible(true);
                return;
            }
            browseNpcs();
        }
    }
}
