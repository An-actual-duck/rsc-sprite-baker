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
    public void export(AnimationWorkspace workspace,SpriteProject project,Path outputDirectory)throws IOException{
        export(workspace,project,DesktopExportPlan.legacy(outputDirectory,project.npcId));
    }
    void export(AnimationWorkspace workspace,SpriteProject project,DesktopExportPlan plan)throws IOException{
        project.visual.validate();
        Path outputDirectory=plan.directory;
        Main.enforceOutputBoundary(outputDirectory.toAbsolutePath().normalize(),workspace.cachePath,Path.of("").toRealPath());
        Files.createDirectories(outputDirectory);
        List<ModelDefinition> poses=new ArrayList<>(); List<StaticRenderer.View> views=new ArrayList<>();
        for(int r=0;r<TargetSheet.ROWS;r++)for(int c=0;c<TargetSheet.COLUMNS;c++){
            PoseSelection selection=required(project.sheet.cells[r][c].pose,r,c);
            ModelDefinition pose=workspace.pose(selection,project.tweening); poses.add(pose); views.add(new StaticRenderer.View(pose,SheetDirection.yawDegrees(c)));
        }
        StaticRenderer renderer=new StaticRenderer(); StaticRenderer.Viewport viewport=renderer.fitStyled(views,workspace.npc,project.visual);
        BufferedImage sheet=new BufferedImage(project.visual.cellWidth*6,project.visual.cellHeight*3,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=sheet.createGraphics(); int index=0;
        for(int r=0;r<3;r++)for(int c=0;c<6;c++)graphics.drawImage(renderer.renderStyled(List.of(poses.get(index++)),workspace.npc,SheetDirection.yawDegrees(c),viewport,project.visual,workspace.textures),c*project.visual.cellWidth,r*project.visual.cellHeight,null);
        graphics.dispose(); Path png=plan.png; ImageIO.write(sheet,"PNG",png.toFile());
        Path manifest=plan.manifest;
        Map<String,Object> root=new LinkedHashMap<>();root.put("schemaVersion",2);root.put("npcId",project.npcId);
        Map<String,Object> cacheIdentity=new LinkedHashMap<>();cacheIdentity.put("directory",workspace.cachePath.toString());cacheIdentity.put("layout","JS5 dat2 with idx files");
        cacheIdentity.put("dataFile",fileIdentity(workspace.cachePath.resolve("main_file_cache.dat2")));cacheIdentity.put("referenceIndex",fileIdentity(workspace.cachePath.resolve("main_file_cache.idx255")));root.put("cache",cacheIdentity);
        root.put("componentModelIds",workspace.npc.modelIds);root.put("renderAnimationId",workspace.npc.renderAnimation);
        TextureDiagnostics530.Report textureReport=TextureDiagnostics530.analyze(workspace.baseModel,workspace.npc,workspace.textures);Map<String,Object> material=new LinkedHashMap<>();material.put("materialIds",textureReport.materialIds);material.put("supportedMaterialIds",textureReport.supportedMaterialIds);material.put("texturedFaces",textureReport.texturedFaces);material.put("type0Mappings",textureReport.type0Mappings);material.put("advancedMappingFallbacks",textureReport.advancedMappingFallbacks);material.put("faceLocalMappings",textureReport.faceLocalMappings);material.put("errors",textureReport.errors);material.put("textureGamma",1.0);material.put("alphaBehavior","index-26 opaque flag; zero-RGB discard for alpha-tested materials; face alpha source-over");material.put("definitions",materialSettings(workspace.textures));root.put("materials",material);
        Map<String,Object> appearance=new LinkedHashMap<>();appearance.put("recolors",pairs(workspace.npc.recolorFrom,workspace.npc.recolorTo));appearance.put("retextures",pairs(workspace.npc.retextureFrom,workspace.npc.retextureTo));appearance.put("widthScale",workspace.npc.widthScale);appearance.put("heightScale",workspace.npc.heightScale);root.put("appearance",appearance);
        root.put("sequenceIds",ordered("standing",project.standingSequenceId,"walking",project.walkingSequenceId,"combat",project.combatSequenceId));
        root.put("timelineUnitMillis",20);root.put("tweening",project.tweening);root.put("selectionBehavior",ordered("sharedMovementColumns",5,"cellOverridesAllowed",true,"locksPersisted",true,"suggestionsReplaceExisting",false,"mirroredPreview",project.mirroredPreview));
        root.put("render",renderSettings(project.visual,viewport));
        List<Map<String,Object>> cells=new ArrayList<>();
        for(int r=0;r<3;r++)for(int c=0;c<6;c++){TargetSheet.Cell cell=project.sheet.cells[r][c];PoseSelection p=cell.pose;Sequence530 sequence=workspace.cache.loadSequence(p.sequenceId);Frame530 frame=workspace.cache.loadFrame(sequence.frameIds[p.frameIndex]);Map<String,Object> trace=new LinkedHashMap<>();trace.put("row",r);trace.put("rowLabel",TargetSheet.ROW_LABELS[r]);trace.put("column",c);trace.put("columnLabel",TargetSheet.COLUMN_LABELS[c]);trace.put("yawDegrees",SheetDirection.yawDegrees(c));trace.put("sequenceId",p.sequenceId);trace.put("frameIndex",p.frameIndex);trace.put("packedFrameId",sequence.frameIds[p.frameIndex]);trace.put("framemapId",frame.framemap.id);trace.put("frameDurationCycles",sequence.durations[p.frameIndex]);trace.put("cycleOffset",p.cycleOffset);trace.put("timeMillis",p.timeMillis);trace.put("source",p.source);trace.put("locked",cell.locked);trace.put("override",cell.override);cells.add(trace);}
        root.put("cells",cells);root.put("png",png.getFileName().toString());root.put("pngSha256",Hashing.sha256(png));
        root.put("limitations",List.of("procedural material behavior remains fail-closed when an unimplemented operation or combine function is encountered","no animation blending or equipment overrides","global tween preview is user-selectable","framemap transform masks other than 65535 are not applied"));
        try(Writer writer=Files.newBufferedWriter(manifest)){new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root,writer);writer.write(System.lineSeparator());}
    }
    private static PoseSelection required(PoseSelection pose,int row,int col){if(pose==null)throw new IllegalStateException("unassigned sheet cell "+row+","+col);return pose;}
    private static Map<String,Object> renderSettings(VisualSettings settings,StaticRenderer.Viewport viewport){Map<String,Object> out=new LinkedHashMap<>();out.put("preset",settings.preset);out.put("projection","orthographic");out.put("cellWidth",settings.cellWidth);out.put("cellHeight",settings.cellHeight);out.put("sheetWidth",settings.cellWidth*6);out.put("sheetHeight",settings.cellHeight*3);out.put("supersample",settings.supersample);out.put("internalCellWidth",settings.cellWidth*settings.supersample);out.put("internalCellHeight",settings.cellHeight*settings.supersample);out.put("reduction",MaterialStylizer.RSC_RAMPS.equals(settings.materialStyle)?"material-aware robust block reduction":"nearest-neighbor center sample");out.put("padding",settings.padding);out.put("modelScale",settings.modelScale);out.put("pitchDegrees",settings.pitchDegrees);out.put("yawOffsetDegrees",settings.yawOffsetDegrees);out.put("baseColumnYawDegrees",SheetDirection.YAW_DEGREES);out.put("verticalOffsetPixels",settings.verticalOffsetPixels);out.put("sharedAnchor","origin-centered horizontal, minimum projected Y ground");out.put("sharedViewport",ordered("internalPixelsPerModelUnit",viewport.scale,"centerX",viewport.centerX,"groundY",viewport.groundY));out.put("lighting",ordered("ambient",settings.ambient,"diffuse",settings.diffuse,"azimuthDegrees",settings.lightAzimuthDegrees,"elevationDegrees",settings.lightElevationDegrees,"direction",settings.lightDirection()));out.put("color",ordered("materialStyle",settings.materialStyle,"palette",settings.palette,"dithering",settings.dithering,"ditherStrength",settings.ditherStrength));return out;}
    private static Map<String,Object> fileIdentity(Path path)throws IOException{Map<String,Object> out=new LinkedHashMap<>();out.put("name",path.getFileName().toString());out.put("bytes",Files.size(path));out.put("sha256",Hashing.sha256(path));return out;}
    private static List<Map<String,Integer>> pairs(short[] from,short[] to){List<Map<String,Integer>> out=new ArrayList<>();for(int i=0;i<from.length;i++){Map<String,Integer> pair=new LinkedHashMap<>();pair.put("from",Short.toUnsignedInt(from[i]));pair.put("to",Short.toUnsignedInt(to[i]));out.add(pair);}return out;}
    private static List<Map<String,Object>> materialSettings(TextureProvider530 provider){List<Map<String,Object>> out=new ArrayList<>();provider.loaded().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{TextureMaterial530 value=entry.getValue();MaterialDefinition530 definition=value.definition;Map<String,Object> item=new LinkedHashMap<>();item.put("id",entry.getKey());item.put("size",value.size);item.put("opaque",definition.opaque);item.put("lowDetail",definition.lowDetail);item.put("averageColor",definition.averageColor);item.put("colorBoost",definition.colorBoost());item.put("grayscaleBlend",definition.grayscaleBlend());item.put("materialType",definition.materialType());item.put("materialArgument",definition.materialArgument());item.put("scrollU",definition.scrollU);item.put("scrollV",definition.scrollV);item.put("effect",definition.effect);item.put("effectParam",definition.effectParam);item.put("operationTypes",value.operationTypes);out.add(item);});return out;}
    private static Map<String,Object> ordered(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
}
