package com.spoiledmilk.spritebaker;

public final class CombatCandidate {
    public final int sequenceId,frameCount,totalCycles,score;public final String reason;
    CombatCandidate(int id,int frames,int cycles,int score,String reason){sequenceId=id;frameCount=frames;totalCycles=cycles;this.score=score;this.reason=reason;}
    public String toString(){return"Possible combat animation — "+frameCount+" frames / "+totalCycles+" cycles — "+reason+" (sequence "+sequenceId+")";}
}
