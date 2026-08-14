package com.spoiledmilk.spritebaker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Deterministic validation and all-or-nothing export of reviewed projects. */
public final class BatchProcessor {
    public static final String TOOL_VERSION="0.1.0-SNAPSHOT";
    public enum Mode { EXPORT, VALIDATE_ONLY, DRY_RUN }
    public static final class Request {
        public Path cache,output,manifestFile,singleProject,reportFile;public String singleOutputName;public Mode mode=Mode.EXPORT;
    }
    public static final class Result {
        public final boolean accepted;public final Map<String,Object> report;public final Path reportPath;
        Result(boolean accepted,Map<String,Object> report,Path reportPath){this.accepted=accepted;this.report=report;this.reportPath=reportPath;}
    }
    private static final String NAME_PATTERN="[A-Za-z0-9][A-Za-z0-9._-]{0,79}";

    public Result process(Request request)throws IOException{
        if(request.cache==null||request.output==null)throw new IOException("cache and output paths are required");
        Path output=request.output.toAbsolutePath().normalize();CacheIdentity cache=CacheIdentity.read(request.cache);
        Main.enforceOutputBoundary(output,cache.directory,Path.of("").toRealPath());
        if(request.reportFile!=null){Path report=request.reportFile.toAbsolutePath().normalize();Main.enforceOutputBoundary(report,cache.directory,Path.of("").toRealPath());if(report.startsWith(output))throw new IOException("report path must be outside the publishable output package");}
        LoadedBatch batch=loadBatch(request);List<Map<String,Object>> results=new ArrayList<>();List<Map<String,Object>> failures=new ArrayList<>();List<String> warnings=new ArrayList<>();
        validateCacheExpectation(batch.manifest.cache,cache,failures,batch.entries);
        Set<String> collisionNames=collisions(batch.entries,failures);
        AtomicPackage packageOutput=null;boolean needsRaster=request.mode!=Mode.DRY_RUN;
        if(needsRaster)try{packageOutput=new AtomicPackage(output);}catch(Exception e){for(ResolvedEntry entry:batch.entries)addFailure(failures,safe(entry.input.id),"output",root(e));}
        try{
            for(ResolvedEntry resolved:batch.entries){Map<String,Object> entryReport=new LinkedHashMap<>();entryReport.put("id",safe(resolved.input.id));entryReport.put("outputName",safe(resolved.input.outputName));List<String> entryWarnings=new ArrayList<>(),entryFailures=new ArrayList<>();
                if(collisionNames.contains(normalizeName(resolved.input.outputName)))entryFailures.add("output name collides with another entry");
                if(!cacheMatches(batch.manifest.cache,cache))entryFailures.add("cache identity does not match batch manifest");
                processEntry(resolved,cache,packageOutput==null&&needsRaster?Mode.DRY_RUN:request.mode,packageOutput,entryReport,entryWarnings,entryFailures);
                entryReport.put("warnings",entryWarnings);entryReport.put("failures",entryFailures);entryReport.put("status",entryFailures.isEmpty()?(request.mode==Mode.DRY_RUN?"dry-run-valid":request.mode==Mode.VALIDATE_ONLY?"validated":"staged"):"failed");results.add(entryReport);
                for(String failure:entryFailures)addFailure(failures,safe(resolved.input.id),"entry",failure);
            }
            boolean accepted=failures.isEmpty();Map<String,Object> mapping=accepted&&needsRaster?mapping(results):null;
            if(accepted&&needsRaster){Path mappingPath=packageOutput.staging.resolve("sprite-mapping.json");writeJson(mappingPath,mapping);String mappingHash=Hashing.sha256(mappingPath);if(request.mode==Mode.EXPORT)for(Map<String,Object> item:results)item.put("status","accepted");Map<String,Object> report=report(request,batch,cache,results,warnings,failures,mappingHash);writeJson(packageOutput.staging.resolve("batch-report.json"),report);if(request.mode==Mode.EXPORT){packageOutput.publish();return new Result(true,report,output.resolve("batch-report.json"));}Path reportPath=reportPath(request,output);AtomicPackage.atomicWrite(reportPath,json(report));return new Result(true,report,reportPath);}
            Map<String,Object> report=report(request,batch,cache,results,warnings,failures,null);Path reportPath=accepted&&request.mode==Mode.DRY_RUN?reportPath(request,output):failureReportPath(request,output);AtomicPackage.atomicWrite(reportPath,json(report));return new Result(accepted,report,reportPath);
        }finally{if(packageOutput!=null)packageOutput.close();}
    }

