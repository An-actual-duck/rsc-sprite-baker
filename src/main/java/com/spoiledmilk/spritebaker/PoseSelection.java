package com.spoiledmilk.spritebaker;

public final class PoseSelection {
    public int sequenceId;
    public int frameIndex;
    public int cycleOffset;
    public long timeMillis;
    public String source;

    public PoseSelection() { }
    public PoseSelection(FrameSample sample, String source) {
        this.sequenceId=sample.sequenceId; this.frameIndex=sample.frameIndex;
        this.cycleOffset=sample.cycleOffset; this.timeMillis=sample.timeMillis; this.source=source;
    }
    public PoseSelection copy() {
        PoseSelection out=new PoseSelection(); out.sequenceId=sequenceId; out.frameIndex=frameIndex;
        out.cycleOffset=cycleOffset; out.timeMillis=timeMillis; out.source=source; return out;
    }
}
