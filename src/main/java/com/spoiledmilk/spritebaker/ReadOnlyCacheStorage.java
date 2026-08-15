package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Container;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.index.ArchiveData;
import net.runelite.cache.index.IndexData;

/** Strictly read-only access to a Jagex JS5 disk cache. */
final class ReadOnlyCacheStorage implements Storage {
    private static final int INDEX_ENTRY_BYTES = 6;
    private static final int SECTOR_BYTES = 520;

    private final Path directory;
    private final FileChannel data;
    private final FileChannel referenceIndex;
    private final List<FileChannel> indexes = new ArrayList<>();

    ReadOnlyCacheStorage(Path directory) throws IOException {
        this.directory = directory.toRealPath();
        data = FileChannel.open(this.directory.resolve("main_file_cache.dat2"), StandardOpenOption.READ);
        referenceIndex = FileChannel.open(this.directory.resolve("main_file_cache.idx255"), StandardOpenOption.READ);
    }

    @Override
    public void init(Store store) throws IOException {
        int count = Math.toIntExact(referenceIndex.size() / INDEX_ENTRY_BYTES);
        try {
            for (int id = 0; id < count; id++) {
                store.addIndex(id);
                indexes.add(FileChannel.open(directory.resolve("main_file_cache.idx" + id), StandardOpenOption.READ));
            }
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    @Override
    public void load(Store store) throws IOException {
        for (Index index : store.getIndexes()) {
            byte[] packed = readArchive(referenceIndex, 255, index.getId());
            if (packed == null) continue;
            Container container = Container.decompress(packed, null);
            IndexData source = new IndexData();
            source.load(container.data);
            index.setProtocol(source.getProtocol());
            index.setRevision(source.getRevision());
            index.setNamed(source.isNamed());
            index.setSized(source.isSized());
            for (ArchiveData sourceArchive : source.getArchives()) {
                Archive archive = index.addArchive(sourceArchive.getId());
                archive.setNameHash(sourceArchive.getNameHash());
                archive.setCrc(sourceArchive.getCrc());
                archive.setCompressedSize(sourceArchive.getCompressedSize());
                archive.setDecompressedSize(sourceArchive.getDecompressedSize());
                archive.setRevision(sourceArchive.getRevision());
                archive.setFileData(sourceArchive.getFiles());
            }
            index.setCrc(container.crc);
            index.setCompression(container.compression);
        }
    }

    @Override
    public byte[] load(int index, int archive) throws IOException {
        if (index < 0 || index >= indexes.size()) return null;
        return readArchive(indexes.get(index), index, archive);
    }

    private byte[] readArchive(FileChannel index, int indexId, int archiveId) throws IOException {
        ByteBuffer entry = ByteBuffer.allocate(INDEX_ENTRY_BYTES);
        if (!readFully(index, entry, (long) archiveId * INDEX_ENTRY_BYTES)) return null;
        entry.flip();
        int length = unsignedMedium(entry);
        int sector = unsignedMedium(entry);
        if (length <= 0 || sector <= 0 || (long) sector * SECTOR_BYTES >= data.size()) return null;

        ByteBuffer output = ByteBuffer.allocate(length);
        int part = 0;
        while (output.hasRemaining()) {
            int headerBytes = archiveId > 0xffff ? 10 : 8;
            int payloadBytes = Math.min(output.remaining(), SECTOR_BYTES - headerBytes);
            ByteBuffer block = ByteBuffer.allocate(headerBytes + payloadBytes);
            if (!readFully(data, block, (long) sector * SECTOR_BYTES)) return null;
            block.flip();

            int foundArchive = archiveId > 0xffff ? block.getInt() : Short.toUnsignedInt(block.getShort());
            int foundPart = Short.toUnsignedInt(block.getShort());
            int nextSector = unsignedMedium(block);
            int foundIndex = Byte.toUnsignedInt(block.get());
            if (foundArchive != archiveId || foundPart != part || foundIndex != indexId) return null;
            if (nextSector < 0 || (nextSector != 0 && (long) nextSector * SECTOR_BYTES >= data.size())) return null;
            output.put(block);
            sector = nextSector;
            part++;
            if (output.hasRemaining() && sector == 0) return null;
        }
        return output.array();
    }

    private static boolean readFully(FileChannel channel, ByteBuffer target, long position) throws IOException {
        while (target.hasRemaining()) {
            int read = channel.read(target, position);
            if (read < 0) return false;
            if (read == 0) return false;
            position += read;
        }
        return true;
    }

    private static int unsignedMedium(ByteBuffer source) {
        return (Byte.toUnsignedInt(source.get()) << 16)
            | (Byte.toUnsignedInt(source.get()) << 8)
            | Byte.toUnsignedInt(source.get());
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (FileChannel index : indexes) {
            try { index.close(); } catch (IOException e) { failure = append(failure, e); }
        }
        try { referenceIndex.close(); } catch (IOException e) { failure = append(failure, e); }
        try { data.close(); } catch (IOException e) { failure = append(failure, e); }
        if (failure != null) throw failure;
    }

    private static IOException append(IOException first, IOException next) {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
    }

    @Override
    public void save(Store store) throws IOException {
        throw new IOException("Sprite Baker cache access is read-only");
    }

    @Override
    public void store(int index, int archive, byte[] data) throws IOException {
        throw new IOException("Sprite Baker cache access is read-only");
    }
}
