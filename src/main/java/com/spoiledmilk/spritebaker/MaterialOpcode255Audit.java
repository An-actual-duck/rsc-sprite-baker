package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.runelite.cache.definitions.ModelDefinition;

/** Deterministic forensic audit for the apparent opcode-255 material failure. */
final class MaterialOpcode255Audit {
    private static final Set<Integer> PREVIOUSLY_AFFECTED_NPCS=Set.of(42,1648,1649,1654,1655,1762,1764,2031,2493,3311,3336,3484,3599,3600,3601,3602,3603,3604,3605,3727,3728,3729,3730,3731,3747,3748,3749,3750,3751,3939,3940,3943,5148,5149,5150,5151,5152,5153,5154,5155,5165,5172,5533,5534,5535,5536,5537,5538,5547,5548,5549,5550,5551,5552,6815,6816,7595,7596,7597,7598,7642,7643);
    private static final String PINNED_COMMIT = "a569f0af7754ada96ed7ac76d7582b2c7511b7a0";

    private MaterialOpcode255Audit(){}

    static Map<String,Object> collect(Path cacheDirectory,Progress progress)throws IOException {
        Path directory=cacheDirectory.toRealPath();Map<String,Object> report=new LinkedHashMap<>();
        report.put("schemaVersion",3);report.put("audit","material-opcode-255");
        report.put("cache",ordered("directory",directory.toString(),"dataSha256",Hashing.sha256(directory.resolve("main_file_cache.dat2")),"referenceIndexSha256",Hashing.sha256(directory.resolve("main_file_cache.idx255"))));
        report.put("pinnedClient",ordered("repository","https://github.com/conan513/2009scape-client","commit",PINNED_COMMIT,"factory","client/src/main/java/rt4/Texture.java","monochromeFill","client/src/main/java/rt4/TextureOpMonochromeFill.java","curve","client/src/main/java/rt4/TextureOpCurve.java","operation17","client/src/main/java/rt4/TextureOp17.java","materialMetadata","client/src/main/java/rt4/GlTexture.java"));

        List<NpcCompatibility> affected=new ArrayList<>();Set<Integer> graphIds=new TreeSet<>();graphIds.add(168);
        try(NpcCatalog catalog=new NpcCatalog(directory)){
            List<Integer> ids=catalog.ids(0,catalog.size());for(int i=0;i<ids.size();i++){
                NpcCompatibility result=catalog.assess(ids.get(i));if(PREVIOUSLY_AFFECTED_NPCS.contains(result.npcId))affected.add(result);if(progress!=null&&(i%512==0||i+1==ids.size()))progress.update(i+1,ids.size());
            }
        }

        List<Map<String,Object>> graphs=new ArrayList<>();
        try(CacheReader cache=new CacheReader(directory)){
            for(int graphId:graphIds){byte[] data=cache.loadFile(9,graphId,0);Trace legacy=trace(data,false),pinned=trace(data,true);List<Map<String,Object>> occurrences=new ArrayList<>();
                for(Node node:legacy.nodes)if(node.type==255){int from=Math.max(0,node.typeOffset-12),to=Math.min(data.length,node.typeOffset+13);occurrences.add(ordered("offset",node.typeOffset,"rawByte","ff","windowStart",from,"windowEndExclusive",to,"windowHex",hex(data,from,to)));}
                graphs.add(ordered("materialId",graphId,"textureGraphId",graphId,"index",9,"archive",graphId,"file",0,"bytes",data.length,"sha256",sha256(data),"rawFfOffsets",byteOffsets(data,255),"legacyTwoByteFraming",legacy.report(),"productionPinnedFraming",pinned.report(),"apparentOpcode255Occurrences",occurrences));
            }

            TreeSet<Integer> allModels=new TreeSet<>(),allMaterials=new TreeSet<>(),directModels=new TreeSet<>();List<Map<String,Object>> routes=new ArrayList<>();
            for(NpcCompatibility result:affected){allModels.addAll(result.modelIds);allMaterials.addAll(result.materialIds);NpcDefinition530 npc=cache.loadNpc(result.npcId);
                for(int modelId:npc.modelIds){ModelDefinition model=cache.loadModel(modelId);if(model.faceTextures==null)continue;TreeMap<Integer,Integer> facesBySource=new TreeMap<>();
                    for(short encoded:model.faceTextures){if(encoded==-1)continue;int source=Short.toUnsignedInt(encoded),resolved=resolve(source,npc);if(graphIds.contains(resolved))facesBySource.merge(source,1,Integer::sum);}
                    for(Map.Entry<Integer,Integer> entry:facesBySource.entrySet()){directModels.add(modelId);routes.add(ordered("npcId",npc.id,"modelId",modelId,"sourceMaterialId",entry.getKey(),"resolvedMaterialId",resolve(entry.getKey(),npc),"faceCount",entry.getValue()));}
                }
            }
            report.put("affectedMaterialGraphIds",new ArrayList<>(graphIds));report.put("affectedTextureGraphs",graphs);report.put("affectedNpcCount",affected.size());report.put("affectedNpcs",affected);report.put("allComponentModelIds",new ArrayList<>(allModels));report.put("directlyAffectedModelIds",new ArrayList<>(directModels));report.put("materialIdsReferencedByAffectedNpcs",new ArrayList<>(allMaterials));report.put("directModelMaterialRoutes",routes);
        }
        report.put("determination",ordered("genuineOperation255",false,"historicalParserDesynchronization",true,"operation0CorrectionApplied",true,"productionOperation0Read","one unsigned byte scaled as (value << 12) / 255","operation17SupportApplied",true,"falseOpcode255Present",false,"mixedCacheRevisions",false,"truncatedOrCorrupt",false,"sentinel",false,"otherNonOperationValue","curve operation cache setting","opcode255ProductionSupportRecommended",false,"material168Operation17Supported",true,"narrowestSafeFollowUp","address remaining blockers independently: combine function 7, curve interpolation 1, and combine function 10"));
        return report;
    }

