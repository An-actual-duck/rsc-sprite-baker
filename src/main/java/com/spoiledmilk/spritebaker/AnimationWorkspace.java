package com.spoiledmilk.spritebaker;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Loaded NPC animation context. Cache access remains read-only. */
public final class AnimationWorkspace implements Closeable {
    public final Path cachePath;
    public final CacheReader cache;
    public final NpcDefinition530 npc;
    public final RenderAnimation530 bas;
    public final ModelDefinition baseModel;
    public final TextureProvider530 textures;
    private final ModelAnimator animator=new ModelAnimator();

    public AnimationWorkspace(Path cachePath,int npcId) throws IOException {
        this.cachePath=cachePath.toRealPath(); cache=new CacheReader(this.cachePath); npc=cache.loadNpc(npcId);
        bas=npc.renderAnimation==-1?null:cache.loadRenderAnimation(npc.renderAnimation);
        List<ModelDefinition> parts=new ArrayList<>(); for(int id:npc.modelIds)parts.add(cache.loadModel(id));
        baseModel=ModelAssembler.combine(parts);textures=new TextureProvider530(cache);
    }
    public ModelDefinition pose(PoseSelection selection,boolean globalTweening) throws IOException {
        Sequence530 sequence=cache.loadSequence(selection.sequenceId);
        if(selection.frameIndex<0||selection.frameIndex>=sequence.frameIds.length)throw new IllegalArgumentException("frame index outside sequence");
        Frame530 current=cache.loadFrame(sequence.frameIds[selection.frameIndex]); Frame530 next=null;
        if((globalTweening||sequence.tween)&&selection.cycleOffset>0){
            int nextIndex=selection.frameIndex+1;
            if(nextIndex>=sequence.frameIds.length)nextIndex=sequence.loopOffset>0?nextIndex-sequence.loopOffset:-1;
            if(nextIndex>=0&&nextIndex<sequence.frameIds.length)next=cache.loadFrame(sequence.frameIds[nextIndex]);
        }
        return animator.pose(baseModel,current,next,selection.cycleOffset,sequence.durations[selection.frameIndex]);
    }
    public PoseSelection selectionAt(int sequenceId,long millis,String source)throws IOException{
        return new PoseSelection(AnimationTimeline.sample(cache.loadSequence(sequenceId),millis),source);
    }
    @Override public void close()throws IOException{cache.close();}
}
