package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.index.FileData;

/** Lazy NPC definition browser. IDs are indexed without decompressing NPC archives. */
public final class NpcCatalog implements Closeable {
    private final CacheReader cache;private final List<Integer> ids;private NpcCompatibilityScanner compatibility;
    public NpcCatalog(Path cacheDirectory)throws IOException{cache=new CacheReader(cacheDirectory);Index index=cache.store().findIndex(CacheReader.NPC_INDEX);if(index==null)throw new IOException("cache has no NPC index "+CacheReader.NPC_INDEX);List<Integer> found=new ArrayList<>();for(Archive archive:index.getArchives()){FileData[] files=archive.getFileData();if(files==null)continue;for(FileData file:files)found.add(npcId(archive.getArchiveId(),file.getId()));}Collections.sort(found);ids=List.copyOf(found);}
    public int size(){return ids.size();}
    public List<Integer> ids(int offset,int limit){int from=Math.max(0,Math.min(offset,ids.size())),to=Math.min(ids.size(),from+Math.max(0,limit));return ids.subList(from,to);}
    public synchronized NpcCatalogEntry load(int id)throws IOException{NpcDefinition530 npc=cache.loadNpc(id);RenderAnimation530 bas=npc.renderAnimation<0?null:cache.loadRenderAnimation(npc.renderAnimation);return new NpcCatalogEntry(npc,bas);}
    public synchronized NpcCompatibility assess(int id)throws IOException{if(compatibility==null)compatibility=new NpcCompatibilityScanner(cache);return compatibility.assess(id);}
    public synchronized List<NpcCatalogEntry> search(NpcSearchCriteria criteria,int maxResults,Progress progress,Cancellation cancellation)throws IOException{
        if(criteria.isEmpty())return List.of();List<NpcCatalogEntry> out=new ArrayList<>();Integer exact=criteria.exactId();
        if(exact!=null){if(cancellation.cancelled())throw new CancellationException();int position=Collections.binarySearch(ids,exact);if(position>=0)try{NpcCatalogEntry entry=load(exact);if(criteria.matches(entry))out.add(entry);}catch(RuntimeException|IOException ignored){}if(progress!=null)progress.update(1,1);return out;}
        int scanned=0;for(int i=0;i<ids.size()&&out.size()<maxResults;i++){if(cancellation.cancelled()||Thread.currentThread().isInterrupted())throw new CancellationException();try{NpcCatalogEntry entry=load(ids.get(i));if(criteria.matches(entry))out.add(entry);}catch(RuntimeException|IOException ignored){}scanned=i+1;if(progress!=null&&i%128==0)progress.update(scanned,ids.size());}
        if(progress!=null)progress.update(scanned,ids.size());return out;
    }
    public List<NpcCatalogEntry> search(String query,int maxResults,Progress progress)throws IOException{return search(new NpcSearchCriteria(query,NpcSearchCriteria.MatchMode.ALL,Set.of()),maxResults,progress,()->false);}
    @Override public synchronized void close()throws IOException{cache.close();}
    static int npcId(int archiveId,int fileId){return(archiveId<<7)|fileId;}
    @FunctionalInterface public interface Progress{void update(int completed,int total);}
    @FunctionalInterface public interface Cancellation{boolean cancelled();}
}
