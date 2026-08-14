package com.spoiledmilk.spritebaker;

import java.util.HashMap;
import java.util.Map;
import net.runelite.cache.definitions.ModelDefinition;

/** Applies revision-530 vertex, alpha and packed-HSL transforms to a copied model. */
public final class ModelAnimator {
    private static final int[] SIN=new int[2048], COS=new int[2048];
    static { for(int i=0;i<2048;i++){ SIN[i]=(int)(Math.sin(i*Math.PI/1024)*65536); COS[i]=(int)(Math.cos(i*Math.PI/1024)*65536); } }

    public ModelDefinition pose(ModelDefinition base, Frame530 current, Frame530 next,
                                int numerator, int denominator) {
        if (next != null && next.framemap.id != current.framemap.id) next=null;
        ModelDefinition model=ModelAssembler.copy(base);
        Map<Integer,Integer> currentBySlot=index(current), nextBySlot=next==null?Map.of():index(next);
        int pivotX=0,pivotY=0,pivotZ=0;
        for(int slot=0;slot<current.framemap.types.length;slot++) {
            Integer ci=currentBySlot.get(slot), ni=nextBySlot.get(slot);
            if(ci==null && ni==null) continue;
            int type=current.framemap.types[slot], defaultValue=type==3?128:0;
            int x=ci==null?defaultValue:current.x[ci], y=ci==null?defaultValue:current.y[ci], z=ci==null?defaultValue:current.z[ci];
            boolean held = (ci != null && (current.flags[ci] & 2) != 0)
                || (ni != null && (next.flags[ni] & 1) != 0);
            boolean allowTween=next!=null && numerator>0 && !held;
            if(allowTween) {
                int nx=ni==null?defaultValue:next.x[ni], ny=ni==null?defaultValue:next.y[ni], nz=ni==null?defaultValue:next.z[ni];
                if(type==2) { x=angle(x,nx,numerator,denominator); y=angle(y,ny,numerator,denominator); z=angle(z,nz,numerator,denominator); }
                else if(type==7) { x=hue(x,nx,numerator,denominator); y=lerp(y,ny,numerator,denominator); z=lerp(z,nz,numerator,denominator); }
                else { x=lerp(x,nx,numerator,denominator); y=lerp(y,ny,numerator,denominator); z=lerp(z,nz,numerator,denominator); }
            }
            int[] groups=current.framemap.groups[slot];
            if(type==0) {
                long sx=0,sy=0,sz=0; int count=0;
                for(int v=0;v<model.vertexCount;v++) if(contains(groups,group(model.packedVertexGroups,v))) { sx+=model.vertexX[v]; sy+=model.vertexY[v]; sz+=model.vertexZ[v]; count++; }
                pivotX=(count==0?0:(int)(sx/count))+x; pivotY=(count==0?0:(int)(sy/count))+y; pivotZ=(count==0?0:(int)(sz/count))+z;
            } else if(type>=1 && type<=3) {
                for(int v=0;v<model.vertexCount;v++) if(contains(groups,group(model.packedVertexGroups,v))) {
                    if(type==1){ model.vertexX[v]+=x; model.vertexY[v]+=y; model.vertexZ[v]+=z; }
                    else if(type==2) rotate(model,v,pivotX,pivotY,pivotZ,x,y,z);
                    else { model.vertexX[v]=pivotX+(model.vertexX[v]-pivotX)*x/128; model.vertexY[v]=pivotY+(model.vertexY[v]-pivotY)*y/128; model.vertexZ[v]=pivotZ+(model.vertexZ[v]-pivotZ)*z/128; }
                }
            } else if(type==5 && model.faceTransparencies!=null) {
                for(int f=0;f<model.faceCount;f++) if(contains(groups,group(model.packedTransparencyVertexGroups,f))) {
                    int alpha=Math.max(0,Math.min(255,Byte.toUnsignedInt(model.faceTransparencies[f])+x*8)); model.faceTransparencies[f]=(byte)alpha;
                }
            } else if(type==7) {
                for(int f=0;f<model.faceCount;f++) if(contains(groups,group(model.packedTransparencyVertexGroups,f))) {
                    int hsl=Short.toUnsignedInt(model.faceColors[f]); int h=((hsl>>>10)+x)&63;
                    int s=Math.max(0,Math.min(7,((hsl>>>7)&7)+y)); int l=Math.max(0,Math.min(127,(hsl&127)+z));
                    model.faceColors[f]=(short)((h<<10)|(s<<7)|l);
                }
            }
        }
        return model;
    }

    private static Map<Integer,Integer> index(Frame530 frame){ Map<Integer,Integer> out=new HashMap<>(); for(int i=0;i<frame.slots.length;i++)out.put(frame.slots[i],i); return out; }
    private static int group(int[] groups,int i){return groups==null?-1:groups[i];}
    private static boolean contains(int[] groups,int value){for(int group:groups)if(group==value)return true;return false;}
    private static int lerp(int a,int b,int n,int d){return a+(b-a)*n/d;}
    private static int angle(int a,int b,int n,int d){int delta=(b-a)&2047;if(delta>=1024)delta-=2048;return(a+delta*n/d)&2047;}
    private static int hue(int a,int b,int n,int d){int delta=(b-a)&63;if(delta>=32)delta-=64;return(a+delta*n/d)&63;}
    private static void rotate(ModelDefinition m,int v,int px,int py,int pz,int ax,int ay,int az){
        int x=m.vertexX[v]-px,y=m.vertexY[v]-py,z=m.vertexZ[v]-pz,t;
        if(az!=0){t=(SIN[az]*y+COS[az]*x)>>16;y=(COS[az]*y-SIN[az]*x)>>16;x=t;}
        if(ax!=0){t=(COS[ax]*y-SIN[ax]*z)>>16;z=(SIN[ax]*y+COS[ax]*z)>>16;y=t;}
        if(ay!=0){t=(SIN[ay]*z+COS[ay]*x)>>16;z=(COS[ay]*z-SIN[ay]*x)>>16;x=t;}
        m.vertexX[v]=x+px;m.vertexY[v]=y+py;m.vertexZ[v]=z+pz;
    }
}
