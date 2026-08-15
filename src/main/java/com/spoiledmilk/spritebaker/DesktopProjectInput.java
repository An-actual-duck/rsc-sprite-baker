package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Swing-independent state and validation for the desktop New/Open forms. */
final class DesktopProjectInput {
    enum Mode { CREATE, OPEN }

    final Mode mode;
    String cacheDirectory = "";
    String projectDirectory = "";
    String projectName = "";
    String projectFile = "";
    String exportDirectory = "";
    String npcId = "72";

    DesktopProjectInput(Mode mode) {
        this.mode = mode;
    }

    Validation validate() {
        List<String> errors = new ArrayList<>();
        Path cache = parseRequired(cacheDirectory, "Cache directory", errors);
        Path export = parseRequired(exportDirectory, "Export directory", errors);
        Path project = null;
        int npc = -1;

        if (cache != null) {
            try {
                cache = DesktopWorkflow.validateCache(cache);
            } catch (IOException e) {
                errors.add("Cache directory must directly contain main_file_cache.dat2 and main_file_cache.idx255.");
            }
        }

        if (mode == Mode.CREATE) {
            Path directory = parseRequired(projectDirectory, "Project location", errors);
            if (directory != null) {
                try {
                    DesktopWorkflow.validateCreatableDirectory(directory, "Project location");
                } catch (IOException e) {
                    errors.add(e.getMessage());
                }
            }
            try {
                project = directory == null ? null : buildProjectFile(directory, projectName);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            if (project != null && Files.isDirectory(project)) {
                errors.add("Project filename resolves to a directory, not a JSON file.");
            }
            try {
                npc = Integer.parseInt(npcId.trim());
                if (npc < 0) errors.add("Initial NPC ID must be zero or greater.");
            } catch (RuntimeException e) {
                errors.add("Initial NPC ID must be a whole number.");
            }
        } else {
            project = parseRequired(projectFile, "Project file", errors);
            if (project != null && !Files.isRegularFile(project)) {
                errors.add("Project file must be an existing JSON file.");
            }
        }

        if (export != null) {
            try {
                DesktopWorkflow.validateCreatableDirectory(export, "Export directory");
            } catch (IOException e) {
                errors.add(e.getMessage());
            }
        }

        return new Validation(cache, project, export, npc, errors);
    }

    static Path buildProjectFile(Path directory, String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Project filename is required.");
        if (name.equals(".") || name.equals("..") || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Project filename must be a filename, not a path.");
        }
        if (name.matches(".*[<>:\"|?*].*") || name.endsWith(".") || name.endsWith(" ")) {
            throw new IllegalArgumentException("Project filename contains characters that are not portable across supported systems.");
        }
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) name += ".json";
        if (name.equalsIgnoreCase(".json")) throw new IllegalArgumentException("Project filename needs a name before .json.");
        try {
            return directory.toAbsolutePath().normalize().resolve(name).normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Project filename contains invalid characters.");
        }
    }

    private static Path parseRequired(String value, String label, List<String> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(label + " is required.");
            return null;
        }
        try {
            return Path.of(value.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            errors.add(label + " is not a valid path.");
            return null;
        }
    }

    static final class Validation {
        final Path cacheDirectory;
        final Path projectFile;
        final Path exportDirectory;
        final int npcId;
        final List<String> errors;

        Validation(Path cache, Path project, Path export, int npc, List<String> errors) {
            cacheDirectory = cache;
            projectFile = project;
            exportDirectory = export;
            npcId = npc;
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        }

        boolean valid() {
            return errors.isEmpty();
        }
    }
}
