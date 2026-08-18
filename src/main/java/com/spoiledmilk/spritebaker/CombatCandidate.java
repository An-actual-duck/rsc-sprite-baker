package com.spoiledmilk.spritebaker;

public final class CombatCandidate {
    public final int sequenceId,frameCount,totalCycles,score;public final String reason;private final PoseSelection[] suggestions;
    CombatCandidate(int id,int frames,int cycles,int score,String reason){this(id,frames,cycles,score,reason,null);}
    CombatCandidate(int id,int frames,int cycles,int score,String reason,PoseSelection[] suggestions){sequenceId=id;frameCount=frames;totalCycles=cycles;this.score=score;this.reason=reason;this.suggestions=suggestions==null?null:new PoseSelection[]{suggestions[0].copy(),suggestions[1].copy(),suggestions[2].copy()};}
    public PoseSelection[] suggestions(){return suggestions==null?null:new PoseSelection[]{suggestions[0].copy(),suggestions[1].copy(),suggestions[2].copy()};}
    public String toString(){return"Possible combat animation — "+frameCount+" frames / "+totalCycles+" cycles — "+reason+" (sequence "+sequenceId+")";}
}
