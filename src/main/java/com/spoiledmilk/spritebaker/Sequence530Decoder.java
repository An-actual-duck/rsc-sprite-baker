package com.spoiledmilk.spritebaker;

/** Strict decoder for the sequence opcodes used by the January 2009 revision-530 client. */
public final class Sequence530Decoder {
    public Sequence530 decode(int id, byte[] data) {
        Sequence530 out = new Sequence530(id);
        BinaryInput in = new BinaryInput(data);
        while (true) {
            int opcode = in.u8();
            if (opcode == 0) break;
            switch (opcode) {
                case 1:
                    int count = in.u16();
                    out.durations = new int[count];
                    out.frameIds = new int[count];
                    for (int i = 0; i < count; i++) out.durations[i] = in.u16();
                    for (int i = 0; i < count; i++) out.frameIds[i] = in.u16();
                    for (int i = 0; i < count; i++) out.frameIds[i] |= in.u16() << 16;
                    break;
                case 2: out.loopOffset = in.u16(); break;
                case 3:
                    out.interleave = new boolean[256];
                    int masks = in.u8();
                    for (int i = 0; i < masks; i++) out.interleave[in.u8()] = true;
                    break;
                case 4: out.stretches = true; break;
                case 5: out.priority = in.u8(); break;
                case 6: out.rightHandItem = in.u16(); break;
                case 7: out.leftHandItem = in.u16(); break;
                case 8: out.maxLoops = in.u8(); break;
                case 9: out.precedence = in.u8(); break;
                case 10: out.walkingPrecedence = in.u8(); break;
                case 11: out.replayMode = in.u8(); break;
                case 12:
                    int secondaryCount = in.u8();
                    out.secondaryFrameIds = new int[secondaryCount];
                    for (int i = 0; i < secondaryCount; i++) out.secondaryFrameIds[i] = in.u16();
                    for (int i = 0; i < secondaryCount; i++) out.secondaryFrameIds[i] |= in.u16() << 16;
                    break;
                case 13:
                    int soundCount = in.u8();
                    for (int i = 0; i < soundCount; i++) in.u24();
                    break;
                case 14: out.special = true; break;
                case 15: out.tween = true; break;
                case 16: in.i8(); break;
                default: throw new IllegalArgumentException("sequence " + id + " has unsupported opcode " + opcode);
            }
        }
        if (in.remaining() != 0) throw new IllegalArgumentException("sequence " + id + " has trailing bytes");
        if (out.precedence == -1) out.precedence = out.interleave == null ? 0 : 2;
        if (out.walkingPrecedence == -1) out.walkingPrecedence = out.interleave == null ? 0 : 2;
        return out;
    }
}
