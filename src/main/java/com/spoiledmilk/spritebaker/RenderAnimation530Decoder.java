package com.spoiledmilk.spritebaker;

import java.nio.ByteBuffer;

/** Strict revision-530 BAS/render-animation metadata decoder. */
public final class RenderAnimation530Decoder {
    public RenderAnimation530 decode(int id, byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data);
        RenderAnimation530 result = new RenderAnimation530(id);
        while (in.hasRemaining()) {
            int offset = in.position();
            int opcode = u8(in);
            if (opcode == 0) {
                return result;
            }
            if (opcode == 1) {
                result.standingAnimation = nullableU16(in);
                result.walkingAnimation = nullableU16(in);
            } else if (opcode == 6) {
                result.runningAnimation = nullableU16(in);
            } else if (opcode >= 2 && opcode <= 9) {
                skip(in, 2);
            } else if (opcode == 26) {
                skip(in, 2);
            } else if (opcode == 27) {
                skip(in, 13);
            } else if (opcode == 28) {
                skip(in, 12);
            } else if (opcode == 29 || opcode == 31 || opcode == 34 || opcode == 37) {
                skip(in, 1);
            } else if ((opcode >= 30 && opcode <= 36) || (opcode >= 38 && opcode <= 51)) {
                skip(in, 2);
            } else if (opcode == 52) {
                int count = u8(in);
                skip(in, count * 3);
            } else if (opcode == 53) {
                // flag only
            } else if (opcode == 54) {
                skip(in, 2);
            } else if (opcode == 55) {
                skip(in, 3);
            } else if (opcode == 56) {
                skip(in, 7);
            } else {
                throw new UnsupportedOperationException(
                    "unsupported revision-530 render-animation opcode " + opcode + " at byte " + offset);
            }
        }
        throw new IllegalArgumentException("render animation " + id + " has no terminating opcode");
    }

    private static int nullableU16(ByteBuffer in) {
        int value = Short.toUnsignedInt(in.getShort());
        return value == 65535 ? -1 : value;
    }

    private static int u8(ByteBuffer in) {
        return Byte.toUnsignedInt(in.get());
    }

    private static void skip(ByteBuffer in, int count) {
        in.position(in.position() + count);
    }
}