    private static void processEntry(ResolvedEntry resolved,CacheIdentity cache,Mode mode,AtomicPackage packageOutput,Map<String,Object> report,List<String> warnings,List<String> failures){
        BatchManifest.Entry input=resolved.input;try{
            validateName("entry id",input.id);validateName("outputName",input.outputName);int sourceSchema=validateProjectSchema(resolved.project);if(sourceSchema==1)warnings.add("project schemaVersion 1 was migrated in memory to schemaVersion 2");String projectHash=Hashing.sha256(resolved.project);report.put("projectSchemaVersion",sourceSchema);report.put("projectSha256",projectHash);expect("project SHA-256",input.expected==null?null:input.expected.projectSha256,projectHash);
            SpriteProject project=SpriteProject.load(resolved.project);validateProject(project,input,resolved.requireExplicitMapping);report.put("npcId",project.npcId);report.put("mapping",mapping(input.mapping));report.put("sourceAnimationSelections",selections(project));report.put("visualSettings",visual(project.visual));int width=Math.multiplyExact(project.visual.cellWidth,TargetSheet.COLUMNS),height=Math.multiplyExact(project.visual.cellHeight,TargetSheet.ROWS);report.put("dimensions",dimensions(width,height));
            if(input.expected!=null){expect("sheet width",input.expected.sheetWidth,width);expect("sheet height",input.expected.sheetHeight,height);}
            try(AnimationWorkspace workspace=new AnimationWorkspace(cache.directory,project.npcId)){TextureDiagnostics530.Report materials=TextureDiagnostics530.analyze(workspace.baseModel,workspace.npc,workspace.textures);report.put("npcName",workspace.npc.name);report.put("componentModelIds",workspace.npc.modelIds);report.put("materials",materials(materials));if(!materials.supported())throw new IOException(materials.summary());validatePoses(workspace,project);if(mode==Mode.DRY_RUN){if(input.expected!=null&&(present(input.expected.pngSha256)||present(input.expected.provenanceSha256)))warnings.add("expected output hashes were not checked in dry-run mode");return;}
                Path entryDirectory=packageOutput.staging.resolve("entries").resolve(input.outputName);new SheetExporter().export(workspace,project,entryDirectory);Path png=entryDirectory.resolve("npc-"+project.npcId+"-rsc-sheet.png"),provenance=entryDirectory.resolve("npc-"+project.npcId+"-sheet-diagnostic.json");ImageFacts image=inspectPng(png,width,height);String pngHash=Hashing.sha256(png),provenanceHash=Hashing.sha256(provenance);expect("PNG SHA-256",input.expected==null?null:input.expected.pngSha256,pngHash);expect("provenance SHA-256",input.expected==null?null:input.expected.provenanceSha256,provenanceHash);Map<String,Object> outputs=new LinkedHashMap<>();outputs.put("png","entries/"+input.outputName+"/"+png.getFileName());outputs.put("pngSha256",pngHash);outputs.put("provenance","entries/"+input.outputName+"/"+provenance.getFileName());outputs.put("provenanceSha256",provenanceHash);outputs.put("hasAlphaChannel",image.alphaChannel);outputs.put("hasTransparentPixels",image.transparent);outputs.put("hasVisiblePixels",image.visible);report.put("outputs",outputs);
            }
        }catch(Exception e){failures.add(root(e));}
    }

