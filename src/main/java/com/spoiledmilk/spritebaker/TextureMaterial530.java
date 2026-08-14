package com.spoiledmilk.spritebaker;

import java.util.List;

/** Decoded deterministic software-raster material. Pixels are row-major RGB. */
public final class TextureMaterial530 {
    public final MaterialDefinition530 definition;
    public final int size;
    public final int[] pixels;
    public final List<Integer> operationTypes;

    TextureMaterial530(MaterialDefinition530 definition,int size,int[] pixels,List<Integer> operationTypes){
        this.definition=definition;this.size=size;this.pixels=pixels;this.operationTypes=List.copyOf(operationTypes);
    }
}
