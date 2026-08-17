package com.spoiledmilk.spritebaker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only structural tracing for model bytes; never produces renderable geometry. */
final class ModelFormatDiagnostic {
    static final String ROOT_CAUSE = "RuneLite type-1 layout assigns two extra bytes to every complex texture face";

    private ModelFormatDiagnostic(){}

    static Map<String,Object> analyze(int modelId,byte[] data){
        Map<String,Object> out=ordered("modelId",modelId,"bytes",data.length,"sha256",sha256(data),
            "footerWindowHex",hex(data,Math.max(0,data.length-32),data.length));
        if(data.length<2){out.put("format","unselectable");out.put("failure",failure("format-selection","footer",0,2,data.length));return out;}
        int penultimate=data[data.length-2]&255,last=data[data.length-1]&255;
        String format=penultimate==255&&last==253?"type-3":penultimate==255&&last==254?"type-2":penultimate==255&&last==255?"type-1":"old";
        int footerSize="type-3".equals(format)?26:("old".equals(format)?18:23);
        out.put("format",format);out.put("revisionMarker",String.format("%02x%02x",penultimate,last));out.put("footerSize",footerSize);out.put("footerBytesHex",data.length>=footerSize?hex(data,data.length-footerSize,data.length):hex(data,0,data.length));
        if(!"type-1".equals(format)){out.put("diagnosticScope","format selected by dependency; detailed revision-530 comparison applies to type-1 only");return out;}
        if(data.length<footerSize){out.put("failure",failure("footer","footer",0,footerSize,data.length));return out;}
        int footer=data.length-footerSize,p=footer;
        int vertices=u16(data,p);p+=2;int faces=u16(data,p);p+=2;int textures=u8(data,p++),info=u8(data,p++),priority=u8(data,p++),alpha=u8(data,p++),faceBones=u8(data,p++),faceTextures=u8(data,p++),vertexBones=u8(data,p++);
        int dx=u16(data,p);p+=2;int dy=u16(data,p);p+=2;int dz=u16(data,p);p+=2;int faceIndices=u16(data,p);p+=2;int textureCoords=u16(data,p);
        int simple=0,complex=0,cube=0,unknown=0;
        for(int i=0;i<textures;i++){if(i>=data.length){out.put("failure",failure("texture-render-types","texture-types",i,1,0));return out;}int type=u8(data,i);if(type==0)simple++;else if(type>=1&&type<=3)complex++;else unknown++;if(type==2)cube++;}
        Map<String,Object> counts=ordered("vertices",vertices,"faces",faces,"textureFaces",textures,"simpleTextureFaces",simple,"complexTextureFaces",complex,"cubeTextureFaces",cube,"unknownTextureTypes",unknown);
        Map<String,Object> flags=ordered("rawInfo",info,"triangleInfo",(info&1)!=0,"particleExtension",(info&2)!=0,"priority",priority,"alpha",alpha,"faceBones",faceBones,"faceTextures",faceTextures,"vertexBones",vertexBones);
        Map<String,Object> lengths=ordered("vertexX",dx,"vertexY",dy,"vertexZ",dz,"faceIndices",faceIndices,"textureCoordinates",textureCoords);
        out.put("counts",counts);out.put("flags",flags);out.put("serializedLengths",lengths);out.put("footerOffset",footer);
        long common=(long)textures+vertices+((info&1)!=0?faces:0)+faces+(priority==255?faces:0)+(faceBones==1?faces:0)+(vertexBones==1?vertices:0)+(alpha==1?faces:0)+faceIndices+(faceTextures==1?(long)faces*2:0)+textureCoords+(long)faces*2+dx+dy+dz+(long)simple*6+(long)complex*6+(long)complex*6;
        long pinnedEnd=common+complex+complex+complex+(long)cube*2;
        long dependencyEnd=common+(long)complex*2+complex+(long)complex*2+(long)cube*2;
        out.put("pinnedRevision530",ordered("calculatedDataEnd",pinnedEnd,"footerOffset",footer,"layoutMatchesFooter",pinnedEnd==footer,"complexRotationBytesPerFace",1,"complexAuxiliaryBytesPerFace",2));
        out.put("dependencyType1",ordered("calculatedDataEnd",dependencyEnd,"footerOffset",footer,"dataEndMinusFooter",dependencyEnd-footer,"complexRotationBytesPerFace",2,"complexAuxiliaryBytesPerFace",3));
        Map<String,Object> trace=traceExtension(data,dependencyEnd);
        out.put("dependencyExtensionTrace",trace);
        if(trace.get("failure")!=null){out.put("failure",trace.get("failure"));out.put("structuralSignature",signature(complex,(Map<String,Object>)trace.get("failure")));out.put("likelyRootCause",ROOT_CAUSE);}
        return out;
    }

    private static Map<String,Object> traceExtension(byte[] data,long rawOffset){
        Map<String,Object> trace=new LinkedHashMap<>();trace.put("stream","dependency-extension");trace.put("startOffset",rawOffset);
        if(rawOffset<0||rawOffset>Integer.MAX_VALUE){trace.put("failure",failure("extension-flag","dependency-extension",Integer.MAX_VALUE,1,0));return trace;}
        int offset=(int)rawOffset;
        FailureRead flag=read(data,offset,1,"extension-flag");if(flag.failure!=null){trace.put("failure",flag.failure);return trace;}int marker=u8(data,offset);trace.put("marker",marker);offset++;
        if(marker==0){trace.put("complete",true);trace.put("endOffset",offset);return trace;}
        for(int field=1;field<=3;field++){FailureRead value=read(data,offset,2,"extension-metadata-u16-"+field);if(value.failure!=null){trace.put("failure",value.failure);return trace;}offset+=2;}
        FailureRead integer=read(data,offset,4,"extension-metadata-i32");if(integer.failure!=null){trace.put("failure",integer.failure);return trace;}offset+=4;trace.put("complete",true);trace.put("endOffset",offset);return trace;
    }

    private static FailureRead read(byte[] data,int offset,int count,String stage){int remaining=Math.max(0,data.length-Math.max(0,offset));return offset<0||offset>data.length||remaining<count?new FailureRead(failure(stage,"dependency-extension",offset,count,remaining)):new FailureRead(null);}
    private static Map<String,Object> failure(String stage,String stream,int offset,int requested,int remaining){return ordered("stage",stage,"stream",stream,"offset",offset,"requestedBytes",requested,"remainingBytes",remaining);}
    private static String signature(int complex,Map<String,Object> failure){return "type-1|complex="+complex+"|dependency-extra="+(complex*2)+"|"+failure.get("stage")+"|requested="+failure.get("requestedBytes")+"|remaining="+failure.get("remainingBytes");}
    private static int u8(byte[] data,int offset){return data[offset]&255;}
    private static int u16(byte[] data,int offset){return u8(data,offset)<<8|u8(data,offset+1);}
    private static String sha256(byte[] data){try{return hex(MessageDigest.getInstance("SHA-256").digest(data),0,32);}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    static String hex(byte[] data,int from,int to){StringBuilder out=new StringBuilder(Math.max(0,to-from)*2);for(int i=from;i<to;i++)out.append(String.format("%02x",data[i]&255));return out.toString();}
    static Map<String,Object> ordered(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    private static final class FailureRead{final Map<String,Object> failure;FailureRead(Map<String,Object> failure){this.failure=failure;}}
}
