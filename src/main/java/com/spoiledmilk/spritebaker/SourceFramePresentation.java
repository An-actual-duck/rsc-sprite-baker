package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;

/** Visual orientation applied to source-frame selection cards without changing their assignment identity. */
public final class SourceFramePresentation {
    private SourceFramePresentation(){ }

    public static int renderDirection(int sourceDirection,boolean swapFacingAway){
        return SheetDirection.rendered(sourceDirection,swapFacingAway);
    }

    public static BufferedImage orient(BufferedImage rendered,boolean horizontallyInverted){
        return HorizontalOrientation.apply(rendered,horizontallyInverted);
    }
}
