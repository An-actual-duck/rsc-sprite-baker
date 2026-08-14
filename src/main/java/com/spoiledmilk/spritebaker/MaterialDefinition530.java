package com.spoiledmilk.spritebaker;

/** One entry from revision-530 index 26/archive 0 material metadata. */
public final class MaterialDefinition530 {
    public final int id;
    public final boolean present, modelTextureFlag, opaque, lowDetail, materialFlag3;
    public final int scrollU, scrollV, effect, effectParam, averageColor;

    MaterialDefinition530(int id, boolean present, boolean modelTextureFlag, boolean opaque,
                          boolean lowDetail, boolean materialFlag3, int scrollU, int scrollV,
                          int effect, int effectParam, int averageColor) {
        this.id=id;this.present=present;this.modelTextureFlag=modelTextureFlag;this.opaque=opaque;
        this.lowDetail=lowDetail;this.materialFlag3=materialFlag3;this.scrollU=scrollU;this.scrollV=scrollV;
        this.effect=effect;this.effectParam=effectParam;this.averageColor=averageColor;
    }
}
