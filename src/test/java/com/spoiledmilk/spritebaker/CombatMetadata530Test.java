package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombatMetadata530Test {
    @TempDir Path temporary;
    private static final String REVISION="0123456789abcdef0123456789abcdef01234567";

    @Test void preservesMultipleDistinctAttackRolesAndDeduplicatesRepeatedSequenceIds()throws Exception{
        Path file=temporary.resolve("npc_configs.json");Files.writeString(file,"[{\"id\":\"7\",\"melee_animation\":\"40\",\"magic_animation\":\"41\",\"range_animation\":\"40\"}]");
        List<CombatMetadata530.Entry> entries=CombatMetadata530.loadFile(file,7);
        assertEquals(List.of(40,41),entries.stream().map(entry->entry.sequenceId).collect(java.util.stream.Collectors.toList()));
        assertEquals(List.of("melee","range"),entries.get(0).roles);assertEquals(List.of("magic"),entries.get(1).roles);
    }

    @Test void sparseOrNullCombatMetadataStaysBoundedAndDoesNotInventCandidates()throws Exception{
        Path file=temporary.resolve("sparse.json");Files.writeString(file,"[{\"id\":8,\"melee_animation\":null},{\"id\":9,\"range_animation\":\"\"}]");
        assertTrue(CombatMetadata530.loadFile(file,8).isEmpty());assertTrue(CombatMetadata530.loadFile(file,10).isEmpty());
    }

    @Test void packagedManifestWinsAndMalformedOrMissingManifestFallsBackSafely()throws Exception{
        Path root=temporary.resolve("app"),cache=Files.createDirectories(root.resolve("cache")),metadataSource=temporary.resolve("packaged-source.json");Files.writeString(metadataSource,"[{\"id\":7,\"melee_animation\":400,\"magic_animation\":401,\"range_animation\":402}]");Path manifest=root.resolve(CombatRoleManifest.RELATIVE_PATH);CombatRoleManifest.derive(metadataSource,REVISION).write(manifest);
        Path configs=Files.createDirectories(root.resolve("configs"));Files.writeString(configs.resolve("npc_configs.json"),"[{\"id\":7,\"melee_animation\":99}]");
        CombatMetadata530.LoadResult packaged=CombatMetadata530.loadWithDiagnostics(cache,7);assertEquals(List.of(400,401,402),packaged.entries.stream().map(entry->entry.sequenceId).collect(java.util.stream.Collectors.toList()));assertEquals("packaged combat-role manifest",packaged.provenance);
        Files.writeString(manifest,"{broken");CombatMetadata530.LoadResult malformed=CombatMetadata530.loadWithDiagnostics(cache,7);assertEquals(List.of(99),malformed.entries.stream().map(entry->entry.sequenceId).collect(java.util.stream.Collectors.toList()));assertFalse(malformed.diagnostics.isEmpty());
        Files.delete(manifest);CombatMetadata530.LoadResult missing=CombatMetadata530.loadWithDiagnostics(cache,7);assertEquals(List.of(99),missing.entries.stream().map(entry->entry.sequenceId).collect(java.util.stream.Collectors.toList()));
        Files.delete(configs.resolve("npc_configs.json"));CombatMetadata530.LoadResult absent=CombatMetadata530.loadWithDiagnostics(cache,7);assertTrue(absent.entries.isEmpty());
    }
}
