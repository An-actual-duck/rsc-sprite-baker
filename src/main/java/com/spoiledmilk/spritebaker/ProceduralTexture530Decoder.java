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
        return decode(textureId,data,size,dependencies,id->{throw new UnsupportedTextureFormatException(textureId,"sprite dependency "+id+" requires a provider");});
    }
    public Decoded decode(int textureId,byte[] data,int size,DependencyResolver dependencies,SpriteResolver sprites)throws IOException{
        BinaryInput in=new BinaryInput(data);int count=in.u8();Node[] nodes=new Node[count];int[][] children=new int[count][];List<Integer> types=new ArrayList<>();
        for(int i=0;i<count;i++){
            in.u8();int type=in.u8();types.add(type);Node node=create(textureId,type,dependencies,sprites);node.cache=in.u8();int codes=in.u8();
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

    private static Node create(int id,int type,DependencyResolver dependencies,SpriteResolver sprites){switch(type){
        case 0:return new Fill(true);case 1:return new Fill(false);case 2:return new Gradient(true);case 3:return new Gradient(false);case 4:return new BrickTiles();case 5:return new BoxBlur();case 6:return new Clamp();
        case 7:return new Combine();case 8:return new Curve();case 9:return new Flip();case 10:return new ColorGradient();case 12:return new Waveform();case 13:return new HashNoise();case 15:return new CellularNoise();case 17:return new HslAdjust();case 19:return new CoordinateDisplacement();case 20:return new Tile();case 21:return new Interpolate();case 22:return new Invert();case 27:return new Stripes();case 30:return new Range();case 32:return new BumpLighting();case 34:return new PerlinNoise();case 36:return new TextureDependency(dependencies);case 38:return new LineNoise();case 39:return new SpriteDependencyNode(sprites);
        default:throw new UnsupportedTextureFormatException(id,"procedural operation "+type);
    }}
    @FunctionalInterface public interface DependencyResolver{Dependency resolve(int textureId)throws IOException;}
    @FunctionalInterface public interface SpriteResolver{SpriteDependency resolve(int spriteId)throws IOException;}
    public static final class Dependency{public final int size;public final int[] pixels;public Dependency(int size,int[] pixels){this.size=size;this.pixels=pixels;}}
    public static final class SpriteDependency{public final int width,height;public final int[] pixels;public SpriteDependency(int width,int height,int[] pixels){this.width=width;this.height=height;this.pixels=pixels;}}
    public static final class Decoded{public final int[] pixels;public final List<Integer> operationTypes;Decoded(int[] p,List<Integer> t){pixels=p;operationTypes=List.copyOf(t);}}
    private abstract static class Node{int cache;Node[] children;abstract int childCount();void decode(int id,int code,BinaryInput in){throw new UnsupportedTextureFormatException(id,"operation parameter "+code+" for "+getClass().getSimpleName());}void finish(int id){}abstract int[] rgb(int x,int y,int size)throws IOException;int mono(int x,int y,int size)throws IOException{return rgb(x,y,size)[0];}}
    private static final class Fill extends Node{final boolean mono;int value=4096,r,g,b;Fill(boolean mono){this.mono=mono;}int childCount(){return 0;}void decode(int id,int code,BinaryInput in){if(code!=0)super.decode(id,code,in);if(mono){if(in.remaining()<1)throw new UnsupportedTextureFormatException(id,"truncated operation 0 parameter 0");value=(in.u8()<<12)/255;}else{int color=in.u24();r=(color>>12)&4080;g=(color>>4)&4080;b=(color&255)<<4;}}int[] rgb(int x,int y,int size){return mono?new int[]{value,value,value}:new int[]{r,g,b};}}
    private static final class Gradient extends Node{final boolean horizontal;Gradient(boolean horizontal){this.horizontal=horizontal;}int childCount(){return 0;}int[] rgb(int x,int y,int size){int v=((horizontal?x:y)<<12)/size;return new int[]{v,v,v};}}
    private static final class BrickTiles extends Node{
        int columns=4,rows=8,horizontalJitter=409,verticalJitter=204,rowOffset=1024,verticalPhase,mortar=81,brightnessVariation=1024;
        int columnWidth,mortarHalf;int[] rowBounds;int[][] columnBounds,brightness;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:columns=in.u8();break;case 1:rows=in.u8();break;case 2:horizontalJitter=in.u16();break;case 3:verticalJitter=in.u16();break;case 4:rowOffset=in.u16();break;case 5:verticalPhase=in.u16();break;case 6:mortar=in.u16();break;case 7:brightnessVariation=in.u16();break;default:super.decode(id,code,in);}}
        void finish(int id){
            if(columns<=0||rows<=0)throw new UnsupportedTextureFormatException(id,"brick grid "+columns+"x"+rows);
            Random random=new Random(rows);int rowHeight=4096/rows,rowHalf=rowHeight/2;columnWidth=4096/columns;int columnHalf=columnWidth/2;
            mortarHalf=mortar/2;rowBounds=new int[rows+1];columnBounds=new int[rows][columns+1];brightness=new int[rows][columns];
            for(int row=0;row<rows;row++){
                if(row>0){int variation=(randomBound(4096,random)-2048)*verticalJitter>>12;int height=rowHeight+(variation*rowHalf>>12);rowBounds[row]=rowBounds[row-1]+height;}
                for(int column=0;column<columns;column++){
                    if(column>0){int variation=(randomBound(4096,random)-2048)*horizontalJitter>>12;int width=columnWidth+(columnHalf*variation>>12);columnBounds[row][column]=columnBounds[row][column-1]+width;}
                    brightness[row][column]=brightnessVariation<=0?4096:4096-randomBound(brightnessVariation,random);
                }
                columnBounds[row][columns]=4096;
            }
            rowBounds[rows]=4096;
        }
        int[] rgb(int x,int y,int size){
            int yFraction=(y<<12)/size+verticalPhase;while(yFraction<0)yFraction+=4096;while(yFraction>4096)yFraction-=4096;
            int rowIndex=0;while(rows>rowIndex&&yFraction>=rowBounds[rowIndex])rowIndex++;int row=rowIndex-1,rowUpper=rowBounds[rowIndex],rowLower=rowBounds[row];
            int value=0;if(rowLower+mortarHalf<yFraction&&rowUpper-mortarHalf>yFraction){
                int signedOffset=(rowIndex&1)==0?rowOffset:-rowOffset,xFraction=(x<<12)/size+(columnWidth*signedOffset>>12);
                while(xFraction<0)xFraction+=4096;while(xFraction>4096)xFraction-=4096;
                int columnIndex=0;while(columns>columnIndex&&xFraction>=columnBounds[row][columnIndex])columnIndex++;
                int column=columnIndex-1,columnUpper=columnBounds[row][columnIndex],columnLower=columnBounds[row][column];
                if(columnLower+mortarHalf<xFraction&&columnUpper-mortarHalf>xFraction)value=brightness[row][column];
            }
            return new int[]{value,value,value};
        }
    }
    private static final class BoxBlur extends Node{
        int horizontalRadius=1,verticalRadius=1,cachedSize=-1;boolean monochrome;int[][][] image;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:horizontalRadius=in.u8();break;case 1:verticalRadius=in.u8();break;case 2:monochrome=in.u8()==1;break;default:super.decode(id,code,in);}}
        int[] rgb(int x,int y,int size)throws IOException{
            if(cachedSize!=size)generate(size);if(monochrome){int value=image[0][y][x];return new int[]{value,value,value};}
            return new int[]{image[0][y][x],image[1][y][x],image[2][y][x]};
        }
        private void generate(int size)throws IOException{
            cachedSize=size;int channels=monochrome?1:3,mask=size-1;
            int horizontalWidth=horizontalRadius+horizontalRadius+1,horizontalFactor=65536/horizontalWidth;
            int verticalHeight=verticalRadius+verticalRadius+1,verticalFactor=65536/verticalHeight;
            int[][][] source=new int[channels][size][size],horizontal=new int[channels][size][size];image=new int[channels][size][size];
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){int[] pixel=children[0].rgb(x,y,size);for(int channel=0;channel<channels;channel++)source[channel][y][x]=pixel[channel];}
            for(int channel=0;channel<channels;channel++)for(int y=0;y<size;y++){
                int sum=0;for(int offset=-horizontalRadius;offset<=horizontalRadius;offset++)sum+=source[channel][y][offset&mask];
                for(int x=0;x<size;x++){
                    horizontal[channel][y][x]=horizontalFactor*sum>>16;
                    sum-=source[channel][y][x-horizontalRadius&mask];sum+=source[channel][y][horizontalRadius+x+1&mask];
                }
            }
            for(int channel=0;channel<channels;channel++)for(int x=0;x<size;x++){
                int sum=0;for(int offset=-verticalRadius;offset<=verticalRadius;offset++)sum+=horizontal[channel][offset&mask][x];
                for(int y=0;y<size;y++){
                    image[channel][y][x]=verticalFactor*sum>>16;
                    sum-=horizontal[channel][y-verticalRadius&mask][x];sum+=horizontal[channel][verticalRadius+y+1&mask][x];
                }
            }
        }
    }
    private static final class Clamp extends Node{
        int lower,upper=4096;boolean monochrome;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){if(code==0)lower=in.u16();else if(code==1)upper=in.u16();else if(code==2)monochrome=in.u8()==1;else super.decode(id,code,in);}
        int[] rgb(int x,int y,int size)throws IOException{
            if(monochrome){int value=clamp(children[0].mono(x,y,size));return new int[]{value,value,value};}
            int[] color=children[0].rgb(x,y,size);return new int[]{clamp(color[0]),clamp(color[1]),clamp(color[2])};
        }
        private int clamp(int value){return lower>value?lower:upper>=value?value:upper;}
    }
    private static final class Combine extends Node{
        int function=6;boolean monochrome;
        int childCount(){return 2;}
        void decode(int id,int code,BinaryInput in){
            if(code==0||code==1){if(in.remaining()<1)throw new UnsupportedTextureFormatException(id,"truncated operation 7 parameter "+code);int value=in.u8();if(code==0)function=value;else monochrome=value==1;}
            else super.decode(id,code,in);
        }
        void finish(int id){if(function!=1&&function!=2&&function!=3&&function!=5&&function!=6&&function!=7&&function!=8&&function!=10)throw new UnsupportedTextureFormatException(id,"combine function "+function);}
        int[] rgb(int x,int y,int size)throws IOException{
            if(monochrome){int value=apply(children[0].mono(x,y,size),children[1].mono(x,y,size));return new int[]{value,value,value};}
            int[] first=children[0].rgb(x,y,size),second=children[1].rgb(x,y,size);
            return new int[]{apply(first[0],second[0]),apply(first[1],second[1]),apply(first[2],second[2])};
        }
        private int apply(int first,int second){
            if(function==1)return second+first;
            if(function==2)return first-second;
            if(function==3)return second*first>>12;
            if(function==5)return 4096-((4096-first)*(4096-second)>>12);
            if(function==7)return first==4096?4096:(second<<12)/(4096-first);
            if(function==8)return first==0?0:4096-((4096-second<<12)/first);
            if(function==10)return first>second?first:second;
            return second>=2048?4096-((4096-first)*(4096-second)>>11):second*first>>11;
        }
    }
    private static final class Flip extends Node{
        boolean horizontal=true,vertical=true,monochrome;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){if(code==0)horizontal=in.u8()==1;else if(code==1)vertical=in.u8()==1;else if(code==2)monochrome=in.u8()==1;else super.decode(id,code,in);}
        int[] rgb(int x,int y,int size)throws IOException{
            int sampleX=horizontal?size-1-x:x,sampleY=vertical?size-1-y:y;
            if(monochrome){int value=children[0].mono(sampleX,sampleY,size);return new int[]{value,value,value};}
            return children[0].rgb(sampleX,sampleY,size);
        }
        int mono(int x,int y,int size)throws IOException{return monochrome?children[0].mono(horizontal?size-1-x:x,vertical?size-1-y:y,size):super.mono(x,y,size);}
    }
    private static final class Waveform extends Node{
        private static final int[] SINE=createSine();
        int coordinateMode,waveform,frequency=1;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){
            if(code==0||code==1||code==3){if(in.remaining()<1)throw new UnsupportedTextureFormatException(id,"truncated operation 12 parameter "+code);int value=in.u8();if(code==0)coordinateMode=value;else if(code==1)waveform=value;else frequency=value;}
            else if(code==2||code==4||code==5||code==6)return;
            else super.decode(id,code,in);
        }
        int[] rgb(int x,int y,int size){
            int xFraction=(x<<12)/size,yFraction=(y<<12)/size,phase;
            if(coordinateMode==0)phase=(xFraction-yFraction)*frequency;
            else{
                int dx=xFraction-2048>>1,dy=yFraction-2048>>1;
                int squared=dx*dx+dy*dy>>12;
                int radius=(int)(Math.sqrt((float)squared/4096.0F)*4096.0D);
                phase=(int)((double)(radius*frequency)*Math.PI);
            }
            phase-=phase&0xFFFFF000;
            if(waveform==0)phase=SINE[phase>>4&0xFF]+4096>>1;
            else if(waveform==2){phase-=2048;if(phase<0)phase=-phase;phase=2048-phase<<1;}
            return new int[]{phase,phase,phase};
        }
        private static int[] createSine(){int[] values=new int[256];for(int i=0;i<values.length;i++)values[i]=(int)(Math.sin((double)i/255.0D*6.283185307179586D)*4096.0D);return values;}
    }
    private static final class Invert extends Node{
        boolean monochrome;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){if(code==0)monochrome=in.u8()==1;else super.decode(id,code,in);}
        int[] rgb(int x,int y,int size)throws IOException{
            if(monochrome){int value=4096-children[0].mono(x,y,size);return new int[]{value,value,value};}
            int[] color=children[0].rgb(x,y,size);return new int[]{4096-color[0],4096-color[1],4096-color[2]};
        }
    }
    private static final class SpriteDependencyNode extends Node{
        final SpriteResolver resolver;int spriteId=-1;SpriteDependency sprite;
        SpriteDependencyNode(SpriteResolver resolver){this.resolver=resolver;}
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){if(code==0)spriteId=in.u16();else super.decode(id,code,in);}
        void finish(int id){if(spriteId<0)throw new UnsupportedTextureFormatException(id,"missing sprite dependency");}
        int[] rgb(int x,int y,int size)throws IOException{
            if(sprite==null){sprite=resolver.resolve(spriteId);if(sprite==null||sprite.width<=0||sprite.height<=0||sprite.pixels==null||sprite.pixels.length!=sprite.width*sprite.height)throw new UnsupportedTextureFormatException(spriteId,"invalid sprite dependency dimensions");}
            int sampleX=sprite.width==size?x:sprite.width*x/size,sampleY=sprite.height==size?y:sprite.height*y/size;
            int pixel=sprite.pixels[sampleY*sprite.width+sampleX];
            return new int[]{pixel>>12&4080,pixel>>4&4080,(pixel&255)<<4};
        }
    }
    private static final class Range extends Node{int min=1024,max=3072,range=2048;boolean monochrome;int childCount(){return 1;}void decode(int id,int code,BinaryInput in){if(code==0)min=in.u16();else if(code==1)max=in.u16();else if(code==2)monochrome=in.u8()==1;else super.decode(id,code,in);}void finish(int id){range=max-min;}int[] rgb(int x,int y,int size)throws IOException{int[] c=children[0].rgb(x,y,size);return new int[]{min+(range*c[0]>>12),min+(range*c[1]>>12),min+(range*c[2]>>12)};}}
    private static final class Curve extends Node{
        private static final int[] COSINE=createCosine();
        int mode;int[][] markers;int[] beforeMarker,afterMarker;short[] cosineValues,cubicValues;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){
            if(code!=0)super.decode(id,code,in);
            if(in.remaining()<2)throw new UnsupportedTextureFormatException(id,"truncated operation 8 parameter 0");
            mode=in.u8();int n=in.u8();if(in.remaining()<n*4)throw new UnsupportedTextureFormatException(id,"truncated operation 8 parameter 0");
            markers=new int[n][2];for(int i=0;i<n;i++){markers[i][0]=in.u16();markers[i][1]=in.u16();}
        }
        void finish(int id){
            if(mode!=0&&mode!=1&&mode!=2)throw new UnsupportedTextureFormatException(id,"curve interpolation "+mode);
            if(markers==null||markers.length<2)throw new UnsupportedTextureFormatException(id,"curve marker count");
            if(mode==1)prepareCosine();
            if(mode==2)prepareCubic();
        }
        int[] rgb(int x,int y,int size)throws IOException{
            int v=children[0].mono(x,y,size);
            if(mode==1){int index=v>>4;if(index<0)index=0;if(index>256)index=256;int result=cosineValues[index];return new int[]{result,result,result};}
            if(mode==2){int index=v>>4;if(index<0)index=0;if(index>256)index=256;int result=cubicValues[index];return new int[]{result,result,result};}
            int i=1;while(i<markers.length&&v>=markers[i][0])i++;int result;if(i==markers.length)result=markers[i-1][1];else if(i==0)result=markers[0][1];else{int[] a=markers[i-1],b=markers[i];result=a[1]+(b[1]-a[1])*(v-a[0])/Math.max(1,b[0]-a[0]);}return new int[]{result,result,result};
        }
        private void prepareCosine(){
            cosineValues=new short[257];
            for(int sample=0;sample<cosineValues.length;sample++){
                int position=sample<<4,marker=1;while(marker<markers.length-1&&markers[marker][0]<=position)marker++;
                int[] lower=markers[marker-1],upper=markers[marker];
                int fraction=(position-lower[0]<<12)/(upper[0]-lower[0]);
                int upperWeight=4096-COSINE[fraction>>5&0xFF]>>1,lowerWeight=4096-upperWeight;
                int value=upperWeight*upper[1]+lower[1]*lowerWeight>>12;
                if(value<=-32768)value=-32767;if(value>=32768)value=32767;cosineValues[sample]=(short)value;
            }
        }
        private void prepareCubic(){
            int[] first=markers[0],second=markers[1],penultimate=markers[markers.length-2],last=markers[markers.length-1];
            beforeMarker=new int[]{first[0]+first[0]-second[0],first[1]+first[1]-second[1]};
            afterMarker=new int[]{penultimate[0]+penultimate[0]-last[0],penultimate[1]+penultimate[1]-last[1]};
            cubicValues=new short[257];
            for(int sample=0;sample<cubicValues.length;sample++){
                int position=sample<<4,marker=1;while(marker<markers.length-1&&markers[marker][0]<=position)marker++;
                int[] lower=markers[marker-1],upper=markers[marker];
                int previous=getMarker(marker-2)[1],next=getMarker(marker+1)[1];
                int upperValue=upper[1],lowerValue=lower[1],slope=upperValue-previous;
                int fraction=(position-lower[0]<<12)/(upper[0]-lower[0]);
                int cross=next+lowerValue-upperValue-previous,square=fraction*fraction>>12;
                int curvature=previous-lowerValue-cross;
                int value=lowerValue+(square*curvature>>12)+(square*(fraction*cross>>12)>>12)+(slope*fraction>>12);
                if(value<=-32768)value=-32767;if(value>=32768)value=32767;cubicValues[sample]=(short)value;
            }
        }
        private int[] getMarker(int index){if(index<0)return beforeMarker;if(index>=markers.length)return afterMarker;return markers[index];}
        private static int[] createCosine(){int[] values=new int[256];for(int i=0;i<values.length;i++)values[i]=(int)(Math.cos((double)i/255.0D*6.283185307179586D)*4096.0D);return values;}
    }
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
    private static final class HslAdjust extends Node{
        int hueOffset,saturationOffset,lightnessOffset,red,green,blue,hue,saturation,lightness,cachedSize=-1;int[][][] image;boolean[] generatedRows;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){
            if(code==0){if(in.remaining()<2)throw new UnsupportedTextureFormatException(id,"truncated operation 17 parameter 0");hueOffset=in.i16();}
            else if(code==1){if(in.remaining()<1)throw new UnsupportedTextureFormatException(id,"truncated operation 17 parameter 1");saturationOffset=(in.i8()<<12)/100;}
            else if(code==2){if(in.remaining()<1)throw new UnsupportedTextureFormatException(id,"truncated operation 17 parameter 2");lightnessOffset=(in.i8()<<12)/100;}
            else super.decode(id,code,in);
        }
        int[] rgb(int x,int y,int size)throws IOException{
            if(cachedSize!=size){cachedSize=size;image=new int[3][size][size];generatedRows=new boolean[size];red=green=blue=0;}
            if(!generatedRows[y]){for(int pixel=0;pixel<size;pixel++){int[] color=children[0].rgb(pixel,y,size);adjust(color);image[0][y][pixel]=red;image[1][y][pixel]=green;image[2][y][pixel]=blue;}generatedRows[y]=true;}
            return new int[]{image[0][y][x],image[1][y][x],image[2][y][x]};
        }
        private void adjust(int[] color){toHsl(color[0],color[1],color[2]);
            lightness+=lightnessOffset;if(lightness<0)lightness=0;
            saturation+=saturationOffset;if(lightness>4096)lightness=4096;
            if(saturation<0)saturation=0;if(saturation>4096)saturation=4096;
            for(hue+=hueOffset;hue<0;hue+=4096){}while(hue>4096)hue-=4096;
            toRgb(lightness,saturation,hue);
        }
        private void toHsl(int red,int green,int blue){
            int max=red>green?red:green;if(blue>max)max=blue;
            int min=green>red?red:green;if(blue<min)min=blue;
            int delta=max-min;
            if(delta>0){
                int fromGreen=(max-green<<12)/delta,fromRed=(max-red<<12)/delta,fromBlue=(max-blue<<12)/delta;
                if(red==max)hue=min==green?fromBlue+20480:4096-fromGreen;
                else if(max==green)hue=min==blue?fromRed+4096:12288-fromBlue;
                else hue=min==red?fromGreen+12288:20480-fromRed;
                hue/=6;
            }else hue=0;
            lightness=(min+max)/2;
            if(lightness>0&&lightness<4096)saturation=(delta<<12)/(lightness>2048?8192-lightness*2:lightness*2);else saturation=0;
        }
        private void toRgb(int lightness,int saturation,int hue){
            int high=lightness<=2048?lightness*(saturation+4096)>>12:lightness+saturation-(lightness*saturation>>12);
            if(high<=0){red=green=blue=lightness;return;}
            int hue6=hue*6,low=lightness+lightness-high,sector=hue6>>12;
            int ratio=(high-low<<12)/high,remainder=hue6-(sector<<12),range=high*ratio>>12;
            int rising=(range*remainder>>12)+low,falling=high-(range*remainder>>12);
            if(sector==0){blue=low;red=high;green=rising;}
            else if(sector==1){blue=low;green=high;red=falling;}
            else if(sector==2){red=low;green=high;blue=rising;}
            else if(sector==3){red=low;green=falling;blue=high;}
            else if(sector==4){red=rising;green=low;blue=high;}
            else if(sector==5){red=high;green=low;blue=falling;}
        }
    }
    private static final class CellularNoise extends Node{
        int xScale=5,yScale=5,seed,jitter=2048,selector=2,distanceMetric=1;byte[] permutation;short[] offsets;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:xScale=yScale=in.u8();break;case 1:seed=in.u8();break;case 2:jitter=in.u16();break;case 3:selector=in.u8();break;case 4:distanceMetric=in.u8();break;case 5:xScale=in.u8();break;case 6:yScale=in.u8();break;default:super.decode(id,code,in);}}
        void finish(int id){
            permutation=permutation(seed);offsets=new short[512];Random random=new Random(seed);
            if(jitter>0)for(int i=0;i<offsets.length;i++)offsets[i]=(short)randomBound(jitter,random);
        }
        int[] rgb(int x,int y,int size){
            int scaledY=yScale*((y<<12)/size)+2048,yCell=scaledY>>12,yNext=yCell+1;
            int scaledX=xScale*((x<<12)/size)+2048,xCell=scaledX>>12,xNext=xCell+1;
            int first=Integer.MAX_VALUE,second=Integer.MAX_VALUE,third=Integer.MAX_VALUE,fourth=Integer.MAX_VALUE;
            for(int cellY=yCell-1;cellY<=yNext;cellY++){
                int yHash=permutation[(yScale<=cellY?cellY-yScale:cellY)&0xff]&0xff;
                for(int cellX=xCell-1;cellX<=xNext;cellX++){
                    int offset=(permutation[((xScale<=cellX?cellX-xScale:cellX)+yHash)&0xff]&0xff)*2;
                    int dx=scaledX-(cellX<<12)-offsets[offset],dy=scaledY-offsets[offset+1]-(cellY<<12);
                    int distance=distance(dx,dy);
                    if(distance<first){fourth=third;third=second;second=first;first=distance;}
                    else if(distance<second){fourth=third;third=second;second=distance;}
                    else if(distance<third){fourth=third;third=distance;}
                    else if(distance<fourth){fourth=distance;}
                }
            }
            int value=0;if(selector==0)value=first;else if(selector==1)value=second;else if(selector==2)value=second-first;else if(selector==3)value=third;else if(selector==4)value=fourth;
            return new int[]{value,value,value};
        }
        private int distance(int dx,int dy){
            if(distanceMetric==1)return dy*dy+dx*dx>>12;
            if(distanceMetric==2)return (dx>=0?dx:-dx)+(dy<0?-dy:dy);
            if(distanceMetric==3){dx=dx<0?-dx:dx;dy=dy>=0?dy:-dy;return dy>=dx?dy:dx;}
            if(distanceMetric==4){dx=(int)(Math.sqrt((float)(dx<0?-dx:dx)/4096.0F)*4096.0D);dy=(int)(Math.sqrt((float)(dy>=0?dy:-dy)/4096.0F)*4096.0D);int sum=dy+dx;return sum*sum>>12;}
            if(distanceMetric==5){dx*=dx;dy*=dy;return(int)(Math.sqrt(Math.sqrt((float)(dy+dx)/1.6777216E7F))*4096.0D);}
            return(int)(Math.sqrt((float)(dy*dy+dx*dx)/1.6777216E7F)*4096.0D);
        }
        private static byte[] permutation(int seed){
            Random random=new Random(seed);byte[] values=new byte[512];for(int i=0;i<255;i++)values[i]=(byte)i;
            for(int i=0;i<255;i++){int remaining=255-i,index=randomBound(remaining,random);byte value=values[index];values[index]=values[remaining];values[remaining]=values[511-i]=value;}
            return values;
        }
    }
    private static final class CoordinateDisplacement extends Node{
        private static final int[] SINE=trigonometry(true),COSINE=trigonometry(false);
        int scale=32768;boolean monochrome;
        int childCount(){return 3;}
        void decode(int id,int code,BinaryInput in){if(code==0)scale=in.u16()<<4;else if(code==1)monochrome=in.u8()==1;else super.decode(id,code,in);}
        int[] rgb(int x,int y,int size)throws IOException{
            if(monochrome){int value=monochrome(x,y,size);return new int[]{value,value,value};}
            int angle=children[1].mono(x,y,size)*255>>12&0xff,magnitude=children[2].mono(x,y,size)*scale>>12;
            int sampleX=x+(magnitude*COSINE[angle]>>12>>12)&size-1,sampleY=y+(SINE[angle]*magnitude>>12>>12)&size-1;
            return children[0].rgb(sampleX,sampleY,size);
        }
        int mono(int x,int y,int size)throws IOException{
            return monochrome?monochrome(x,y,size):super.mono(x,y,size);
        }
        private int monochrome(int x,int y,int size)throws IOException{
            int angle=children[1].mono(x,y,size)>>4&0xff,magnitude=scale*children[2].mono(x,y,size)>>12;
            int sampleX=x+(COSINE[angle]*magnitude>>12>>12)&size-1,sampleY=y+(SINE[angle]*magnitude>>12>>12)&size-1;
            return children[0].mono(sampleX,sampleY,size);
        }
        private static int[] trigonometry(boolean sine){int[] table=new int[256];for(int i=0;i<256;i++){double radians=(double)i/255.0D*6.283185307179586D;table[i]=(int)((sine?Math.sin(radians):Math.cos(radians))*4096.0D);}return table;}
    }
    private static final class Tile extends Node{
        int horizontalTiles=4,verticalTiles=4;
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){if(code==0)horizontalTiles=in.u8();else if(code==1)verticalTiles=in.u8();else super.decode(id,code,in);}
        void finish(int id){if(horizontalTiles==0||verticalTiles==0)throw new UnsupportedTextureFormatException(id,"tile grid "+horizontalTiles+"x"+verticalTiles);}
        int[] rgb(int x,int y,int size)throws IOException{
            int tileWidth=size/horizontalTiles,tileHeight=size/verticalTiles;
            int sampleX=tileWidth<=0?0:size*(x%tileWidth)/tileWidth,sampleY=tileHeight<=0?0:size*(y%tileHeight)/tileHeight;
            return children[0].rgb(sampleX,sampleY,size);
        }
    }
    private static final class Interpolate extends Node{
        boolean monochrome;
        int childCount(){return 3;}
        void decode(int id,int code,BinaryInput in){if(code==0)monochrome=in.u8()==1;else super.decode(id,code,in);}
        int[] rgb(int x,int y,int size)throws IOException{
            int control=children[2].mono(x,y,size);
            if(monochrome){int value=blend(children[0].mono(x,y,size),children[1].mono(x,y,size),control);return new int[]{value,value,value};}
            int[] first=children[0].rgb(x,y,size),second=children[1].rgb(x,y,size);
            return new int[]{blend(first[0],second[0],control),blend(first[1],second[1],control),blend(first[2],second[2],control)};
        }
        int mono(int x,int y,int size)throws IOException{return monochrome?blend(children[0].mono(x,y,size),children[1].mono(x,y,size),children[2].mono(x,y,size)):super.mono(x,y,size);}
        private int blend(int first,int second,int control){if(control==4096)return first;if(control==0)return second;return (4096-control)*second+control*first>>12;}
    }
    private static final class Stripes extends Node{
        int bands=10,dutyWidth=2048,mode;int[] starts,ends;
        int childCount(){return 0;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:bands=in.u8();break;case 1:dutyWidth=in.u16();break;case 2:mode=in.u8();break;default:super.decode(id,code,in);}}
        void finish(int id){
            if(bands<=0)throw new UnsupportedTextureFormatException(id,"stripe band count "+bands);
            starts=new int[bands+1];ends=new int[bands+1];int interval=4096/bands,filledWidth=dutyWidth*interval>>12,position=0;
            for(int band=0;band<bands;band++){starts[band]=position;ends[band]=position+filledWidth;position+=interval;}
            starts[bands]=4096;ends[bands]=ends[0]+4096;
        }
        int[] rgb(int x,int y,int size){
            int xFraction=(x<<12)/size,yFraction=(y<<12)/size,coordinate=0;
            if(mode==0)coordinate=yFraction;else if(mode==1)coordinate=xFraction;else if(mode==2)coordinate=((xFraction+yFraction-4096)>>1)+2048;else if(mode==3)coordinate=((xFraction-yFraction)>>1)+2048;
            int value=0;for(int band=0;band<bands;band++)if(starts[band]<=coordinate&&coordinate<starts[band+1]){if(coordinate<ends[band])value=4096;break;}
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
    private static final class BumpLighting extends Node{
        private static final byte[] NORMALIZATION=normalization();
        int scale=4096,horizontalAngle=3216,verticalAngle=3216;final int[] light=new int[3];
        int childCount(){return 1;}
        void decode(int id,int code,BinaryInput in){switch(code){case 0:scale=in.u16();break;case 1:horizontalAngle=in.u16();break;case 2:verticalAngle=in.u16();break;default:super.decode(id,code,in);}}
        void finish(int id){
            double verticalCosine=Math.cos((float)verticalAngle/4096.0F);
            light[0]=(int)(verticalCosine*4096.0D*Math.sin((float)horizontalAngle/4096.0F));
            light[1]=(int)(Math.cos((float)horizontalAngle/4096.0F)*verticalCosine*4096.0D);
            light[2]=(int)(Math.sin((float)verticalAngle/4096.0F)*4096.0D);
            int zSquare=light[2]*light[2]>>12,ySquare=light[1]*light[1]>>12,xSquare=light[0]*light[0]>>12;
            int magnitude=(int)(Math.sqrt(xSquare+ySquare+zSquare>>12)*4096.0D);
            if(magnitude!=0){light[2]=(light[2]<<12)/magnitude;light[0]=(light[0]<<12)/magnitude;light[1]=(light[1]<<12)/magnitude;}
        }
        int[] rgb(int x,int y,int size)throws IOException{
            int gradientScale=(size==64?2048:4096)*scale>>12,mask=size-1;
            int left=children[0].rgb(x-1&mask,y,size)[0],right=children[0].rgb(x+1&mask,y,size)[0];
            int above=children[0].rgb(x,y-1&mask,size)[0],below=children[0].rgb(x,y+1&mask,size)[0];
            int horizontal=(left-right)*gradientScale>>12,vertical=gradientScale*(below-above)>>12;
            int horizontalIndex=horizontal>>4;if(horizontalIndex<0)horizontalIndex=-horizontalIndex;if(horizontalIndex>255)horizontalIndex=255;
            int verticalIndex=vertical>>4;if(verticalIndex<0)verticalIndex=-verticalIndex;if(verticalIndex>255)verticalIndex=255;
            int normalization=NORMALIZATION[(verticalIndex*(verticalIndex+1)>>1)+horizontalIndex]&0xff;
            int normalVertical=vertical*normalization>>8,normalHorizontal=normalization*horizontal>>8,normalDepth=normalization*4096>>8;
            int value=(normalDepth*light[2]>>12)+(normalVertical*light[1]>>12)+(light[0]*normalHorizontal>>12);
            return new int[]{value,value,value};
        }
        private static byte[] normalization(){
            byte[] values=new byte[32896];int index=0;
            for(int vertical=0;vertical<256;vertical++)for(int horizontal=0;horizontal<=vertical;horizontal++)
                values[index++]=(byte)(255.0D/Math.sqrt((float)(horizontal*horizontal+vertical*vertical+65535)/65535.0F));
            return values;
        }
    }
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
