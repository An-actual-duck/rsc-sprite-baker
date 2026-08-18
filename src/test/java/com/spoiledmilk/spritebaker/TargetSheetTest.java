package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetSheetTest {
    @Test void suggestionsNeverReplaceChoicesAndLocksPreventReplacement(){
        TargetSheet sheet=new TargetSheet();PoseSelection a=pose(1),b=pose(2),c=pose(3);
        assertTrue(sheet.suggest(0,0,a));sheet.override(0,0,b);assertFalse(sheet.suggest(0,0,c));assertEquals(2,sheet.cells[0][0].pose.sequenceId);
        sheet.cells[0][0].locked=true;sheet.override(0,0,c);assertEquals(2,sheet.cells[0][0].pose.sequenceId);
    }
    @Test void sharedRowsPopulateFiveDirectionsButRespectOverrides(){TargetSheet sheet=new TargetSheet();sheet.override(1,2,pose(7));sheet.assignShared(1,pose(8));for(int c=0;c<5;c++)assertEquals(c==2?7:8,sheet.cells[1][c].pose.sequenceId);assertNull(sheet.cells[1][5].pose);}
    @Test void autoPopulateRecommendsEntireSheetButPreservesLocks(){TargetSheet sheet=new TargetSheet();sheet.override(0,0,pose(90));sheet.override(1,2,pose(91));sheet.cells[1][2].locked=true;PoseSelection[] movement={pose(10),pose(11),pose(12)},combat={pose(20),pose(21),pose(22)};assertEquals(17,sheet.autoPopulate(movement,combat));for(int row=0;row<3;row++)for(int column=0;column<6;column++)assertEquals(row==1&&column==2?91:column<5?10+row:20+row,sheet.cells[row][column].pose.sequenceId);assertFalse(sheet.cells[0][0].override);assertTrue(sheet.cells[1][2].override);assertEquals("auto-populate",sheet.cells[2][5].pose.source);assertEquals(0,sheet.autoPopulate(movement,combat));}
    @Test void autoPopulateUsesMovementAsBoundedCombatFallback(){TargetSheet sheet=new TargetSheet();PoseSelection[] movement={pose(1),pose(2),pose(3)};assertEquals(18,sheet.autoPopulate(movement,null));for(int row=0;row<3;row++)assertEquals(1+row,sheet.cells[row][5].pose.sequenceId);}
    @Test void newlyDiscoveredCombatReplacesOnlyItsUnlockedColumn(){TargetSheet sheet=new TargetSheet();PoseSelection[] movement={pose(1),pose(2),pose(3)};sheet.autoPopulate(movement,null);sheet.override(0,0,pose(99));sheet.cells[2][5].locked=true;assertEquals(2,sheet.autoPopulateCombat(new PoseSelection[]{pose(10),pose(11),pose(12)}));assertEquals(99,sheet.cells[0][0].pose.sequenceId);assertEquals(10,sheet.cells[0][5].pose.sequenceId);assertEquals(11,sheet.cells[1][5].pose.sequenceId);assertEquals(3,sheet.cells[2][5].pose.sequenceId);}
    @Test void detectedCombatRefreshPreservesOverridesAndLocks(){TargetSheet sheet=new TargetSheet();sheet.suggest(0,5,pose(1));sheet.override(1,5,pose(2));sheet.suggest(2,5,pose(3));sheet.cells[2][5].locked=true;assertEquals(1,sheet.refreshDetectedCombat(new PoseSelection[]{pose(10),pose(11),pose(12)}));assertEquals(10,sheet.cells[0][5].pose.sequenceId);assertEquals("combat-detection",sheet.cells[0][5].pose.source);assertEquals(2,sheet.cells[1][5].pose.sequenceId);assertEquals(3,sheet.cells[2][5].pose.sequenceId);}
    @Test void projectPersistsSequenceFrameCycleTimeLocksAndVisuals(@TempDir Path dir)throws Exception{SpriteProject p=new SpriteProject();p.npcId=72;p.sheet.override(2,5,pose(9));p.sheet.cells[2][5].locked=true;p.visual.cellWidth=72;p.visual.pitchDegrees=19;p.visual.palette=PaletteReducer.RSC_27;p.visual.preset="Custom";Path file=dir.resolve("project.json");p.save(file);SpriteProject loaded=SpriteProject.load(file);assertEquals(9,loaded.sheet.cells[2][5].pose.sequenceId);assertEquals(4,loaded.sheet.cells[2][5].pose.frameIndex);assertEquals(60,loaded.sheet.cells[2][5].pose.timeMillis);assertTrue(loaded.sheet.cells[2][5].locked);assertEquals(72,loaded.visual.cellWidth);assertEquals(19,loaded.visual.pitchDegrees);assertEquals(PaletteReducer.RSC_27,loaded.visual.palette);}
    @Test void phaseOneProjectMigratesToVisualSchema(@TempDir Path dir)throws Exception{Path file=dir.resolve("legacy.json");Files.writeString(file,"{\"schemaVersion\":1,\"npcId\":72}");SpriteProject loaded=SpriteProject.load(file);assertEquals(2,loaded.schemaVersion);assertNotNull(loaded.visual);assertNotNull(loaded.sheet);assertEquals("Original colors",loaded.visual.preset);}
    @Test void projectCopyIsAnIndependentExportSnapshot(){SpriteProject project=new SpriteProject();project.npcId=72;project.sheet.override(0,0,pose(4));SpriteProject copy=project.copy();project.npcId=40;project.sheet.cells[0][0].pose.sequenceId=99;assertEquals(72,copy.npcId);assertEquals(4,copy.sheet.cells[0][0].pose.sequenceId);}
    private static PoseSelection pose(int id){PoseSelection p=new PoseSelection();p.sequenceId=id;p.frameIndex=4;p.cycleOffset=3;p.timeMillis=60;p.source="test";return p;}
}