    static Trace trace(byte[] data,boolean pinnedMonochromeFill){Cursor in=new Cursor(data);int count=in.u8();List<Node> nodes=new ArrayList<>();String stop=null;int colorRoot=-1,alphaRoot=-1,metadataBytes=-1;
        try{for(int index=0;index<count;index++){int start=in.position,descriptor=in.u8(),typeOffset=in.position,type=in.u8(),cacheOffset=in.position,cache=in.u8(),parameterCountOffset=in.position,parameterCount=in.u8();Node node=new Node(index,start,descriptor,typeOffset,type,cacheOffset,cache,parameterCountOffset,parameterCount);nodes.add(node);
                if(type==255){stop="apparent operation 255";break;}if(!framingKnown(type)){stop="framing not modeled for operation "+type;break;}
                int octaves=4;for(int parameter=0;parameter<parameterCount;parameter++){int code=in.u8();if(type==34&&code==1){octaves=in.u8();continue;}skipParameter(in,type,code,pinnedMonochromeFill,octaves);}
                int children=children(type);for(int child=0;child<children;child++)node.children.add(in.u8());node.endOffset=in.position;}
            if(stop==null){colorRoot=in.u8();alphaRoot=in.u8();metadataBytes=data.length-in.position;if(metadataBytes==9)in.skip(9);else if(metadataBytes!=0)stop="expected 0 or 9 trailing material-metadata bytes, found "+metadataBytes;}
        }catch(RuntimeException error){stop=error.getMessage();}
        return new Trace(pinnedMonochromeFill,count,nodes,stop,colorRoot,alphaRoot,metadataBytes,in.position,data.length);
    }

