package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicPackageTest {
    @Test void publishesCompletePackageAndReplacesOnlyOwnedOutput(@TempDir Path directory)throws Exception{
        Path output=directory.resolve("handoff");try(AtomicPackage first=new AtomicPackage(output)){Files.writeString(first.staging.resolve("value.txt"),"first");first.publish();}assertEquals("first",Files.readString(output.resolve("value.txt")));
        try(AtomicPackage second=new AtomicPackage(output)){Files.writeString(second.staging.resolve("value.txt"),"second");second.publish();}assertEquals("second",Files.readString(output.resolve("value.txt")));assertTrue(Files.isRegularFile(output.resolve(AtomicPackage.MARKER)));
    }
    @Test void failedStageLeavesAcceptedOutputAndRefusesForeignDirectory(@TempDir Path directory)throws Exception{
        Path accepted=directory.resolve("accepted");try(AtomicPackage first=new AtomicPackage(accepted)){Files.writeString(first.staging.resolve("value.txt"),"accepted");first.publish();}try(AtomicPackage abandoned=new AtomicPackage(accepted)){Files.writeString(abandoned.staging.resolve("value.txt"),"rejected");}assertEquals("accepted",Files.readString(accepted.resolve("value.txt")));
        Path foreign=directory.resolve("foreign");Files.createDirectory(foreign);Files.writeString(foreign.resolve("keep.txt"),"user");assertThrows(java.io.IOException.class,()->new AtomicPackage(foreign));assertEquals("user",Files.readString(foreign.resolve("keep.txt")));
    }
}
