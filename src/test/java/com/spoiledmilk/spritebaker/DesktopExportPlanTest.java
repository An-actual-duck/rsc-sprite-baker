package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopExportPlanTest {
    @Test
    void createsDeterministicHumanReadableNpcPaths(@TempDir Path directory) {
        DesktopExportPlan plan = DesktopExportPlan.forNpc(directory, 72, "Troll (Level 14)");
        assertEquals("npc-72-troll-level-14-rsc-sheet.png", plan.png.getFileName().toString());
        assertEquals("npc-72-troll-level-14-sheet-provenance.json", plan.manifest.getFileName().toString());
        assertFalse(plan.wouldOverwrite());
    }

    @Test
    void detectsEitherExactOutputBeforeOverwrite(@TempDir Path directory) throws Exception {
        DesktopExportPlan plan = DesktopExportPlan.forNpc(directory, 40, "Shark");
        Files.createFile(plan.manifest);
        assertTrue(plan.wouldOverwrite());
        assertFalse(Files.exists(plan.png));
    }
}
