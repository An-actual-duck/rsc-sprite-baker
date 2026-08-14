package com.spoiledmilk.spritebaker;

final class BinaryInput {
    private final byte[] data;
    private int position;

    BinaryInput(byte[] data) { this(data, 0); }
    BinaryInput(byte[] data, int position) { this.data = data; this.position = position; }
    int position() { return position; }
    int remaining() { return data.length - position; }
    int u8() { return Byte.toUnsignedInt(data[position++]); }
    int i8() { return data[position++]; }
    int u16() { return (u8() << 8) | u8(); }
    int i32() { return (u8() << 24) | (u8() << 16) | (u8() << 8) | u8(); }
    int u24() { return (u8() << 16) | (u8() << 8) | u8(); }
    int signedShortSmart() {
        return Byte.toUnsignedInt(data[position]) < 128 ? u8() - 64 : u16() - 49152;
    }
}
