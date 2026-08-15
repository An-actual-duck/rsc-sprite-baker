package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Independent decoder for the deliberately bounded revision-530 procedural graph subset.
 * Unsupported operation IDs fail closed and are never replaced with an average color.
 */
public final class ProceduralTexture530Decoder {
    public Decoded decode(int textureId,byte[] data,int size){
        try{return decode(textureId,data,size,id->{throw new UnsupportedTextureFormatException(textureId,"texture dependency "+id+" requires a provider");});}
        catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public Decoded decode(int textureId,byte[] data,int size,DependencyResolver dependencies)throws IOException{
        BinaryInput in=new BinaryInput(data);int count=in.u8();Node[] nodes=new Node[count];int[][] children=new int[count][];List<Integer> types=new ArrayList<>();
        for(int i=0;i<count;i++){
            in.u8();int type=in.u8();types.add(type);Node node=create(textureId,type,dependencies);node.cache=in.u8();int codes=in.u8();
            for(int c=0;c<codes;c++)node.decode(textureId,in.u8(),in);
            node.finish(textureId);children[i]=new int[node.childCount()];for(int c=0;c<children[i].length;c++)children[i][c]=in.u8();nodes[i]=node;
        }
        for(int i=0;i<count;i++){nodes[i].children=new Node[children[i].length];for(int c=0;c<children[i].length;c++){int child=children[i][c];if(child<0||child>=count)throw new UnsupportedTextureFormatException(textureId,"invalid child operation "+child);nodes[i].children[c]=nodes[child];}}
        int colorRoot=in.u8();in.u8();in.u8();if(colorRoot>=count)throw new UnsupportedTextureFormatException(textureId,"invalid color root "+colorRoot);
        int[] pixels=new int[size*size];for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int[] rgb=nodes[colorRoot].rgb(x,y,size);int r=clamp(rgb[0]>>4,0,255),g=clamp(rgb[1]>>4,0,255),b=clamp(rgb[2]>>4,0,255);pixels[y*size+(size-1-x)]=(r<<16)|(g<<8)|b;
        }
        return new Decoded(pixels,types);
    }

