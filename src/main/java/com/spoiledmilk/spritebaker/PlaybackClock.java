package com.spoiledmilk.spritebaker;

import java.util.function.LongSupplier;

/** Small deterministic play/pause clock; callers own rendering and scheduling. */
public final class PlaybackClock implements AutoCloseable {
    private final LongSupplier nowMillis;
    private boolean playing,closed;
    private long positionMillis,startedMillis;

    public PlaybackClock(){this(System::currentTimeMillis);}
    PlaybackClock(LongSupplier nowMillis){this.nowMillis=nowMillis;}

    public void play(){
        ensureOpen();
        if(!playing){startedMillis=nowMillis.getAsLong()-positionMillis;playing=true;}
    }

    public void pause(){if(playing){positionMillis=Math.max(0,nowMillis.getAsLong()-startedMillis);playing=false;}}
    public void seek(long millis){
        ensureOpen();
        positionMillis=Math.max(0,millis);
        if(playing)startedMillis=nowMillis.getAsLong()-positionMillis;
    }
    public void stop(){playing=false;positionMillis=0;}
    public boolean isPlaying(){return playing;}
    public boolean isClosed(){return closed;}
    public long elapsedMillis(){return playing?Math.max(0,nowMillis.getAsLong()-startedMillis):positionMillis;}
    public long loopPosition(long durationMillis){return durationMillis<=0?0:Math.floorMod(elapsedMillis(),durationMillis);}

    @Override public void close(){stop();closed=true;}
    private void ensureOpen(){if(closed)throw new IllegalStateException("playback is closed");}
}
