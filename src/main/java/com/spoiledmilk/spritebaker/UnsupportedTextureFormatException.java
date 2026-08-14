package com.spoiledmilk.spritebaker;

/** A precise compatibility failure; callers must show it rather than substituting a color. */
public final class UnsupportedTextureFormatException extends IllegalArgumentException {
    public UnsupportedTextureFormatException(int textureId,String detail){super("texture "+textureId+" unsupported: "+detail);}
}
