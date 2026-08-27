package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;
import net.runelite.cache.index.FileData;

/** Read-only access to the revision-530 archive mappings used by 2009scape. */
public final class CacheReader implements Closeable {
    public static final int MODEL_INDEX = 7;
    public static final int NPC_INDEX = 18;
    public static final int FRAME_INDEX = 0;
    public static final int FRAMEMAP_INDEX = 1;
    public static final int SEQUENCE_INDEX = 20;

    private final Store store;
    private final NpcDefinition530Decoder npcDecoder = new NpcDefinition530Decoder();
    private final ModelLoader modelLoader = new ModelLoader();
    private final Revision530Type1ModelDecoder revision530Type1ModelDecoder = new Revision530Type1ModelDecoder();
    private final RenderAnimation530Decoder renderAnimationDecoder = new RenderAnimation530Decoder();
    private final Sequence530Decoder sequenceDecoder = new Sequence530Decoder();
    private final Frame530Decoder frameDecoder = new Frame530Decoder();
    private final Framemap530Decoder framemapDecoder = new Framemap530Decoder();

    public CacheReader(Path cacheDirectory) throws IOException {
        this.store = new Store(new ReadOnlyCacheStorage(cacheDirectory));
        this.store.load();
    }

    public Store store() {
        return store;
    }

    public NpcDefinition530 loadNpc(int id) throws IOException {
        return npcDecoder.decode(id, loadFile(NPC_INDEX, id >>> 7, id & 0x7f));
    }

    public ModelDefinition loadModel(int id) throws IOException {
        byte[] data = loadFile(MODEL_INDEX, id, 0);
        if (revision530Type1ModelDecoder.matches(data)) {
            return revision530Type1ModelDecoder.decode(id, data);
        }
        return modelLoader.load(id, data);
    }

    public RenderAnimation530 loadRenderAnimation(int id) throws IOException {
        return renderAnimationDecoder.decode(id, loadFile(2, 32, id));
    }

    public Sequence530 loadSequence(int id) throws IOException {
        return sequenceDecoder.decode(id, loadFile(SEQUENCE_INDEX, id >>> 7, id & 0x7f));
    }

    /** Sequence IDs in the anchor's JS5 group and its immediate neighbours (at most 384 IDs). */
    public List<Integer> relatedSequenceIds(int... anchors) {
        Index index=store.findIndex(SEQUENCE_INDEX);if(index==null)return List.of();
        java.util.Set<Integer> groups=new java.util.TreeSet<>();
        for(int anchor:anchors)if(anchor>=0){int group=anchor>>>7;for(int delta=-1;delta<=1;delta++)if(group+delta>=0)groups.add(group+delta);}
        List<Integer> out=new ArrayList<>();
        for(int group:groups){Archive archive=index.getArchive(group);if(archive==null)continue;for(FileData file:archive.getFileData())out.add((group<<7)|file.getId());}
        return List.copyOf(out);
    }

    public Frame530 loadFrame(int packedFrameId) throws IOException {
        int frameSetId = packedFrameId >>> 16;
        int frameId = packedFrameId & 0xffff;
        byte[] data = loadFile(FRAME_INDEX, frameSetId, frameId);
        int framemapId = (Byte.toUnsignedInt(data[0]) << 8) | Byte.toUnsignedInt(data[1]);
        Framemap530 framemap = framemapDecoder.decode(
            framemapId, loadFile(FRAMEMAP_INDEX, framemapId, 0));
        return frameDecoder.decode(packedFrameId, framemap, data);
    }

    public byte[] loadFile(int indexId, int archiveId, int fileId) throws IOException {
        Index index = store.findIndex(indexId);
        if (index == null) {
            throw new IOException("cache has no index " + indexId);
        }
        Archive archive = index.getArchive(archiveId);
        if (archive == null) {
            throw new IOException("cache has no archive " + indexId + "/" + archiveId);
        }
        byte[] packed = store.getStorage().loadArchive(archive);
        FSFile file = archive.getFiles(packed).findFile(fileId);
        if (file == null) {
            throw new IOException("cache has no file " + indexId + "/" + archiveId + "/" + fileId);
        }
        return file.getContents();
    }

    @Override
    public void close() throws IOException {
        store.close();
    }
}
