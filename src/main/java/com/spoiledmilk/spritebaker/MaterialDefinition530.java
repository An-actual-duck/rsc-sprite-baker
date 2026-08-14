package com.spoiledmilk.spritebaker;

/** One entry from revision-530 index 26/archive 0 material metadata. */
public final class MaterialDefinition530 {
    public final int id;
    public final boolean present, softwareEligible, opaque, lowDetail, repeat;
    public final int scrollU, scrollV, effect, effectParam, averageColor;

    MaterialDefinition530(int id, boolean present, boolean softwareEligible, boolean opaque,
                          boolean lowDetail, boolean repeat, int scrollU, int scrollV,
                          int effect, int effectParam, int averageColor) {
        this.id=id;this.present=present;this.softwareEligible=softwareEligible;this.opaque=opaque;
        this.lowDetail=lowDetail;this.repeat=repeat;this.scrollU=scrollU;this.scrollV=scrollV;
        this.effect=effect;this.effectParam=effectParam;this.averageColor=averageColor;
    }
}