    private static LoadedBatch loadBatch(Request request)throws IOException{
        if((request.manifestFile==null)==(request.singleProject==null))throw new IOException("choose exactly one of a batch manifest or a single project");
        if(request.manifestFile!=null){Path file=request.manifestFile.toRealPath();BatchManifest manifest=BatchManifest.load(file);List<ResolvedEntry> entries=new ArrayList<>();Path parent=file.getParent();for(BatchManifest.Entry entry:manifest.entries){if(entry==null)throw new IOException("batch manifest contains a null entry");if(entry.project==null)throw new IOException("batch entry "+safe(entry.id)+" has no project path");Path relative=Path.of(entry.project);if(relative.isAbsolute())throw new IOException("batch project paths must be relative: "+entry.project);Path project=parent.resolve(relative).normalize();if(!project.startsWith(parent))throw new IOException("batch project path escapes manifest directory: "+entry.project);entries.add(new ResolvedEntry(entry,project,true));}return new LoadedBatch(manifest,entries,Hashing.sha256(file));}
        Path project=request.singleProject.toRealPath();BatchManifest manifest=new BatchManifest();BatchManifest.Entry entry=new BatchManifest.Entry();entry.id=request.singleOutputName;entry.outputName=request.singleOutputName;entry.project=project.getFileName().toString();manifest.entries.add(entry);return new LoadedBatch(manifest,List.of(new ResolvedEntry(entry,project,false)),null);
    }

    static int validateProjectSchema(Path path)throws IOException{try(Reader reader=Files.newBufferedReader(path)){JsonElement parsed=JsonParser.parseReader(reader);if(!parsed.isJsonObject())throw new IOException("project root must be an object");JsonObject root=parsed.getAsJsonObject();if(!root.has("schemaVersion"))throw new IOException("project schemaVersion is required");int schema=root.get("schemaVersion").getAsInt();if(schema!=1&&schema!=2)throw new IOException("unsupported project schemaVersion "+schema+" (expected 1 or 2)");return schema;}catch(com.google.gson.JsonParseException|IllegalStateException e){throw new IOException("invalid project JSON: "+e.getMessage(),e);}}
    private static void validateProject(SpriteProject project,BatchManifest.Entry input,boolean requireExplicitMapping){project.visual.validate();if(project.npcId<0)throw new IllegalArgumentException("NPC ID must be non-negative");if(project.sheet==null||project.sheet.cells==null||project.sheet.cells.length!=3)throw new IllegalArgumentException("project target sheet must contain 3 rows");for(int r=0;r<3;r++){if(project.sheet.cells[r]==null||project.sheet.cells[r].length!=6)throw new IllegalArgumentException("project target sheet row "+r+" must contain 6 cells");for(int c=0;c<6;c++)if(project.sheet.cells[r][c]==null||project.sheet.cells[r][c].pose==null)throw new IllegalArgumentException("unassigned sheet cell "+r+","+c);}
        BatchManifest.Mapping mapping=input.mapping;if(mapping==null)throw new IllegalArgumentException("mapping is required");if(!"npc".equals(mapping.assetKind))throw new IllegalArgumentException("mapping assetKind must be npc");if(requireExplicitMapping&&mapping.gameId<0)throw new IllegalArgumentException("batch mapping gameId must be explicit");if(mapping.gameId<0)mapping.gameId=project.npcId;if(mapping.gameId!=project.npcId)throw new IllegalArgumentException("mapping gameId "+mapping.gameId+" does not match project NPC "+project.npcId);validateName("mapping variant",mapping.variant);
    }
    private static void validatePoses(AnimationWorkspace workspace,SpriteProject project)throws IOException{for(int r=0;r<3;r++)for(int c=0;c<6;c++){PoseSelection pose=project.sheet.cells[r][c].pose;if(pose.sequenceId<0)throw new IllegalArgumentException("negative sequence ID in cell "+r+","+c);Sequence530 sequence=workspace.cache.loadSequence(pose.sequenceId);if(pose.frameIndex<0||pose.frameIndex>=sequence.frameIds.length)throw new IllegalArgumentException("frame index outside sequence in cell "+r+","+c);if(pose.cycleOffset<0||pose.cycleOffset>=sequence.durations[pose.frameIndex])throw new IllegalArgumentException("cycle offset outside frame duration in cell "+r+","+c);workspace.pose(pose,project.tweening);}}
    static ImageFacts inspectPng(Path png,int width,int height)throws IOException{BufferedImage image=ImageIO.read(png.toFile());if(image==null)throw new IOException("export did not produce a readable PNG");if(image.getWidth()!=width||image.getHeight()!=height)throw new IOException("PNG dimensions "+image.getWidth()+"x"+image.getHeight()+" do not match expected "+width+"x"+height);boolean alpha=image.getColorModel().hasAlpha(),transparent=false,visible=false;for(int y=0;y<height;y++)for(int x=0;x<width;x++){int a=image.getRGB(x,y)>>>24;if(a<255)transparent=true;if(a>0)visible=true;}if(!alpha||!transparent)throw new IOException("PNG must contain a transparency channel and transparent pixels");if(!visible)throw new IOException("PNG contains no visible sprite pixels");return new ImageFacts(alpha,transparent,visible);}

