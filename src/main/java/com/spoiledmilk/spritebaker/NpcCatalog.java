package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    public synchronized NpcCatalogEntry load(int id)throws IOException{return new NpcCatalogEntry(cache.loadNpc(id));}
    public synchronized NpcCompatibility assess(int id)throws IOException{if(compatibility==null)compatibility=new NpcCompatibilityScanner(cache);return compatibility.assess(id);}
    public synchronized List<NpcCatalogEntry> search(String query,int maxResults,Progress progress,Cancellation cancellation)throws IOException{
        String text=query==null?"":query.trim();if(text.isEmpty())return List.of();List<NpcCatalogEntry> out=new ArrayList<>();Integer exact=exactId(text);
        if(exact!=null){checkCancelled(cancellation);int position=Collections.binarySearch(ids,exact);if(position>=0)try{out.add(load(exact));}catch(RuntimeException|IOException ignored){}if(progress!=null)progress.update(1,1);return out;}
        String needle=text.toLowerCase(Locale.ROOT);int scanned=0;
        for(int i=0;i<ids.size()&&out.size()<maxResults;i++){checkCancelled(cancellation);try{NpcCatalogEntry entry=load(ids.get(i));if(entry.name.toLowerCase(Locale.ROOT).contains(needle))out.add(entry);}catch(RuntimeException|IOException ignored){}scanned=i+1;if(progress!=null&&i%128==0)progress.update(scanned,ids.size());}
        if(progress!=null)progress.update(scanned,ids.size());return out;
    }
    public List<NpcCatalogEntry> search(String query,int maxResults,Progress progress)throws IOException{return search(query,maxResults,progress,()->false);}
    @Override public synchronized void close()throws IOException{cache.close();}
    static Integer exactId(String query){if(query==null||!query.trim().matches("[0-9]+"))return null;try{return Integer.valueOf(query.trim());}catch(NumberFormatException ignored){return null;}}
    static int npcId(int archiveId,int fileId){return(archiveId<<7)|fileId;}
    private static void checkCancelled(Cancellation cancellation){if(cancellation.cancelled()||Thread.currentThread().isInterrupted())throw new CancellationException();}
    @FunctionalInterface public interface Progress{void update(int completed,int total);}
    @FunctionalInterface public interface Cancellation{boolean cancelled();}
}
