package com.spoiledmilk.spritebaker;

/** A reproducible revision-530 timeline position. Cycles are 20 ms client ticks. */
public final class FrameSample {
    public final int sequenceId;
    public final int frameIndex;
    public final int cycleOffset;
    public final long timeMillis;
    public final int durationCycles;
    public final int currentFrameId;
    public final int nextFrameId;

    FrameSample(int sequenceId, int frameIndex, int cycleOffset, long timeMillis,
                int durationCycles, int currentFrameId, int nextFrameId) {
        this.sequenceId=sequenceId; this.frameIndex=frameIndex; this.cycleOffset=cycleOffset;
        this.timeMillis=timeMillis; this.durationCycles=durationCycles;
        this.currentFrameId=currentFrameId; this.nextFrameId=nextFrameId;
    }
}
