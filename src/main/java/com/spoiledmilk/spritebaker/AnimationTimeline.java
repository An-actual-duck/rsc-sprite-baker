package com.spoiledmilk.spritebaker;

public final class AnimationTimeline {
    private AnimationTimeline() { }

    public static FrameSample sample(Sequence530 sequence, long timeMillis) {
        if (sequence.frameIds.length == 0) throw new IllegalArgumentException("sequence has no frames");
        long total = sequence.totalMillis();
        long normalized = total == 0 ? 0 : Math.floorMod(timeMillis, total);
        int elapsedCycles = (int) (normalized / 20L);
        int frame = 0;
        while (frame + 1 < sequence.durations.length && elapsedCycles >= sequence.durations[frame]) {
            elapsedCycles -= sequence.durations[frame++];
        }
        int next = frame + 1;
        if (next >= sequence.frameIds.length) {
            next = sequence.loopOffset > 0 ? next - sequence.loopOffset : -1;
            if (next < 0 || next >= sequence.frameIds.length) next = -1;
        }
        return new FrameSample(sequence.id, frame, elapsedCycles, normalized,
            sequence.durations[frame], sequence.frameIds[frame], next < 0 ? -1 : sequence.frameIds[next]);
    }

    public static long frameStartMillis(Sequence530 sequence, int frameIndex) {
        if (frameIndex < 0 || frameIndex >= sequence.frameIds.length) throw new IndexOutOfBoundsException();
        long cycles=0;
        for (int i=0; i<frameIndex; i++) cycles += sequence.durations[i];
        return cycles * 20L;
    }
}
