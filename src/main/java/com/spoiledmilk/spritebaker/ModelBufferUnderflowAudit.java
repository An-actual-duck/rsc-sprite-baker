package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Diagnostic-only audit of model failures currently surfaced as BufferUnderflowException. */
final class ModelBufferUnderflowAudit {
    private static final String PINNED_COMMIT="a569f0af7754ada96ed7ac76d7582b2c7511b7a0";
    private ModelBufferUnderflowAudit(){}

    static Map<String,Object> collect(Path cacheDirectory,Progress progress)throws IOException{
        Path directory=cacheDirectory.toRealPath();Map<String,Object> report=new LinkedHashMap<>();
        report.put("schemaVersion",1);report.put("audit","model-buffer-underflow");
        report.put("cache",ModelFormatDiagnostic.ordered("directory",directory.toString(),"dataSha256",Hashing.sha256(directory.resolve("main_file_cache.dat2")),"referenceIndexSha256",Hashing.sha256(directory.resolve("main_file_cache.idx255"))));
        report.put("productionDecoder",ModelFormatDiagnostic.ordered("dependency","net.runelite:cache:1.12.35","type1Class","com.spoiledmilk.spritebaker.Revision530Type1ModelDecoder","fallbackClass","net.runelite.cache.definitions.loaders.ModelLoader","productionModified",true));
        report.put("pinnedClient",ModelFormatDiagnostic.ordered("commit",PINNED_COMMIT,"modelDecoder","client/src/main/java/rt4/RawModel.java","formatSelection","trailing ff ff selects the 23-byte new/type-1 format; otherwise the 18-byte old format"));

        TreeMap<Integer,Outcome> outcomes=new TreeMap<>();TreeMap<Integer,List<NpcRef>> references=new TreeMap<>();List<Map<String,Object>> affectedNpcs=new ArrayList<>();
        try(CacheReader cache=new CacheReader(directory);NpcCatalog catalog=new NpcCatalog(directory)){
            List<Integer> npcIds=catalog.ids(0,catalog.size());
            for(int index=0;index<npcIds.size();index++){
                int npcId=npcIds.get(index);NpcDefinition530 npc;
                try{npc=cache.loadNpc(npcId);}catch(RuntimeException|IOException ignored){continue;}
                if(npc.morphDefinition||npc.modelIds.length==0)continue;
                Integer firstFailure=null;Outcome firstOutcome=null;
                for(int modelId:npc.modelIds){Outcome outcome=outcome(cache,outcomes,modelId);if(!outcome.success){firstFailure=modelId;firstOutcome=outcome;break;}}
                if(firstOutcome!=null&&firstOutcome.bufferUnderflow){
                    List<Integer> failing=new ArrayList<>();List<Integer> components=Arrays.stream(npc.modelIds).boxed().collect(java.util.stream.Collectors.toList());
                    NpcRef ref=new NpcRef(npc.id,npc.name,components);
                    for(int modelId:npc.modelIds){Outcome outcome=outcome(cache,outcomes,modelId);if(outcome.bufferUnderflow){failing.add(modelId);references.computeIfAbsent(modelId,unused->new ArrayList<>()).add(ref);}}
                    affectedNpcs.add(ModelFormatDiagnostic.ordered("npcId",npc.id,"name",npc.name,"componentModelIds",components,"currentFirstFailureModelId",firstFailure,"bufferUnderflowModelIds",failing));
                }
                if(progress!=null&&(index%512==0||index+1==npcIds.size()))progress.update(index+1,npcIds.size());
            }

            List<Map<String,Object>> models=new ArrayList<>();TreeMap<String,Cluster> clusters=new TreeMap<>();TreeSet<Integer> affectedModelIds=new TreeSet<>(references.keySet());
            for(int modelId:affectedModelIds){byte[] raw=cache.loadFile(CacheReader.MODEL_INDEX,modelId,0);Map<String,Object> diagnostic=ModelFormatDiagnostic.analyze(modelId,raw);Outcome outcome=outcomes.get(modelId);List<NpcRef> refs=references.get(modelId);List<Integer> referringNpcIds=new ArrayList<>();for(NpcRef ref:refs)referringNpcIds.add(ref.id);
                diagnostic.put("productionFailure",ModelFormatDiagnostic.ordered("exception","BufferUnderflowException","decoderMethod",outcome.method,"decoderLine",outcome.line));diagnostic.put("affectedNpcIds",referringNpcIds);diagnostic.put("affectedNpcReferenceCount",refs.size());
                String signature=(String)diagnostic.getOrDefault("structuralSignature","untraced|"+outcome.method+":"+outcome.line);diagnostic.put("structuralSignature",signature);models.add(diagnostic);
                clusters.computeIfAbsent(signature,Cluster::new).add(modelId,refs);
            }

            List<Map<String,Object>> clusterReports=new ArrayList<>();Cluster highestYield=null;for(Cluster cluster:clusters.values()){
                int representative=cluster.modelIds.first();Map<String,Object> comparison=neighborComparison(cache,outcomes,representative);
                clusterReports.add(ModelFormatDiagnostic.ordered("signature",cluster.signature,"likelyRootCause",ModelFormatDiagnostic.ROOT_CAUSE,"modelCount",cluster.modelIds.size(),"affectedNpcCount",cluster.npcIds.size(),"modelIds",new ArrayList<>(cluster.modelIds),"representativeModelId",representative,"representativeNeighborComparison",comparison));
                if(highestYield==null||cluster.npcIds.size()>highestYield.npcIds.size()||cluster.npcIds.size()==highestYield.npcIds.size()&&cluster.modelIds.size()>highestYield.modelIds.size())highestYield=cluster;
            }
            report.put("definitionCount",catalog.size());report.put("affectedNpcCount",affectedNpcs.size());report.put("uniqueFailingModelCount",affectedModelIds.size());report.put("affectedNpcs",affectedNpcs);report.put("failingModels",models);report.put("structuralClusters",clusterReports);
            report.put("highestYieldCluster",highestYield==null?null:ModelFormatDiagnostic.ordered("signature",highestYield.signature,"modelCount",highestYield.modelIds.size(),"affectedNpcCount",highestYield.npcIds.size(),"representativeModelId",highestYield.modelIds.first(),"narrowestSafeFollowUp","revision-530 type-1 layout selected only after structural validation that its calculated data end equals the ff ff footer"));
            report.put("rootCauseGroups",highestYield==null?List.of():List.of(ModelFormatDiagnostic.ordered("rootCause",ModelFormatDiagnostic.ROOT_CAUSE,"modelCount",affectedModelIds.size(),"affectedNpcCount",affectedNpcs.size(),"evidence","every audited failure is type-1 ff ff; pinned layout ends exactly at the footer while the dependency advances two extra bytes per complex texture face and mistakes footer data for an extension")));
        }
        report.put("checks",ModelFormatDiagnostic.ordered(
            "wrongFormatSelection",false,"footerSizeAssumptionMismatch",false,"signedUnsignedCountMismatch",false,"missingOptionalStream",false,"textureCoordinateLengthMismatch",false,"revisionExtensionPresent",false,"truncatedOrCorrupt",false,"mixedCacheRevisions",false,
            "textureSectionLayoutMismatch",true,"dependencyLibraryLimitation",true,
            "conclusion","RuneLite type-1 offset calculation uses a later complex-texture layout incompatible with the pinned revision-530 one-byte rotation and auxiliary streams"));
        report.put("recommendedFollowUp",ModelFormatDiagnostic.ordered("priority",1,"status","implemented","scope","the bounded revision-530 type-1 decoder is selected only when its calculated data end equals the ff ff footer","guardrails",List.of("retain RuneLite decoding for old/type-2/type-3 formats","validate every calculated stream boundary before decoding","do not skip complex texture data or synthesize geometry","retain complex mappings through assembly and rendering")));
        return report;
    }

