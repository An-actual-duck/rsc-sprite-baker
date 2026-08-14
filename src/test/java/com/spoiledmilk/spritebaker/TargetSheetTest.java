package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetSheetTest {
    @Test void suggestionsNeverReplaceChoicesAndLocksPreventReplacement(){
        TargetSheet sheet=new TargetSheet();PoseSelection a=pose(1),b=pose(2),c=pose(3);
        assertTrue(sheet.suggest(0,0,a));sheet.override(0,0,b);assertFalse(sheet.suggest(0,0,c));assertEquals(2,sheet.cells[0][0].pose.sequenceId);
        sheet.cells[0][0].locked=true;sheet.override(0,0,c);assertEquals(2,sheet.cells[0][0].pose.sequenceId);
    }
    @Test void sharedRowsPopulateFiveDirectionsButRespectOverrides(){TargetSheet sheet=new TargetSheet();sheet.override(1,2,pose(7));sheet.assignShared(1,pose(8));for(int c=0;c<5;c++)assertEquals(c==2?7:8,sheet.cells[1][c].pose.sequenceId);assertNull(sheet.cells[1][5].pose);}
    @Test void projectPersistsSequenceFrameCycleTimeLocksAndVisuals(@TempDir Path dir)throws Exception{SpriteProject p=new SpriteProject();p.npcId=72;p.sheet.override(2,5,pose(9));p.sheet.cells[2][5].locked=true;p.visual.cellWidth=72;p.visual.pitchDegrees=19;p.visual.palette=PaletteReducer.RSC_27;p.visual.preset="Custom";Path file=dir.resolve("project.json");p.save(file);SpriteProject loaded=SpriteProject.load(file);assertEquals(9,loaded.sheet.cells[2][5].pose.sequenceId);assertEquals(4,loaded.sheet.cells[2][5].pose.frameIndex);assertEquals(60,loaded.sheet.cells[2][5].pose.timeMillis);assertTrue(loaded.sheet.cells[2][5].locked);assertEquals(72,loaded.visual.cellWidth);assertEquals(19,loaded.visual.pitchDegrees);assertEquals(PaletteReducer.RSC_27,loaded.visual.palette);}
    private static PoseSelection pose(int id){PoseSelection p=new PoseSelection();p.sequenceId=id;p.frameIndex=4;p.cycleOffset=3;p.timeMillis=60;p.source="test";return p;}
}
