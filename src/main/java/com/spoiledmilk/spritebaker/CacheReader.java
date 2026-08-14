package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

/** Read-only access to the revision-530 archive mappings used by 2009scape. */
public final class CacheReader implements Closeable {
    public static final int MODEL_INDEX = 7;
    public static final int NPC_INDEX = 18;

    private final Store store;
    private final NpcDefinition530Decoder npcDecoder = new NpcDefinition530Decoder();
    private final ModelLoader modelLoader = new ModelLoader();
    private final RenderAnimation530Decoder renderAnimationDecoder = new RenderAnimation530Decoder();

    public CacheReader(Path cacheDirectory) throws IOException {
        this.store = new Store(cacheDirectory.toFile());
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
        return modelLoader.load(id, data);
    }

    public RenderAnimation530 loadRenderAnimation(int id) throws IOException {
        return renderAnimationDecoder.decode(id, loadFile(2, 32, id));
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
