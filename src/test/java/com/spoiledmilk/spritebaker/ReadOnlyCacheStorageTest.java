package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import net.runelite.cache.fs.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadOnlyCacheStorageTest {
    @TempDir Path temp;

    @Test void opensReadOnlyFilesWithoutChangingThemAndRefusesWrites() throws Exception {
        Path data = temp.resolve("main_file_cache.dat2");
        Path index = temp.resolve("main_file_cache.idx255");
        Files.write(data, new byte[] {1, 2, 3});
        Files.write(index, new byte[0]);
        byte[] before = Files.readAllBytes(data);
        if (Files.getFileStore(temp).supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> readOnly = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(data, readOnly);
            Files.setPosixFilePermissions(index, readOnly);
        }

        try (Store store = new Store(new ReadOnlyCacheStorage(temp))) {
            store.load();
            assertThrows(java.io.IOException.class, store::save);
        }
        assertArrayEquals(before, Files.readAllBytes(data));
    }
}