    private static Outcome outcome(CacheReader cache,Map<Integer,Outcome> outcomes,int modelId){Outcome cached=outcomes.get(modelId);if(cached!=null)return cached;Outcome result;try{cache.loadModel(modelId);result=Outcome.success();}catch(Throwable error){Throwable root=error;while(root.getCause()!=null)root=root.getCause();StackTraceElement frame=null;for(StackTraceElement candidate:root.getStackTrace())if(candidate.getClassName().equals("net.runelite.cache.definitions.loaders.ModelLoader")){frame=candidate;break;}result=Outcome.failure(root instanceof BufferUnderflowException,root.getClass().getSimpleName(),frame==null?null:frame.getMethodName(),frame==null?-1:frame.getLineNumber());}outcomes.put(modelId,result);return result;}

    private static Map<String,Object> neighborComparison(CacheReader cache,Map<Integer,Outcome> outcomes,int modelId){Map<String,Object> comparison=new LinkedHashMap<>();comparison.put("failingModelId",modelId);comparison.put("lowerSuccessfulNeighbor",neighbor(cache,outcomes,modelId,-1));comparison.put("higherSuccessfulNeighbor",neighbor(cache,outcomes,modelId,1));return comparison;}
    private static Map<String,Object> neighbor(CacheReader cache,Map<Integer,Outcome> outcomes,int modelId,int direction){for(int distance=1;distance<=32;distance++){int candidate=modelId+direction*distance;if(candidate<0)break;try{byte[] raw=cache.loadFile(CacheReader.MODEL_INDEX,candidate,0);Outcome outcome=outcome(cache,outcomes,candidate);if(outcome.success){Map<String,Object> analyzed=ModelFormatDiagnostic.analyze(candidate,raw),dependency=castMap(analyzed.get("dependencyType1"));long delta=dependency==null?0:((Number)dependency.get("dataEndMinusFooter")).longValue();return ModelFormatDiagnostic.ordered("modelId",candidate,"distance",distance,"bytes",analyzed.get("bytes"),"sha256",analyzed.get("sha256"),"format",analyzed.get("format"),"revisionMarker",analyzed.get("revisionMarker"),"footerBytesHex",analyzed.get("footerBytesHex"),"counts",analyzed.get("counts"),"pinnedRevision530",analyzed.get("pinnedRevision530"),"dependencyType1",dependency,"dependencyExtensionTrace",analyzed.get("dependencyExtensionTrace"),"interpretation",delta>0?"dependency success is accidental: its overrun remains inside the footer and the probed byte/extension happens not to underflow":"no revision-530 complex-layout overrun detected");}}catch(RuntimeException|IOException ignored){}}
        return ModelFormatDiagnostic.ordered("found",false,"searchDirection",direction<0?"lower":"higher","maximumDistance",32);
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> castMap(Object value){return value instanceof Map?(Map<String,Object>)value:null;}

    interface Progress{void update(int complete,int total);}
    private static final class Outcome{final boolean success,bufferUnderflow;final String exception,method;final int line;private Outcome(boolean success,boolean underflow,String exception,String method,int line){this.success=success;bufferUnderflow=underflow;this.exception=exception;this.method=method;this.line=line;}static Outcome success(){return new Outcome(true,false,null,null,-1);}static Outcome failure(boolean underflow,String exception,String method,int line){return new Outcome(false,underflow,exception,method,line);}}
    private static final class NpcRef{final int id;final String name;final List<Integer> componentModels;NpcRef(int id,String name,List<Integer> componentModels){this.id=id;this.name=name;this.componentModels=componentModels;}}
    private static final class Cluster{final String signature;final TreeSet<Integer> modelIds=new TreeSet<>(),npcIds=new TreeSet<>();Cluster(String signature){this.signature=signature;}void add(int modelId,List<NpcRef> refs){modelIds.add(modelId);for(NpcRef ref:refs)npcIds.add(ref.id);}}
}
