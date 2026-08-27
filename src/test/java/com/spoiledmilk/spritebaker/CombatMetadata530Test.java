package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombatMetadata530Test {
    @TempDir Path temporary;

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
}
