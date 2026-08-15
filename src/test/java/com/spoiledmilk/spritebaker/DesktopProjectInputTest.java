package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopProjectInputTest {
    @Test
    void constructsExactJsonFilenameWithoutPrecreatingIt(@TempDir Path directory) {
        Path project = DesktopProjectInput.buildProjectFile(directory, "black dragon");
        assertEquals(directory.resolve("black dragon.json").toAbsolutePath(), project);
        assertFalse(Files.exists(project));
        assertEquals(directory.resolve("ready.JSON").toAbsolutePath(), DesktopProjectInput.buildProjectFile(directory, "ready.JSON"));
    }

    @Test
    void rejectsPathsAndNonPortableProjectFilenames(@TempDir Path directory) {
        assertThrows(IllegalArgumentException.class, () -> DesktopProjectInput.buildProjectFile(directory, "folder/project"));
        assertThrows(IllegalArgumentException.class, () -> DesktopProjectInput.buildProjectFile(directory, "bad:name"));
        assertThrows(IllegalArgumentException.class, () -> DesktopProjectInput.buildProjectFile(directory, ".json"));
    }

    @Test
    void validatesAllCreateFieldsAndReportsCacheIdentityClearly(@TempDir Path directory) throws Exception {
        Path cache = directory.resolve("cache");
        Files.createDirectories(cache);
        DesktopProjectInput input = new DesktopProjectInput(DesktopProjectInput.Mode.CREATE);
        input.cacheDirectory = cache.toString();
        input.projectDirectory = directory.resolve("projects").toString();
        input.projectName = "npc-72";
        input.exportDirectory = directory.resolve("exports").toString();
        input.npcId = "72";

        DesktopProjectInput.Validation invalid = input.validate();
        assertFalse(invalid.valid());
        assertTrue(invalid.errors.stream().anyMatch(error -> error.contains("main_file_cache.dat2") && error.contains("main_file_cache.idx255")));

        Files.createFile(cache.resolve("main_file_cache.dat2"));
        Files.createFile(cache.resolve("main_file_cache.idx255"));
        DesktopProjectInput.Validation valid = input.validate();
        assertTrue(valid.valid(), valid.errors.toString());
        assertEquals(directory.resolve("projects/npc-72.json").toAbsolutePath(), valid.projectFile);
        assertEquals(72, valid.npcId);
        assertFalse(Files.exists(valid.projectFile));
    }

    @Test
    void openModeNeverTreatsADirectoryAsAProjectFile(@TempDir Path directory) throws Exception {
        Path cache = directory.resolve("cache");
        Files.createDirectories(cache);
        Files.createFile(cache.resolve("main_file_cache.dat2"));
        Files.createFile(cache.resolve("main_file_cache.idx255"));
        DesktopProjectInput input = new DesktopProjectInput(DesktopProjectInput.Mode.OPEN);
        input.cacheDirectory = cache.toString();
        input.projectFile = directory.toString();
        input.exportDirectory = directory.resolve("exports").toString();
        DesktopProjectInput.Validation validation = input.validate();
        assertFalse(validation.valid());
        assertTrue(validation.errors.contains("Project file must be an existing JSON file."));
    }
}
