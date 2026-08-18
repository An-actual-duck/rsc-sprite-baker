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
    @Test void completeTimelineIncludesEveryClientCycleAndAdvancesEncodedFrames(){
        Sequence530 sequence=new Sequence530(72);sequence.durations=new int[]{2,3,1};sequence.frameIds=new int[]{100,200,300};sequence.loopOffset=3;
        java.util.List<FrameSample> samples=AnimationTimeline.selectableSamples(sequence,false);
        assertEquals(6,samples.size());assertArrayEquals(new int[]{0,0,1,1,1,2},samples.stream().mapToInt(s->s.frameIndex).toArray());assertArrayEquals(new int[]{0,1,0,1,2,0},samples.stream().mapToInt(s->s.cycleOffset).toArray());assertArrayEquals(new int[]{100,100,200,200,200,300},samples.stream().mapToInt(s->s.currentFrameId).toArray());
        assertEquals(java.util.List.of(0L,20L,40L,60L,80L,100L),samples.stream().map(s->s.timeMillis).collect(java.util.stream.Collectors.toList()));
    }
    @Test void optionalKeyframeViewContainsEachEncodedFrameOnce(){
        Sequence530 sequence=new Sequence530(8);sequence.durations=new int[]{2,3,1};sequence.frameIds=new int[]{10,11,12};
        assertArrayEquals(new int[]{0,1,2},AnimationTimeline.selectableSamples(sequence,true).stream().mapToInt(s->s.frameIndex).toArray());
    }
}
