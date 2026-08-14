package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable identity of the two files that define the selected JS5 cache store. */
final class CacheIdentity {
    final Path directory;final long dataFileBytes,referenceIndexBytes;final String dataFileSha256,referenceIndexSha256;
    private CacheIdentity(Path directory,long dataBytes,String dataHash,long indexBytes,String indexHash){this.directory=directory;dataFileBytes=dataBytes;dataFileSha256=dataHash;referenceIndexBytes=indexBytes;referenceIndexSha256=indexHash;}
    static CacheIdentity read(Path cache)throws IOException{
        Path directory=DesktopWorkflow.validateCache(cache),data=directory.resolve("main_file_cache.dat2"),index=directory.resolve("main_file_cache.idx255");
        return new CacheIdentity(directory,Files.size(data),Hashing.sha256(data),Files.size(index),Hashing.sha256(index));
    }
    Map<String,Object> report(){Map<String,Object> out=new LinkedHashMap<>();out.put("layout","JS5 dat2 with idx files");out.put("dataFile",file("main_file_cache.dat2",dataFileBytes,dataFileSha256));out.put("referenceIndex",file("main_file_cache.idx255",referenceIndexBytes,referenceIndexSha256));return out;}
    private static Map<String,Object> file(String name,long bytes,String sha){Map<String,Object> out=new LinkedHashMap<>();out.put("name",name);out.put("bytes",bytes);out.put("sha256",sha);return out;}
}