    private static boolean framingKnown(int type){return type==0||type==7||type==8||type==10||type==15||type==17||type==19||type==21||type==34;}
    private static int children(int type){if(type==7)return 2;if(type==8||type==10||type==17)return 1;if(type==19||type==21)return 3;return 0;}
    private static void skipParameter(Cursor in,int type,int code,boolean pinnedFill,int octaves){
        if(type==0){require(code==0,type,code);in.skip(pinnedFill?1:2);return;}
        if(type==7){require(code==0||code==1,type,code);in.skip(1);return;}
        if(type==8){require(code==0,type,code);in.skip(1);int markers=in.u8();in.skip(markers*4);return;}
        if(type==10){require(code==0,type,code);int preset=in.u8();if(preset==0){int samples=in.u8();in.skip(samples*5);}return;}
        if(type==15){require(code>=0&&code<=6,type,code);in.skip(code==2?2:1);return;}
        if(type==17){require(code>=0&&code<=2,type,code);in.skip(code==0?2:1);return;}
        if(type==19){require(code==0||code==1,type,code);in.skip(code==0?2:1);return;}
        if(type==21){require(code==0,type,code);in.skip(1);return;}
        if(type==34){require(code>=0&&code<=6,type,code);if(code==2){int persistence=in.i16();if(persistence<0)in.skip(octaves*2);}else in.skip(1);return;}
        throw new IllegalArgumentException("framing not modeled for operation "+type);
    }
    private static void require(boolean condition,int type,int code){if(!condition)throw new IllegalArgumentException("parameter "+code+" not modeled for operation "+type);}
    private static int resolve(int source,NpcDefinition530 npc){for(int i=0;i<npc.retextureFrom.length;i++)if(source==Short.toUnsignedInt(npc.retextureFrom[i]))return Short.toUnsignedInt(npc.retextureTo[i]);return source;}
    private static List<Integer> byteOffsets(byte[] data,int value){List<Integer> offsets=new ArrayList<>();for(int i=0;i<data.length;i++)if(Byte.toUnsignedInt(data[i])==value)offsets.add(i);return offsets;}
    private static String hex(byte[] data,int from,int to){StringBuilder out=new StringBuilder((to-from)*2);for(int i=from;i<to;i++)out.append(String.format("%02x",data[i]));return out.toString();}
    private static String sha256(byte[] data){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(data);return hex(digest,0,digest.length);}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static Map<String,Object> ordered(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}

    interface Progress{void update(int complete,int total);}
    static final class Trace{final boolean pinned;final int nodeCount;final List<Node> nodes;final String stop;final int colorRoot,alphaRoot,metadataBytes,consumed,total;
        Trace(boolean pinned,int count,List<Node> nodes,String stop,int colorRoot,int alphaRoot,int metadataBytes,int consumed,int total){this.pinned=pinned;nodeCount=count;this.nodes=List.copyOf(nodes);this.stop=stop;this.colorRoot=colorRoot;this.alphaRoot=alphaRoot;this.metadataBytes=metadataBytes;this.consumed=consumed;this.total=total;}
        Map<String,Object> report(){List<Map<String,Object>> nodeReports=new ArrayList<>();for(Node node:nodes)nodeReports.add(node.report());return ordered("operation0ParameterBytes",pinned?1:2,"declaredNodeCount",nodeCount,"nodes",nodeReports,"stopReason",stop,"colorRoot",colorRoot,"alphaRoot",alphaRoot,"trailingMaterialMetadataBytes",metadataBytes,"consumedBytes",consumed,"totalBytes",total,"complete",stop==null&&consumed==total);}
    }
    static final class Node{final int index,startOffset,descriptor,typeOffset,type,cacheOffset,cache,parameterCountOffset,parameterCount;final List<Integer> children=new ArrayList<>();int endOffset=-1;
        Node(int index,int start,int descriptor,int typeOffset,int type,int cacheOffset,int cache,int parameterCountOffset,int parameterCount){this.index=index;startOffset=start;this.descriptor=descriptor;this.typeOffset=typeOffset;this.type=type;this.cacheOffset=cacheOffset;this.cache=cache;this.parameterCountOffset=parameterCountOffset;this.parameterCount=parameterCount;}
        Map<String,Object> report(){return ordered("index",index,"startOffset",startOffset,"descriptor",descriptor,"typeOffset",typeOffset,"type",type,"cacheOffset",cacheOffset,"cache",cache,"parameterCountOffset",parameterCountOffset,"parameterCount",parameterCount,"children",children,"endOffset",endOffset);}
    }
    private static final class Cursor{final byte[] data;int position;Cursor(byte[] data){this.data=data;}int u8(){if(position>=data.length)throw new IllegalArgumentException("truncated at byte "+position);return Byte.toUnsignedInt(data[position++]);}int i16(){int value=(u8()<<8)|u8();return value>32767?value-65536:value;}void skip(int bytes){if(bytes<0||position+bytes>data.length)throw new IllegalArgumentException("truncated at byte "+position+" while skipping "+bytes);position+=bytes;}}
}
