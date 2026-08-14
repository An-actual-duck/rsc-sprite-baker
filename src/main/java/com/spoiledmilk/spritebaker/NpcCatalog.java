package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.index.FileData;

/** Lazy NPC definition browser. IDs are indexed without decompressing NPC archives. */
public final class NpcCatalog implements Closeable {
    private final CacheReader cache;private final List<Integer> ids;
    public NpcCatalog(Path cacheDirectory)throws IOException{cache=new CacheReader(cacheDirectory);Index index=cache.store().findIndex(CacheReader.NPC_INDEX);if(index==null)throw new IOException("cache has no NPC index "+CacheReader.NPC_INDEX);List<Integer> found=new ArrayList<>();for(Archive archive:index.getArchives()){FileData[] files=archive.getFileData();if(files==null)continue;for(FileData file:files)found.add(npcId(archive.getArchiveId(),file.getId()));}Collections.sort(found);ids=List.copyOf(found);}
    public int size(){return ids.size();}
    public List<Integer> ids(int offset,int limit){int from=Math.max(0,Math.min(offset,ids.size())),to=Math.min(ids.size(),from+Math.max(0,limit));return ids.subList(from,to);}
    public NpcCatalogEntry load(int id)throws IOException{return new NpcCatalogEntry(cache.loadNpc(id));}
    public List<NpcCatalogEntry> search(String query,int maxResults,Progress progress)throws IOException{String needle=query.trim().toLowerCase(Locale.ROOT);List<NpcCatalogEntry> out=new ArrayList<>();Integer exact=null;try{exact=Integer.valueOf(needle);}catch(NumberFormatException ignored){}for(int i=0;i<ids.size()&&out.size()<maxResults;i++){int id=ids.get(i);if(exact!=null&&id!=exact){if(id>exact)break;continue;}try{NpcCatalogEntry entry=load(id);if(exact!=null||entry.name.toLowerCase(Locale.ROOT).contains(needle))out.add(entry);}catch(RuntimeException|IOException ignored){}if(progress!=null&&i%128==0)progress.update(i,ids.size());if(exact!=null)break;}if(progress!=null)progress.update(ids.size(),ids.size());return out;}
    @Override public void close()throws IOException{cache.close();}
    static int npcId(int archiveId,int fileId){return(archiveId<<7)|fileId;}
    @FunctionalInterface public interface Progress{void update(int completed,int total);}
}
