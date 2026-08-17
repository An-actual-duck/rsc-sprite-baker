package com.spoiledmilk.spritebaker;

import net.runelite.cache.definitions.ModelDefinition;

/**
 * RuneScape revision-530 model data not represented by RuneLite's
 * {@link ModelDefinition}. Arrays are indexed by texture-face index; entries
 * for render type 0 remain zero.
 */
public final class ModelDefinition530 extends ModelDefinition {
    public short[] textureScaleX;
    public short[] textureScaleY;
    public short[] textureScaleZ;
    public byte[] textureRotation;
    public byte[] textureDirection;
    public byte[] textureTranslation;
    public byte[] textureCubeU;
    public byte[] textureCubeV;
}
