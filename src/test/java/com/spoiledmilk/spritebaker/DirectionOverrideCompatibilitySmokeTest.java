package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectionOverrideCompatibilitySmokeTest {
    @Test void abyssalLeechRendersPersistedReversedViewWithoutChangingSheetPosition(@TempDir Path output)throws Exception{
        String configured=System.getProperty("spritebaker.compatibilityCache");
        Assumptions.assumeTrue(configured!=null&&!configured.isBlank(),"local compatibility cache not configured");
        Path cache=Path.of(configured);CacheIdentity identity=CacheIdentity.read(cache);
        try(AnimationWorkspace workspace=new AnimationWorkspace(cache,2263)){
            assertEquals("Abyssal leech",workspace.npc.name);
            SpriteProject canonical=new SpriteProject();canonical.npcId=2263;AutomaticSheetBuilder.populate(canonical,workspace);
            new SheetExporter().export(workspace,canonical,output.resolve("canonical"));
            SpriteProject reversed=canonical.copy();PoseSelection pose=reversed.sheet.cells[0][0].pose;
            reversed.sheet.override(0,0,pose,4);
            new SheetExporter().export(workspace,reversed,output.resolve("reversed"));

            BufferedImage first=ImageIO.read(output.resolve("canonical/npc-2263-rsc-sheet.png").toFile());
            BufferedImage second=ImageIO.read(output.resolve("reversed/npc-2263-rsc-sheet.png").toFile());
            assertEquals(first.getWidth(),second.getWidth());assertEquals(first.getHeight(),second.getHeight());
            assertFalse(sameCell(first,second,0,0,canonical.visual.cellWidth,canonical.visual.cellHeight));
            try(Reader reader=java.nio.file.Files.newBufferedReader(output.resolve("reversed/npc-2263-sheet-diagnostic.json"))){
                JsonObject cell=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("cells").get(0).getAsJsonObject();
                assertEquals(0,cell.get("column").getAsInt());assertEquals(4,cell.get("sourceDirection").getAsInt());
                assertEquals(180.0,cell.get("yawDegrees").getAsDouble());assertTrue(cell.get("directionOverride").getAsBoolean());
            }
            System.out.println("Compatibility smoke: cache="+identity.report()+", npcId=2263, name="+workspace.npc.name
                +", modelIds="+Arrays.toString(workspace.npc.modelIds)+", standingSequenceId="+canonical.standingSequenceId
                +", walkingSequenceId="+canonical.walkingSequenceId+", combatSequenceId="+canonical.combatSequenceId
                +", destination=Standing/Facing camera, sourceDirection=Away, yawDegrees=180.0, render="
                +canonical.visual.preset+", cell="+canonical.visual.cellWidth+"x"+canonical.visual.cellHeight
                +", supersample="+canonical.visual.supersample+", pitchDegrees="+canonical.visual.pitchDegrees
                +", yawOffsetDegrees="+canonical.visual.yawOffsetDegrees+", modelScale="+canonical.visual.modelScale);
        }
    }

    private static boolean sameCell(BufferedImage left,BufferedImage right,int column,int row,int width,int height){
        for(int y=row*height;y<(row+1)*height;y++)for(int x=column*width;x<(column+1)*width;x++)if(left.getRGB(x,y)!=right.getRGB(x,y))return false;
        return true;
    }
}