    private static Node create(int id,int type,DependencyResolver dependencies){switch(type){
        case 0:return new Fill(true);case 1:return new Fill(false);case 2:return new Gradient(true);case 3:return new Gradient(false);
        case 7:return new Combine();case 8:return new Curve();case 10:return new ColorGradient();case 13:return new HashNoise();case 30:return new Range();case 34:return new PerlinNoise();case 36:return new TextureDependency(dependencies);case 38:return new LineNoise();
        default:throw new UnsupportedTextureFormatException(id,"procedural operation "+type);
    }}
    @FunctionalInterface public interface DependencyResolver{Dependency resolve(int textureId)throws IOException;}
    public static final class Dependency{public final int size;public final int[] pixels;public Dependency(int size,int[] pixels){this.size=size;this.pixels=pixels;}}
    public static final class Decoded{public final int[] pixels;public final List<Integer> operationTypes;Decoded(int[] p,List<Integer> t){pixels=p;operationTypes=List.copyOf(t);}}
    private abstract static class Node{int cache;Node[] children;abstract int childCount();void decode(int id,int code,BinaryInput in){throw new UnsupportedTextureFormatException(id,"operation parameter "+code+" for "+getClass().getSimpleName());}void finish(int id){}abstract int[] rgb(int x,int y,int size)throws IOException;int mono(int x,int y,int size)throws IOException{int[] c=rgb(x,y,size);return(c[0]+c[1]+c[2])/3;}}
    private static final class Fill extends Node{final boolean mono;int value=4096,r,g,b;Fill(boolean mono){this.mono=mono;}int childCount(){return 0;}void decode(int id,int code,BinaryInput in){if(code!=0)super.decode(id,code,in);if(mono)value=in.u16();else{int color=in.u24();r=(color>>12)&4080;g=(color>>4)&4080;b=(color&255)<<4;}}int[] rgb(int x,int y,int size){return mono?new int[]{value,value,value}:new int[]{r,g,b};}}
    private static final class Gradient extends Node{final boolean horizontal;Gradient(boolean horizontal){this.horizontal=horizontal;}int childCount(){return 0;}int[] rgb(int x,int y,int size){int v=((horizontal?x:y)<<12)/size;return new int[]{v,v,v};}}
    private static final class Combine extends Node{int function=6;boolean monochrome;int childCount(){return 2;}void decode(int id,int code,BinaryInput in){if(code==0)function=in.u8();else if(code==1)monochrome=in.u8()==1;else super.decode(id,code,in);}void finish(int id){if(function!=3)throw new UnsupportedTextureFormatException(id,"combine function "+function);}int[] rgb(int x,int y,int size)throws IOException{int[] a=children[0].rgb(x,y,size),b=children[1].rgb(x,y,size);return new int[]{a[0]*b[0]>>12,a[1]*b[1]>>12,a[2]*b[2]>>12};}}
    private static final class Range extends Node{int min=1024,max=3072,range=2048;boolean monochrome;int childCount(){return 1;}void decode(int id,int code,BinaryInput in){if(code==0)min=in.u16();else if(code==1)max=in.u16();else if(code==2)monochrome=in.u8()==1;else super.decode(id,code,in);}void finish(int id){range=max-min;}int[] rgb(int x,int y,int size)throws IOException{int[] c=children[0].rgb(x,y,size);return new int[]{min+(range*c[0]>>12),min+(range*c[1]>>12),min+(range*c[2]>>12)};}}
    private static final class Curve extends Node{int mode;int[][] markers;int childCount(){return 1;}void decode(int id,int code,BinaryInput in){if(code!=0)super.decode(id,code,in);mode=in.u8();int n=in.u8();markers=new int[n][2];for(int i=0;i<n;i++){markers[i][0]=in.u16();markers[i][1]=in.u16();}}void finish(int id){if(mode!=0)throw new UnsupportedTextureFormatException(id,"curve interpolation "+mode);if(markers==null||markers.length<2)throw new UnsupportedTextureFormatException(id,"curve marker count");}int[] rgb(int x,int y,int size)throws IOException{int v=children[0].mono(x,y,size),i=1;while(i<markers.length&&v>=markers[i][0])i++;int result;if(i==markers.length)result=markers[i-1][1];else if(i==0)result=markers[0][1];else{int[] a=markers[i-1],b=markers[i];result=a[1]+(b[1]-a[1])*(v-a[0])/Math.max(1,b[0]-a[0]);}return new int[]{result,result,result};}}
    private static final class ColorGradient extends Node{int[][] samples;int childCount(){return 1;}void decode(int id,int code,BinaryInput in){if(code!=0)super.decode(id,code,in);int preset=in.u8();if(preset!=0)throw new UnsupportedTextureFormatException(id,"color-gradient preset "+preset);int n=in.u8();samples=new int[n][4];for(int i=0;i<n;i++){samples[i][0]=in.u16();samples[i][1]=in.u8()<<4;samples[i][2]=in.u8()<<4;samples[i][3]=in.u8()<<4;}}void finish(int id){if(samples==null||samples.length<2)throw new UnsupportedTextureFormatException(id,"color-gradient sample count");}int[] rgb(int x,int y,int size)throws IOException{int v=children[0].mono(x,y,size),i=0;while(i<samples.length&&v>=samples[i][0])i++;if(i==0)return color(samples[0]);if(i==samples.length)return color(samples[i-1]);int[] a=samples[i-1],b=samples[i];int w=(v-a[0])*4096/Math.max(1,b[0]-a[0]);return new int[]{(a[1]*(4096-w)+b[1]*w)>>12,(a[2]*(4096-w)+b[2]*w)>>12,(a[3]*(4096-w)+b[3]*w)>>12};}private int[] color(int[] s){return new int[]{s[1],s[2],s[3]};}}
    private static final class HashNoise extends Node{
        int childCount(){return 0;}
        int[] rgb(int x,int y,int size){
            int xFraction=(x<<12)/size,yFraction=(y<<12)/size;
            int coordinate=xFraction+yFraction*57,hash=coordinate^(coordinate<<1);
            int value=(4096-((hash*(hash*hash*15731+789221)+1376312589)&Integer.MAX_VALUE)/262144)%4096;
            return new int[]{value,value,value};
        }
    }
    private static final class PerlinNoise extends Node{
        boolean normalize=true;int octaves=4,persistence=1638,xScale=4,yScale=4,seed;short[] amplitudes,frequencies;byte[] permutation;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){switch(code){
            case 0:normalize=in.u8()==1;break;
            case 1:octaves=in.u8();break;
            case 2:persistence=in.i16();if(persistence<0){amplitudes=new short[octaves];for(int i=0;i<octaves;i++)amplitudes[i]=(short)in.i16();}break;
            case 3:xScale=yScale=in.u8();break;
            case 4:seed=in.u8();break;
            case 5:xScale=in.u8();break;
            case 6:yScale=in.u8();break;
            default:super.decode(id,code,in);
        }}
        void finish(int id){
            if(octaves<=0)throw new UnsupportedTextureFormatException(id,"noise octave count "+octaves);
            permutation=permutation(seed);
            if(persistence>0){amplitudes=new short[octaves];frequencies=new short[octaves];for(int i=0;i<octaves;i++){amplitudes[i]=(short)(Math.pow((float)persistence/4096.0F,i)*4096.0D);frequencies[i]=(short)Math.pow(2.0D,i);}}
            else if(amplitudes!=null&&amplitudes.length==octaves){frequencies=new short[octaves];for(int i=0;i<octaves;i++)frequencies[i]=(short)Math.pow(2.0D,i);}
            else throw new UnsupportedTextureFormatException(id,"noise amplitudes do not match octave count "+octaves);
            while(octaves>1&&amplitudes[octaves-1]>=-8&&amplitudes[octaves-1]<=8)octaves--;
        }
        int[] rgb(int x,int y,int size){int value=noise(x,y,size);return new int[]{value,value,value};}
        private int noise(int x,int y,int size){
            int yFraction=(y<<12)/size,yBase=yScale*yFraction;
            if(octaves==1){int value=sample(x,size,yBase,0)*amplitudes[0]>>12;return normalize?(value>>1)+2048:value;}
            int value=0;
            for(int octave=0;octave<octaves;octave++){
                int amplitude=amplitudes[octave];
                if(amplitude>8||amplitude< -8)value+=sample(x,size,yBase,octave)*amplitude>>12;
                if(normalize&&octave==octaves-1)value=(value>>1)+2048;
            }
            return value;
        }
        private int sample(int x,int size,int yBase,int octave){
            int frequency=frequencies[octave]<<12;
            int scaledY=yBase*frequency>>12,periodY=yScale*frequency>>12;
            int yCell=scaledY>>12,yNext=yCell+1;
            if(yNext>=periodY)yNext=0;
            int yRemainder=scaledY&0xfff,yFade=fade(yRemainder);
            int yHash=permutation[yCell&0xff]&0xff,yNextHash=permutation[yNext&0xff]&0xff;
            int scaledX=xScale*((x<<12)/size)*frequency>>12,periodX=xScale*frequency>>12;
            return interpolate(scaledX,yNextHash,yHash,periodX,yRemainder,yFade);
        }
        private int interpolate(int x,int yNextHash,int yHash,int periodX,int yRemainder,int yFade){
            int yOffset=yRemainder-4096,xCell=x>>12,xNext=xCell+1,xIndex=xCell&0xff;
            if(xNext>=periodX)xNext=0;
            int xRemainder=x&0xfff,xFade=fade(xRemainder),gradient=permutation[xIndex+yHash]&3;
            int near=gradient>1?(gradient==2?xRemainder-yRemainder:-xRemainder-yRemainder):(gradient==0?yRemainder+xRemainder:-xRemainder+yRemainder);
            xNext&=0xff;
            int xOffset=xRemainder-4096;
            gradient=permutation[yHash+xNext]&3;
            int far=gradient<=1?(gradient==0?yRemainder+xOffset:-xOffset+yRemainder):(gradient==2?xOffset-yRemainder:-xOffset-yRemainder);
            int lower=near+((far-near)*xFade>>12);
            gradient=permutation[xIndex+yNextHash]&3;
            near=gradient>1?(gradient==2?xRemainder-yOffset:-xRemainder-yOffset):(gradient==0?xRemainder+yOffset:yOffset-xRemainder);
            gradient=permutation[yNextHash+xNext]&3;
            far=gradient>1?(gradient==2?xOffset-yOffset:-yOffset-xOffset):(gradient==0?xOffset+yOffset:yOffset-xOffset);
            int upper=near+((far-near)*xFade>>12);
            return lower+(yFade*(upper-lower)>>12);
        }
        private static int fade(int value){int cube=value*(value*value>>12)>>12;int factor=(value*(value*6-61440)>>12)+40960;return cube*factor>>12;}
        private static byte[] permutation(int seed){
            Random random=new Random(seed);byte[] values=new byte[512];for(int i=0;i<255;i++)values[i]=(byte)i;
            for(int i=0;i<255;i++){int remaining=255-i,index=randomBound(remaining,random);byte value=values[index];values[index]=values[remaining];values[remaining]=values[511-i]=value;}
            return values;
        }
    }
    private static final class TextureDependency extends Node{final DependencyResolver resolver;int textureId=-1;TextureDependency(DependencyResolver resolver){this.resolver=resolver;}int childCount(){return 0;}void decode(int id,int code,BinaryInput in){if(code!=0)super.decode(id,code,in);textureId=in.u16();}void finish(int id){if(textureId<0)throw new UnsupportedTextureFormatException(id,"texture dependency ID is absent");}int[] rgb(int x,int y,int size)throws IOException{Dependency dependency=resolver.resolve(textureId);if(dependency==null||dependency.size<=0||dependency.pixels.length!=dependency.size*dependency.size)throw new UnsupportedTextureFormatException(textureId,"invalid dependency pixels");int sx=x*dependency.size/size,sy=y*dependency.size/size;int pixel=dependency.pixels[sy*dependency.size+(dependency.size-1-sx)];return new int[]{pixel>>12&0xff0,pixel>>4&0xff0,(pixel&0xff)<<4};}}
    private static final class LineNoise extends Node{
        private static final int[] SINE=trigonometry(true),COSINE=trigonometry(false);
        int seed,lineCount=2000,length=16,baseAngle,angleRange=4096,cachedSize=-1;int[][] image;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:seed=in.u8();break;case 1:lineCount=in.u16();break;case 2:length=in.u8();break;case 3:baseAngle=in.u16();break;case 4:angleRange=in.u16();break;default:super.decode(id,code,in);}}
        int[] rgb(int x,int y,int size){if(cachedSize!=size)generate(size);int value=image[y][x];return new int[]{value,value,value};}
        private void generate(int size){
            cachedSize=size;image=new int[size][size];int halfRange=angleRange>>1;Random random=new Random(seed);
            for(int line=0;line<lineCount;line++){
                int angle=angleRange>0?baseAngle+randomBound(angleRange,random)-halfRange:baseAngle;
                int startX=randomBound(size,random),angleIndex=angle>>4&0xff,startY=randomBound(size,random);
                int endX=startX+(length*COSINE[angleIndex]>>12),endY=startY+(SINE[angleIndex]*length>>12);
                int deltaX=endX-startX,deltaY=endY-startY;if(deltaX==0&&deltaY==0)continue;
                if(deltaX<0)deltaX=-deltaX;if(deltaY<0)deltaY=-deltaY;boolean steep=deltaY>deltaX;int temporary;
                if(steep){temporary=startX;int swap=endX;endX=endY;endY=swap;startX=startY;startY=temporary;}
                if(startX>endX){temporary=startX;int swap=startY;startX=endX;startY=endY;endY=swap;endX=temporary;}
                deltaX=endX-startX;deltaY=endY-startY;if(deltaY<0)deltaY=-deltaY;int currentY=startY,error=-deltaX/2;
                int initial=1024-(randomBound(4096,random)>>2),yStep=endY<=startY?-1:1,intensityStep=2048/deltaX;
                for(int currentX=startX;currentX<endX;currentX++){
                    error+=deltaY;int value=intensityStep*(currentX-startX)+initial+1024,wrappedY=currentY&(size-1);
                    if(error>0){error-=deltaX;currentY+=yStep;}int wrappedX=currentX&(size-1);
                    if(steep)image[wrappedY][wrappedX]=value;else image[wrappedX][wrappedY]=value;
                }
            }
        }
        private static int[] trigonometry(boolean sine){int[] table=new int[256];for(int i=0;i<256;i++){double radians=(double)i/255.0D*6.283185307179586D;table[i]=(int)((sine?Math.sin(radians):Math.cos(radians))*4096.0D);}return table;}
    }
    private static int randomBound(int bound,Random random){
        if(bound<=0)throw new IllegalArgumentException("random bound must be positive");
        if((bound&-bound)==bound)return(int)(((long)random.nextInt()&0xffffffffL)*bound>>32);
        int threshold=Integer.MIN_VALUE-(int)(0x100000000L%bound),value;do value=random.nextInt();while(threshold<=value);
        return((bound-1)&value>>31)+(value+(value>>>31))%bound;
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
