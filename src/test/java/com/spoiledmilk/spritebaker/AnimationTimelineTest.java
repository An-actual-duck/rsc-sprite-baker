package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationTimelineTest {
    @Test void samplesTwentyMillisecondClientCyclesAndLoops(){
        Sequence530 sequence=new Sequence530(3);sequence.durations=new int[]{2,3};sequence.frameIds=new int[]{10,11};sequence.loopOffset=2;
        FrameSample first=AnimationTimeline.sample(sequence,20);assertEquals(0,first.frameIndex);assertEquals(1,first.cycleOffset);assertEquals(11,first.nextFrameId);
        FrameSample second=AnimationTimeline.sample(sequence,40);assertEquals(1,second.frameIndex);assertEquals(0,second.cycleOffset);
        FrameSample loop=AnimationTimeline.sample(sequence,100);assertEquals(0,loop.frameIndex);assertEquals(0,loop.timeMillis);
    }
}
