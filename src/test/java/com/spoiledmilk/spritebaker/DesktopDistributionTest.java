package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopDistributionTest {
    @Test
    void discoversBundledCacheAndAdjacentExports(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("RSC Sprite Baker");
        Path cache = cache(root.resolve("cache"));
        Path jar = root.resolve("rsc-sprite-baker.jar");
        Files.createFile(jar);

        DesktopDistribution paths = DesktopDistribution.discover(new String[0], Map.of(), new Properties(), jar, directory);
        assertEquals(root.toAbsolutePath(), paths.applicationRoot);
        assertEquals(cache.toRealPath(), paths.cacheDirectory);
        assertEquals(root.resolve("exports").toAbsolutePath(), paths.exportDirectory);
        assertTrue(Files.isDirectory(paths.exportDirectory));
    }

    @Test
    void explicitCacheOverrideWinsWithoutHardCodedMachinePath(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("app");
        Files.createDirectories(root);
        Path cache = cache(directory.resolve("user-cache"));
        Properties properties = new Properties();
        properties.setProperty(DesktopDistribution.HOME_PROPERTY, root.toString());
        properties.setProperty(DesktopDistribution.CACHE_PROPERTY, directory.resolve("wrong-cache").toString());

        DesktopDistribution paths = DesktopDistribution.discover(new String[]{"--cache", cache.toString()}, Map.of(), properties, root.resolve("classes"), directory);
        assertEquals(cache.toRealPath(), paths.cacheDirectory);
        assertEquals(root.resolve("exports").toAbsolutePath(), paths.exportDirectory);
    }

    @Test
    void rejectsExportsInsideReadOnlyCache(@TempDir Path directory) throws Exception {
        Path cache = cache(directory.resolve("cache"));
        Properties properties = new Properties();
        properties.setProperty(DesktopDistribution.HOME_PROPERTY, directory.toString());
        properties.setProperty(DesktopDistribution.CACHE_PROPERTY, cache.toString());
        properties.setProperty(DesktopDistribution.EXPORT_PROPERTY, cache.resolve("exports").toString());
        assertThrows(java.io.IOException.class, () -> DesktopDistribution.discover(new String[0], Map.of(), properties, directory, directory));
    }

    @Test
    void rejectsExportSymlinkResolvingInsideReadOnlyCache(@TempDir Path directory) throws Exception {
        Path cache = cache(directory.resolve("cache"));
        Path actualExport = Files.createDirectories(cache.resolve("exports"));
        Path exportLink = directory.resolve("exports-link");
        try {
            Files.createSymbolicLink(exportLink, actualExport);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links are unavailable: " + e);
        }
        Properties properties = new Properties();
        properties.setProperty(DesktopDistribution.HOME_PROPERTY, directory.toString());
        properties.setProperty(DesktopDistribution.CACHE_PROPERTY, cache.toString());
        properties.setProperty(DesktopDistribution.EXPORT_PROPERTY, exportLink.toString());
        assertThrows(java.io.IOException.class, () -> DesktopDistribution.discover(new String[0], Map.of(), properties, directory, directory));
    }

    @Test
    void rejectsUnexpectedDesktopArguments(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("app");
        cache(root.resolve("cache"));
        Properties properties = new Properties();
        properties.setProperty(DesktopDistribution.HOME_PROPERTY, root.toString());
        assertThrows(java.io.IOException.class, () -> DesktopDistribution.discover(new String[]{"--project", "anything.json"}, Map.of(), properties, root, directory));
        assertThrows(java.io.IOException.class, () -> DesktopDistribution.discover(new String[]{"--cache"}, Map.of(), properties, root, directory));
    }

    private static Path cache(Path directory) throws Exception {
        Files.createDirectories(directory);
        Files.createFile(directory.resolve("main_file_cache.dat2"));
        Files.createFile(directory.resolve("main_file_cache.idx255"));
        return directory;
    }
}
