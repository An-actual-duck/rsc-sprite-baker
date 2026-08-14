package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HeadlessMainTest {
    @Test void parsesSingleAndBatchModes(){BatchProcessor.Request single=HeadlessMain.parse(new String[]{"single","--cache","cache","--project","project.json","--output-dir","out","--name","npc-7","--validate-only"});assertEquals(Path.of("project.json"),single.singleProject);assertEquals(BatchProcessor.Mode.VALIDATE_ONLY,single.mode);BatchProcessor.Request batch=HeadlessMain.parse(new String[]{"batch","--cache","cache","--batch-manifest","batch.json","--output-dir","out","--dry-run"});assertEquals(Path.of("batch.json"),batch.manifestFile);assertEquals(BatchProcessor.Mode.DRY_RUN,batch.mode);}
    @Test void rejectsAmbiguousOrIncompleteCommands(){assertThrows(IllegalArgumentException.class,()->HeadlessMain.parse(new String[]{"single","--cache","cache","--output-dir","out"}));assertThrows(IllegalArgumentException.class,()->HeadlessMain.parse(new String[]{"batch","--cache","cache","--batch-manifest","batch.json","--output-dir","out","--dry-run","--validate-only"}));}
}
