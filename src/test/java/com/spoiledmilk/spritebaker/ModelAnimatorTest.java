package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;

class ModelAnimatorTest {
    @Test void translatesOnlyVerticesInFramemapGroupsWithoutMutatingBase(){
        ModelDefinition base=new ModelDefinition();base.id=1;base.vertexCount=2;base.vertexX=new int[]{0,20};base.vertexY=new int[]{0,0};base.vertexZ=new int[]{0,0};base.packedVertexGroups=new int[]{0,1};base.faceCount=0;base.faceIndices1=new int[0];base.faceIndices2=new int[0];base.faceIndices3=new int[0];base.faceColors=new short[0];
        Framemap530 map=new Framemap530(1,new int[]{0,1},new boolean[2],new int[]{65535,65535},new int[][]{{0},{0}});
        Frame530 frame=new Frame530(1,map,new int[]{0,1},new int[]{0,10},new int[]{0,2},new int[]{0,3},new int[2],new int[]{-1,-1});
        ModelDefinition posed=new ModelAnimator().pose(base,frame,null,0,1);
        assertArrayEquals(new int[]{10,20},posed.vertexX);assertArrayEquals(new int[]{2,0},posed.vertexY);assertArrayEquals(new int[]{3,0},posed.vertexZ);assertArrayEquals(new int[]{0,20},base.vertexX);
    }
    @Test void distinctEncodedFramesProduceDistinctPoses(){
        ModelDefinition base=new ModelDefinition();base.id=2;base.vertexCount=1;base.vertexX=new int[]{0};base.vertexY=new int[]{0};base.vertexZ=new int[]{0};base.packedVertexGroups=new int[]{0};base.faceCount=0;base.faceIndices1=new int[0];base.faceIndices2=new int[0];base.faceIndices3=new int[0];base.faceColors=new short[0];
        Framemap530 map=new Framemap530(2,new int[]{1},new boolean[1],new int[]{65535},new int[][]{{0}});
        Frame530 first=new Frame530(100,map,new int[]{0},new int[]{3},new int[]{0},new int[]{0},new int[1],new int[]{-1});Frame530 second=new Frame530(200,map,new int[]{0},new int[]{17},new int[]{0},new int[]{0},new int[1],new int[]{-1});
        ModelAnimator animator=new ModelAnimator();assertEquals(3,animator.pose(base,first,null,0,1).vertexX[0]);assertEquals(17,animator.pose(base,second,null,0,1).vertexX[0]);
    }
}
