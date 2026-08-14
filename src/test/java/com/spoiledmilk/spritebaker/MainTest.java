package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void rejectsOutputInsideCache() {
        assertThrows(IllegalArgumentException.class, () -> Main.enforceOutputBoundary(
            Path.of("/inputs/cache/output"), Path.of("/inputs/cache"), Path.of("/checkout")));
    }

    @Test
    void acceptsSeparateOutputDirectory() {
        assertDoesNotThrow(() -> Main.enforceOutputBoundary(
            Path.of("/tmp/output"), Path.of("/inputs/cache"), Path.of("/checkout")));
    }
}
