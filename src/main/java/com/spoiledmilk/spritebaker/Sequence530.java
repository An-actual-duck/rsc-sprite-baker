package com.spoiledmilk.spritebaker;

import java.util.Arrays;

public final class Sequence530 {
    public final int id;
    public int[] durations = new int[0];
    public int[] frameIds = new int[0];
    public int loopOffset = -1;
    public boolean[] interleave;
    public boolean stretches;
    public int priority = 5;
    public int rightHandItem = -1;
    public int leftHandItem = -1;
    public int maxLoops = 99;
    public int precedence = -1;
    public int walkingPrecedence = -1;
    public int replayMode = 2;
    public int[] secondaryFrameIds;
    public boolean tween;
    public boolean special;

    Sequence530(int id) { this.id = id; }

    public int totalCycles() { return Arrays.stream(durations).sum(); }
    public long totalMillis() { return totalCycles() * 20L; }
}
