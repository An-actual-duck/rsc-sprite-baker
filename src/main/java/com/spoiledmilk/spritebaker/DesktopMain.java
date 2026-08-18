package com.spoiledmilk.spritebaker;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Zero-configuration desktop entry point. Advanced project/CLI entry points remain available. */
public final class DesktopMain {
    private static DesktopController desktop;
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
                desktop = new DesktopController(DesktopDistribution.discover(args));
                desktop.openBrowser();
            } catch (Exception e) {
                showError(null, e);
                System.exit(2);
            }
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
                    if (ownerWindow instanceof SelectorMain.SelectorFrame) ((SelectorMain.SelectorFrame)ownerWindow).disposeForReplacement();
                } catch (Exception e) {
                    showError(ownerWindow, e);
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
            standing = AutomaticPoseSuggestions.standing(sequence);standing.source="automatic-default";
        }
        if (project.walkingSequenceId >= 0) {
            Sequence530 sequence = workspace.cache.loadSequence(project.walkingSequenceId);
            left = AutomaticPoseSuggestions.leftStep(sequence);left.source="automatic-default";
            right = AutomaticPoseSuggestions.rightStep(sequence);right.source="automatic-default";
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
            PoseSelection[] suggestions=candidates.get(0).suggestions();
            for (int row = 0; row < 3; row++) {suggestions[row].source="automatic-combat-candidate";project.sheet.suggest(row,5,suggestions[row]);}
        } else {
            for (int row = 0; row < 3; row++) if (movement[row] != null) project.sheet.suggest(row, 5, movement[row]);
        }
    }

    static void editorClosed(boolean transientDesktop) {
        if (transientDesktop && !exiting && desktop != null) desktop.openBrowser();
    }

    static void exitApplication() {
        if (exiting) return;
        exiting = true;
        for (Window window : Window.getWindows()) window.dispose();
        System.exit(0);
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
        return owner == null ? null : SwingUtilities.getWindowAncestor(owner);
    }

    private static final class DesktopController {
        private final DesktopDistribution distribution;
        private NpcBrowserDialog browser;

        DesktopController(DesktopDistribution distribution) {
            this.distribution = distribution;
        }

        private void openBrowser() {
            if (browser != null && browser.isDisplayable()) {
                browser.setVisible(true);
                browser.toFront();
                return;
            }
            NpcBrowserDialog opened = new NpcBrowserDialog(null, distribution.cacheDirectory,
                entry -> openTransient(browser, entry, distribution));
            browser = opened;
            opened.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosed(java.awt.event.WindowEvent e) {
                    if (exiting || hasVisibleEditor()) return;
                    if (opened.selectedNpc()) SwingUtilities.invokeLater(DesktopController.this::openBrowser);
                    else exitApplication();
                }
            });
            opened.setVisible(true);
        }
    }
}
