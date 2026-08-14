package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopWorkflowTest {
    @Test void createsAndOpensPortableProjectWithMachinePathsKeptSeparate(@TempDir Path dir)throws Exception{
        Path cache=dir.resolve("cache");Files.createDirectories(cache);Files.createFile(cache.resolve("main_file_cache.dat2"));Files.createFile(cache.resolve("main_file_cache.idx255"));Path project=dir.resolve("projects/neutral.json"),export=dir.resolve("exports");
        DesktopSession created=DesktopWorkflow.create(cache,project,export,123);assertEquals(123,created.project.npcId);assertTrue(Files.isDirectory(export));String json=Files.readString(project);assertFalse(json.contains(cache.toString()));assertFalse(json.contains(export.toString()));
        DesktopSession opened=DesktopWorkflow.open(cache,project,export);assertEquals(123,opened.project.npcId);assertEquals(cache.toRealPath(),opened.cacheDirectory);
    }
    @Test void rejectsDirectoryWithoutJs5IdentityFiles(@TempDir Path dir){assertThrows(java.io.IOException.class,()->DesktopWorkflow.validateCache(dir));}
}
