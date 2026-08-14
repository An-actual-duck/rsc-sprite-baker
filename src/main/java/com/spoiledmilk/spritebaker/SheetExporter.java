package com.spoiledmilk.spritebaker;

import com.google.gson.GsonBuilder;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.runelite.cache.definitions.ModelDefinition;

public final class SheetExporter {
    private static final double[] YAW={0,45,90,135,180,90};
    public void export(AnimationWorkspace workspace,SpriteProject project,Path outputDirectory)throws IOException{
        Main.enforceOutputBoundary(outputDirectory.toAbsolutePath().normalize(),workspace.cachePath,Path.of("").toRealPath());
        Files.createDirectories(outputDirectory);
        List<ModelDefinition> poses=new ArrayList<>(); List<StaticRenderer.View> views=new ArrayList<>();
        for(int r=0;r<TargetSheet.ROWS;r++)for(int c=0;c<TargetSheet.COLUMNS;c++){
            PoseSelection selection=required(project.sheet.cells[r][c].pose,r,c);
            ModelDefinition pose=workspace.pose(selection,project.tweening); poses.add(pose); views.add(new StaticRenderer.View(pose,YAW[c]));
        }
        StaticRenderer renderer=new StaticRenderer(); StaticRenderer.Viewport viewport=renderer.fit(views,workspace.npc);
        BufferedImage sheet=new BufferedImage(StaticRenderer.WIDTH*6,StaticRenderer.HEIGHT*3,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=sheet.createGraphics(); int index=0;
        for(int r=0;r<3;r++)for(int c=0;c<6;c++)graphics.drawImage(renderer.render(List.of(poses.get(index++)),workspace.npc,YAW[c],viewport),c*256,r*256,null);
        graphics.dispose(); Path png=outputDirectory.resolve("npc-"+project.npcId+"-rsc-sheet.png"); ImageIO.write(sheet,"PNG",png.toFile());
        Path manifest=outputDirectory.resolve("npc-"+project.npcId+"-sheet-diagnostic.json");
        Map<String,Object> root=new LinkedHashMap<>();root.put("schemaVersion",1);root.put("npcId",project.npcId);
        root.put("componentModelIds",workspace.npc.modelIds);root.put("renderAnimationId",workspace.npc.renderAnimation);
        root.put("sequenceIds",Map.of("standing",project.standingSequenceId,"walking",project.walkingSequenceId,"combat",project.combatSequenceId));
        root.put("timelineUnitMillis",20);root.put("tweening",project.tweening);root.put("sharedViewport",Map.of("scale",viewport.scale,"centerX",viewport.centerX,"groundY",viewport.groundY));
        root.put("camera",Map.of("projection","orthographic","pitchDegrees",StaticRenderer.PITCH_DEGREES,"columnYawDegrees",YAW));
        root.put("lighting",Map.of("direction",StaticRenderer.LIGHT_DIRECTION,"ambient",StaticRenderer.AMBIENT_LIGHT,"diffuse",StaticRenderer.DIFFUSE_LIGHT));
        List<Map<String,Object>> cells=new ArrayList<>();
        for(int r=0;r<3;r++)for(int c=0;c<6;c++){TargetSheet.Cell cell=project.sheet.cells[r][c];PoseSelection p=cell.pose;Map<String,Object> trace=new LinkedHashMap<>();trace.put("row",r);trace.put("rowLabel",TargetSheet.ROW_LABELS[r]);trace.put("column",c);trace.put("columnLabel",TargetSheet.COLUMN_LABELS[c]);trace.put("yawDegrees",YAW[c]);trace.put("sequenceId",p.sequenceId);trace.put("frameIndex",p.frameIndex);trace.put("cycleOffset",p.cycleOffset);trace.put("timeMillis",p.timeMillis);trace.put("source",p.source);trace.put("locked",cell.locked);trace.put("override",cell.override);cells.add(trace);}
        root.put("cells",cells);root.put("png",png.getFileName().toString());root.put("pngSha256",Hashing.sha256(png));
        root.put("limitations",List.of("untextured models only","no animation blending or equipment overrides","global tween preview is user-selectable","framemap transform masks other than 65535 are not applied"));
        try(Writer writer=Files.newBufferedWriter(manifest)){new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root,writer);writer.write(System.lineSeparator());}
    }
    private static PoseSelection required(PoseSelection pose,int row,int col){if(pose==null)throw new IllegalStateException("unassigned sheet cell "+row+","+col);return pose;}
}
