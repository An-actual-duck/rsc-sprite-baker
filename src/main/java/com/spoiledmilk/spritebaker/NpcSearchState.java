package com.spoiledmilk.spritebaker;

/** Monotonic request tokens prevent stale asynchronous browser results from applying. */
final class NpcSearchState {
    private long generation;
    long supersede(){return ++generation;}
    long current(){return generation;}
    boolean isCurrent(long candidate){return candidate==generation;}
}
