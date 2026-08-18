package com.spoiledmilk.spritebaker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Strict decoder for the revision-530 NPC opcodes needed by the compatibility
 * spike. Unknown opcodes fail with their byte offset instead of desynchronizing.
 */
public final class NpcDefinition530Decoder {
    public NpcDefinition530 decode(int id, byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data);
        NpcDefinition530 npc = new NpcDefinition530(id);
        while (in.hasRemaining()) {
            int offset = in.position();
            int opcode = u8(in);
            if (opcode == 0) {
                return npc;
            }
            decodeOpcode(npc, opcode, in, offset);
        }
        throw new IllegalArgumentException("NPC " + id + " has no terminating opcode");
    }

    private void decodeOpcode(NpcDefinition530 npc, int opcode, ByteBuffer in, int offset) {
        switch (opcode) {
            case 1:
                npc.modelIds = unsignedShortArray(in);
                break;
            case 2:
                npc.name = string(in);
                break;
            case 12:
                u8(in);
                break;
            case 13:
                npc.standingAnimation = nullableUnsignedShort(in);
                break;
            case 14:
                npc.walkingAnimation = nullableUnsignedShort(in);
                break;
            case 15:
            case 16:
                u16(in);
                break;
            case 17:
                npc.walkingAnimation = nullableUnsignedShort(in);
                skip(in, 6);
                break;
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
                string(in);
                break;
            case 40:
                int recolors = u8(in);
                npc.recolorFrom = new short[recolors];
                npc.recolorTo = new short[recolors];
                for (int i = 0; i < recolors; i++) {
                    npc.recolorFrom[i] = (short) u16(in);
                    npc.recolorTo[i] = (short) u16(in);
                }
                break;
            case 41:
                int retextures = u8(in);
                npc.retextureFrom = new short[retextures];
                npc.retextureTo = new short[retextures];
                for (int i = 0; i < retextures; i++) {
                    npc.retextureFrom[i] = (short) u16(in);
                    npc.retextureTo[i] = (short) u16(in);
                }
                break;
            case 42:
                npc.recolorPaletteIndices = byteArray(in);
                break;
            case 60:
                unsignedShortArray(in);
                break;
            case 93:
            case 99:
            case 107:
            case 109:
            case 111:
                break;
            case 113:
                skip(in, 4);
                break;
            case 114:
            case 115:
                skip(in, 2);
                break;
            case 119:
            case 125:
            case 128:
                skip(in, 1);
                break;
            case 121:
                skip(in, u8(in) * 4);
                break;
            case 122:
            case 123:
            case 126:
            case 137:
                skip(in, 2);
                break;
            case 138:
                // Introduced immediately after the revision-530 format: unlike
                // nullable definition IDs, the client preserves unsigned 65535.
                npc.coverMarker = u16(in);
                break;
            case 127:
                npc.renderAnimation = nullableUnsignedShort(in);
                break;
            case 134:
                skip(in, 9);
                break;
            case 135:
            case 136:
                skip(in, 3);
                break;
            case 159:
                npc.attackOptionPriority = 0;
                break;
            case 165:
                npc.pickSizeShift = u8(in);
                break;
            case 95:
            case 102:
            case 103:
                u16(in);
                break;
            case 97:
                npc.widthScale = u16(in);
                break;
            case 98:
                npc.heightScale = u16(in);
                break;
            case 100:
                npc.ambient = in.get();
                break;
            case 101:
                npc.contrast = in.get() * 5;
                break;
            case 106:
            case 118:
                npc.morphDefinition = true;
                skipTransforms(in, opcode == 118);
                break;
            case 249:
                skipParams(in);
                break;
            default:
                throw new UnsupportedOperationException(
                    "unsupported revision-530 NPC opcode " + opcode + " at byte " + offset);
        }
    }

    private static void skipTransforms(ByteBuffer in, boolean hasFallback) {
        skip(in, 4);
        if (hasFallback) {
            skip(in, 2);
        }
        int count = u8(in);
        skip(in, (count + 1) * 2);
    }

    private static void skipParams(ByteBuffer in) {
        int count = u8(in);
        for (int i = 0; i < count; i++) {
            boolean string = u8(in) == 1;
            skip(in, 3);
            if (string) {
                string(in);
            } else {
                skip(in, 4);
            }
        }
    }

    private static int[] unsignedShortArray(ByteBuffer in) {
        int[] values = new int[u8(in)];
        for (int i = 0; i < values.length; i++) {
            values[i] = nullableUnsignedShort(in);
        }
        return values;
    }

    private static byte[] byteArray(ByteBuffer in) {
        byte[] values = new byte[u8(in)];
        in.get(values);
        return values;
    }

    private static String string(ByteBuffer in) {
        int start = in.position();
        while (in.get() != 0) {
            // null terminated
        }
        int end = in.position() - 1;
        byte[] value = new byte[end - start];
        int after = in.position();
        in.position(start);
        in.get(value);
        in.position(after);
        return new String(value, StandardCharsets.ISO_8859_1);
    }

    private static int nullableUnsignedShort(ByteBuffer in) {
        int value = u16(in);
        return value == 65535 ? -1 : value;
    }

    private static int u8(ByteBuffer in) {
        return Byte.toUnsignedInt(in.get());
    }

    private static int u16(ByteBuffer in) {
        return Short.toUnsignedInt(in.getShort());
    }

    private static void skip(ByteBuffer in, int count) {
        in.position(in.position() + count);
    }
}
