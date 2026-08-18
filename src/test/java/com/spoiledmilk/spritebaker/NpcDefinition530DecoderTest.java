package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.jupiter.api.Test;

class NpcDefinition530DecoderTest {
    @Test
    void decodesGeneratedNeutralDefinition() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(127); out.writeShort(42);
            out.writeByte(1); out.writeByte(2); out.writeShort(100); out.writeShort(101);
            out.writeByte(2); out.writeBytes("Fixture"); out.writeByte(0);
            out.writeByte(40); out.writeByte(1); out.writeShort(500); out.writeShort(600);
            out.writeByte(41); out.writeByte(1); out.writeShort(7); out.writeShort(8);
            out.writeByte(42); out.writeByte(2); out.writeByte(-1); out.writeByte(3);
            out.writeByte(97); out.writeShort(96);
            out.writeByte(98); out.writeShort(144);
            out.writeByte(100); out.writeByte(-4);
            out.writeByte(101); out.writeByte(3);
            out.writeByte(0);
        }

        NpcDefinition530 result = new NpcDefinition530Decoder().decode(123, bytes.toByteArray());

        assertEquals(123, result.id);
        assertEquals("Fixture", result.name);
        assertArrayEquals(new int[] {100, 101}, result.modelIds);
        assertArrayEquals(new short[] {500}, result.recolorFrom);
        assertArrayEquals(new short[] {600}, result.recolorTo);
        assertArrayEquals(new short[] {7}, result.retextureFrom);
        assertArrayEquals(new short[] {8}, result.retextureTo);
        assertArrayEquals(new byte[] {-1, 3}, result.recolorPaletteIndices);
        assertEquals(42, result.renderAnimation);
        assertEquals(96, result.widthScale);
        assertEquals(144, result.heightScale);
        assertEquals(-4, result.ambient);
        assertEquals(15, result.contrast);
    }

    @Test
    void rejectsUnknownOpcodeWithoutDesynchronizing() {
        UnsupportedOperationException error = assertThrows(
            UnsupportedOperationException.class,
            () -> new NpcDefinition530Decoder().decode(9, new byte[] {(byte) 200, 0}));
        assertEquals("unsupported revision-530 NPC opcode 200 at byte 0", error.getMessage());
    }

    @Test
    void decodesCoverMarkerAndPreservesUnsignedSentinel() {
        NpcDefinition530 result = new NpcDefinition530Decoder().decode(1688, new byte[] {
            (byte) 138, (byte) 0xff, (byte) 0xff,
            (byte) 97, 0, 96,
            0
        });

        assertEquals(65535, result.coverMarker);
        assertEquals(96, result.widthScale);
    }

    @Test
    void decodesNpc1688TrailingMetadataAtExactBoundaries() {
        NpcDefinition530 result = new NpcDefinition530Decoder().decode(1688, new byte[] {
            (byte) 137, (byte) 0xff, (byte) 0xff,
            (byte) 138, (byte) 0xff, (byte) 0xff,
            (byte) 159,
            (byte) 165, (byte) 0xff,
            0
        });

        assertEquals(65535, result.coverMarker);
        assertEquals(0, result.attackOptionPriority);
        assertEquals(255, result.pickSizeShift);
    }

    @Test
    void keepsOpcode138TruncationFailClosed() {
        assertThrows(BufferUnderflowException.class,
            () -> new NpcDefinition530Decoder().decode(1688,
                new byte[] {(byte) 138, 1}));
    }

    @Test
    void keepsOpcode165TruncationFailClosed() {
        assertThrows(BufferUnderflowException.class,
            () -> new NpcDefinition530Decoder().decode(1688,
                new byte[] {(byte) 165}));
    }
}