    private static Map<String,Object> report(Request request,LoadedBatch batch,CacheIdentity cache,List<Map<String,Object>> entries,List<String> warnings,List<Map<String,Object>> failures,String mappingHash){Map<String,Object> root=new LinkedHashMap<>();root.put("schemaVersion",1);root.put("tool",ordered("name","RSC Sprite Baker","version",TOOL_VERSION));root.put("mode",mode(request.mode));root.put("status",failures.isEmpty()?(request.mode==Mode.EXPORT?"accepted":request.mode==Mode.VALIDATE_ONLY?"validated":"dry-run-valid"):"failed");root.put("cacheIdentity",cache.report());if(batch.manifestHash!=null)root.put("batchManifestSha256",batch.manifestHash);root.put("entries",entries);if(mappingHash!=null)root.put("mappingSha256",mappingHash);root.put("warnings",warnings);root.put("failures",failures);return root;}
    private static Map<String,Object> mapping(List<Map<String,Object>> entries){Map<String,Object> root=new LinkedHashMap<>();root.put("schemaVersion",1);root.put("contract","spoiled-milk-remastered-sprite-handoff");List<Map<String,Object>> mapped=new ArrayList<>();for(Map<String,Object> report:entries){@SuppressWarnings("unchecked")Map<String,Object> source=(Map<String,Object>)report.get("mapping");@SuppressWarnings("unchecked")Map<String,Object> outputs=(Map<String,Object>)report.get("outputs");Map<String,Object> item=new LinkedHashMap<>();item.put("id",report.get("id"));item.put("assetKind",source.get("assetKind"));item.put("gameId",source.get("gameId"));item.put("variant",source.get("variant"));item.put("png",outputs.get("png"));item.put("provenance",outputs.get("provenance"));item.put("pngSha256",outputs.get("pngSha256"));mapped.add(item);}root.put("entries",mapped);return root;}
    private static Map<String,Object> mapping(BatchManifest.Mapping mapping){return ordered("assetKind",mapping.assetKind,"gameId",mapping.gameId,"variant",mapping.variant);}
    private static List<Map<String,Object>> selections(SpriteProject project){List<Map<String,Object>> out=new ArrayList<>();for(int r=0;r<3;r++)for(int c=0;c<6;c++){TargetSheet.Cell cell=project.sheet.cells[r][c];PoseSelection p=cell.pose;out.add(ordered("row",r,"column",c,"sequenceId",p.sequenceId,"frameIndex",p.frameIndex,"cycleOffset",p.cycleOffset,"timeMillis",p.timeMillis,"source",p.source,"locked",cell.locked,"override",cell.override));}return out;}
    private static Map<String,Object> visual(VisualSettings v){return ordered("preset",v.preset,"cellWidth",v.cellWidth,"cellHeight",v.cellHeight,"supersample",v.supersample,"padding",v.padding,"modelScale",v.modelScale,"pitchDegrees",v.pitchDegrees,"yawOffsetDegrees",v.yawOffsetDegrees,"verticalOffsetPixels",v.verticalOffsetPixels,"ambient",v.ambient,"diffuse",v.diffuse,"lightAzimuthDegrees",v.lightAzimuthDegrees,"lightElevationDegrees",v.lightElevationDegrees,"palette",v.palette,"dithering",v.dithering,"ditherStrength",v.ditherStrength);}
    private static Map<String,Object> materials(TextureDiagnostics530.Report report){return ordered("supported",report.supported(),"materialIds",report.materialIds,"supportedMaterialIds",report.supportedMaterialIds,"texturedFaces",report.texturedFaces,"type0Mappings",report.type0Mappings,"advancedMappingFallbacks",report.advancedMappingFallbacks,"faceLocalMappings",report.faceLocalMappings,"errors",report.errors);}
    private static Map<String,Object> dimensions(int width,int height){return ordered("width",width,"height",height,"columns",6,"rows",3);}
    private static Set<String> collisions(List<ResolvedEntry> entries,List<Map<String,Object>> failures){List<BatchManifest.Entry> inputs=new ArrayList<>();for(ResolvedEntry entry:entries)inputs.add(entry.input);Set<String> collisions=collisionNames(inputs);for(ResolvedEntry entry:entries)if(collisions.contains(normalizeName(entry.input.outputName)))addFailure(failures,safe(entry.input.id),"collision","outputName collides case-insensitively: "+safe(entry.input.outputName));return collisions;}
    static Set<String> collisionNames(List<BatchManifest.Entry> entries){Map<String,Integer> counts=new LinkedHashMap<>();for(BatchManifest.Entry entry:entries){String name=normalizeName(entry==null?null:entry.outputName);counts.put(name,counts.getOrDefault(name,0)+1);}Set<String> collisions=new HashSet<>();for(Map.Entry<String,Integer> item:counts.entrySet())if(item.getValue()>1)collisions.add(item.getKey());return collisions;}
    private static void validateCacheExpectation(BatchManifest.CacheExpectation expected,CacheIdentity actual,List<Map<String,Object>> failures,List<ResolvedEntry> entries){if(cacheMatches(expected,actual))return;for(ResolvedEntry entry:entries)addFailure(failures,safe(entry.input.id),"cache","cache identity does not match batch manifest");}
    private static boolean cacheMatches(BatchManifest.CacheExpectation expected,CacheIdentity actual){return expected==null||(matches(expected.dataFileSha256,actual.dataFileSha256)&&matches(expected.referenceIndexSha256,actual.referenceIndexSha256));}
    private static boolean matches(String expected,String actual){return !present(expected)||expected.equalsIgnoreCase(actual);}
    private static void expect(String label,String expected,String actual){if(present(expected)&&!expected.equalsIgnoreCase(actual))throw new IllegalArgumentException(label+" mismatch: expected "+expected+", got "+actual);}
    private static void expect(String label,Integer expected,int actual){if(expected!=null&&expected!=actual)throw new IllegalArgumentException(label+" mismatch: expected "+expected+", got "+actual);}
    private static void validateName(String label,String value){if(value==null||!value.matches(NAME_PATTERN)||".".equals(value)||"..".equals(value))throw new IllegalArgumentException(label+" must match "+NAME_PATTERN);}
    private static String normalizeName(String value){return safe(value).toLowerCase(Locale.ROOT);}
    private static boolean present(String value){return value!=null&&!value.isBlank();}
    private static String safe(String value){return value==null?"<missing>":value;}
    private static String mode(Mode value){return value==Mode.EXPORT?"export":value==Mode.VALIDATE_ONLY?"validate-only":"dry-run";}
    private static void addFailure(List<Map<String,Object>> failures,String entry,String stage,String message){failures.add(ordered("entry",entry,"stage",stage,"message",message));}
    private static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.getMessage()==null?e.toString():e.getMessage();}
    private static Map<String,Object> ordered(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    private static String json(Object value){return BatchManifest.gson().toJson(value)+System.lineSeparator();}
    private static void writeJson(Path path,Object value)throws IOException{Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,json(value));}
    private static Path reportPath(Request request,Path output){return request.reportFile==null?output.resolveSibling(output.getFileName()+"-validation-report.json"):request.reportFile;}
    private static Path failureReportPath(Request request,Path output){return request.reportFile==null?output.resolveSibling(output.getFileName()+"-failed-report.json"):request.reportFile;}
    private static final class ResolvedEntry{final BatchManifest.Entry input;final Path project;final boolean requireExplicitMapping;ResolvedEntry(BatchManifest.Entry input,Path project,boolean requireExplicitMapping){this.input=input;this.project=project;this.requireExplicitMapping=requireExplicitMapping;}}
    private static final class LoadedBatch{final BatchManifest manifest;final List<ResolvedEntry> entries;final String manifestHash;LoadedBatch(BatchManifest manifest,List<ResolvedEntry> entries,String manifestHash){this.manifest=manifest;this.entries=entries;this.manifestHash=manifestHash;}}
    static final class ImageFacts{final boolean alphaChannel,transparent,visible;ImageFacts(boolean alpha,boolean transparent,boolean visible){alphaChannel=alpha;this.transparent=transparent;this.visible=visible;}}
}
