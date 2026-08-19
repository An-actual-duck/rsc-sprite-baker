package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Deterministic, headless, untextured triangle renderer. */
public final class StaticRenderer {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 256;
    public static final int PADDING = 16;
    public static final double YAW_DEGREES = 25.0;
    public static final double PITCH_DEGREES = 15.0;
    public static final double[] LIGHT_DIRECTION = {-0.35, 0.65, -0.68};
    public static final double AMBIENT_LIGHT = 0.45;
    public static final double DIFFUSE_LIGHT = 0.55;
    /** Revision-530's default display preference is brightness 3, which selects exponent 0.7. */
    static final double PINNED_DEFAULT_BRIGHTNESS = 0.7;

    /** Legacy Phase-1 entry point. Its camera, lighting and 256px output remain fixed. */
    public BufferedImage render(List<ModelDefinition> models, NpcDefinition530 npc) {
        return render(models, npc, YAW_DEGREES, null);
    }

    /** Legacy Phase-2 entry point. */
    public BufferedImage render(List<ModelDefinition> models, NpcDefinition530 npc,
                                double yawDegrees, Viewport viewport) {
        return renderRaw(models, npc, yawDegrees, PITCH_DEGREES, viewport,
            WIDTH, HEIGHT, PADDING, 0, LIGHT_DIRECTION, AMBIENT_LIGHT, DIFFUSE_LIGHT, null).image;
    }

    /** Phase-3 output path: high-resolution raster followed by exact nearest-neighbor reduction. */
    public BufferedImage renderStyled(List<ModelDefinition> models, NpcDefinition530 npc,
                                      double baseYawDegrees, Viewport viewport,
                                      VisualSettings settings) {
        settings.validate();
        int factor = settings.supersample;
        RasterFrame highResolution = renderRaw(models, npc,
            baseYawDegrees + settings.yawOffsetDegrees, settings.pitchDegrees, viewport,
            settings.cellWidth * factor, settings.cellHeight * factor, settings.padding * factor,
            settings.verticalOffsetPixels * factor, settings.lightDirection(),
            settings.ambient, settings.diffuse, null);
        BufferedImage reduced = MaterialStylizer.RSC_RAMPS.equals(settings.materialStyle)
            ? MaterialStylizer.reduce(highResolution, settings.cellWidth, settings.cellHeight, factor)
            : nearestNeighbor(highResolution.image, settings.cellWidth, settings.cellHeight, factor);
        return PaletteReducer.apply(reduced, settings);
    }

    /** Phase-4 path. Existing overloads retain their fail-closed untextured behavior. */
    public BufferedImage renderStyled(List<ModelDefinition> models,NpcDefinition530 npc,
                                      double baseYawDegrees,Viewport viewport,VisualSettings settings,
                                      MaterialProvider530 materials) {
        settings.validate();int factor=settings.supersample;
        RasterFrame highResolution=renderRaw(models,npc,baseYawDegrees+settings.yawOffsetDegrees,
            settings.pitchDegrees,viewport,settings.cellWidth*factor,settings.cellHeight*factor,
            settings.padding*factor,settings.verticalOffsetPixels*factor,settings.lightDirection(),
            settings.ambient,settings.diffuse,materials);
        BufferedImage reduced=MaterialStylizer.RSC_RAMPS.equals(settings.materialStyle)
            ?MaterialStylizer.reduce(highResolution,settings.cellWidth,settings.cellHeight,factor)
            :nearestNeighbor(highResolution.image,settings.cellWidth,settings.cellHeight,factor);
        return PaletteReducer.apply(reduced,settings);
    }

    /** Shared orthographic framing for a legacy 256px sheet. */
    public Viewport fit(List<View> views, NpcDefinition530 npc) {
        return fitRaw(views, npc, WIDTH, HEIGHT, PADDING, PITCH_DEGREES, 0, 1);
    }

    /** Shared framing in supersampled pixels; every one of the 18 cells reuses this object. */
    public Viewport fitStyled(List<View> views, NpcDefinition530 npc, VisualSettings settings) {
        settings.validate();
        int factor = settings.supersample;
        return fitRaw(views, npc, settings.cellWidth * factor, settings.cellHeight * factor,
            settings.padding * factor, settings.pitchDegrees, settings.yawOffsetDegrees,
            settings.modelScale);
    }

    private RasterFrame renderRaw(List<ModelDefinition> models, NpcDefinition530 npc,
                                    double yawDegrees, double pitchDegrees, Viewport viewport,
                                    int width, int height, int padding, double verticalOffset,
                                    double[] lightDirection, double ambient, double diffuse,
                                    MaterialProvider530 materials) {
        if (models.isEmpty()) throw new IllegalArgumentException("at least one model is required");
        int totalVertices = models.stream().mapToInt(model -> model.vertexCount).sum();
        double[] projectedX = new double[totalVertices];
        double[] projectedY = new double[totalVertices];
        double[] depth = new double[totalVertices];
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        int vertexOffset = 0;
        for (ModelDefinition model : models) {
            for (int i = 0; i < model.vertexCount; i++) {
                double x = model.vertexX[i] * npc.widthScale / 128.0;
                double up = -model.vertexY[i] * npc.heightScale / 128.0;
                double z = model.vertexZ[i] * npc.widthScale / 128.0;
                double cameraX = Math.cos(yaw) * x + Math.sin(yaw) * z;
                double cameraDepth = -Math.sin(yaw) * x + Math.cos(yaw) * z;
                projectedX[vertexOffset + i] = cameraX;
                projectedY[vertexOffset + i] = up * Math.cos(pitch) - cameraDepth * Math.sin(pitch);
                depth[vertexOffset + i] = up * Math.sin(pitch) + cameraDepth * Math.cos(pitch);
            }
            vertexOffset += model.vertexCount;
        }
        double minX = Arrays.stream(projectedX).min().orElseThrow();
        double maxX = Arrays.stream(projectedX).max().orElseThrow();
        double minY = Arrays.stream(projectedY).min().orElseThrow();
        double maxY = Arrays.stream(projectedY).max().orElseThrow();
        double scale = viewport == null ? Math.min(
            (width - padding * 2.0) / Math.max(1.0, maxX - minX),
            (height - padding * 2.0) / Math.max(1.0, maxY - minY)) : viewport.scale;
        double centerX = viewport == null ? (minX + maxX) / 2.0 : viewport.centerX;
        double groundY = viewport == null ? minY : viewport.groundY;
        double[] screenX = new double[totalVertices];
        double[] screenY = new double[totalVertices];
        for (int i = 0; i < totalVertices; i++) {
            screenX[i] = width / 2.0 + (projectedX[i] - centerX) * scale;
            screenY[i] = height - padding - (projectedY[i] - groundY) * scale - verticalOffset;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[width * height];
        int[] facets = new int[width * height];
        double[] lighting = new double[width * height];
        double[] zBuffer = new double[width * height];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        vertexOffset = 0;int facetOffset=1;
        for (ModelDefinition model : models) {
            if(materials==null)rejectTextures(model);
            for (int face = 0; face < model.faceCount; face++) {
                // RawModel face render type 2 becomes SoftwareModel triangleInfo -2 and is
                // excluded from the pinned client's draw list. These are structural marker
                // faces, not white geometry, even when their packed color is 65535.
                if (hiddenFace(model,face)) continue;
                int a = vertexOffset + model.faceIndices1[face];
                int b = vertexOffset + model.faceIndices2[face];
                int c = vertexOffset + model.faceIndices3[face];
                int alpha = model.faceTransparencies == null ? 255
                    : 255 - Byte.toUnsignedInt(model.faceTransparencies[face]);
                int texture=texture(model,face,npc);
                double brightness=faceBrightness(model,face,npc,lightDirection,ambient,diffuse);
                if(texture==-1){
                    int packedColor=recolor(model.faceColors[face],npc);
                    int rgb = litColor(packedColor,brightness);
                    rasterize(image,zBuffer,surfaces,facets,lighting,screenX,screenY,depth,a,b,c,
                        (alpha<<24)|rgb,0x10000+packedColor,facetOffset+face,brightness,width,height);
                }else{
                    try{
                        TextureMaterial530 material=materials.material(texture);double[] uv=textureCoordinates(model,face);
                        int modulation=texturedModulation(recolor(model.faceColors[face],npc),material.definition,brightness);
                        rasterizeTextured(image,zBuffer,surfaces,facets,lighting,screenX,screenY,depth,a,b,c,
                            uv,material,modulation,alpha,0x20000+texture,facetOffset+face,brightness,width,height);
                    }catch(java.io.IOException exception){throw new IllegalArgumentException("cannot load texture "+texture,exception);}
                }
            }
            vertexOffset += model.vertexCount;facetOffset+=model.faceCount;
        }
        return new RasterFrame(image,surfaces,facets,lighting);
    }

    private Viewport fitRaw(List<View> views, NpcDefinition530 npc, int width, int height,
                            int padding, double pitchDegrees, double yawOffsetDegrees,
                            double modelScale) {
        if (views.isEmpty()) throw new IllegalArgumentException("at least one view is required");
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
        double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
        double pitch=Math.toRadians(pitchDegrees);
        for(View view:views) {
            double yaw=Math.toRadians(view.yawDegrees + yawOffsetDegrees);
            ModelDefinition model=view.model;
            for(int i=0;i<model.vertexCount;i++) {
                double x=model.vertexX[i]*npc.widthScale/128.0;
                double up=-model.vertexY[i]*npc.heightScale/128.0;
                double z=model.vertexZ[i]*npc.widthScale/128.0;
                double projected=Math.cos(yaw)*x+Math.sin(yaw)*z;
                double depth=-Math.sin(yaw)*x+Math.cos(yaw)*z;
                double projectedY=up*Math.cos(pitch)-depth*Math.sin(pitch);
                minX=Math.min(minX,projected);maxX=Math.max(maxX,projected);
                minY=Math.min(minY,projectedY);maxY=Math.max(maxY,projectedY);
            }
        }
        double extent=Math.max(Math.abs(minX),Math.abs(maxX));
        double scale=Math.min((width-padding*2.0)/Math.max(1.0,extent*2),
            (height-padding*2.0)/Math.max(1.0,maxY-minY)) * modelScale;
        return new Viewport(scale,0,minY);
    }

    static BufferedImage nearestNeighbor(BufferedImage source, int width, int height, int factor) {
        if (source.getWidth() != width * factor || source.getHeight() != height * factor) {
            throw new IllegalArgumentException("source dimensions do not match integer reduction factor");
        }
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int offset = factor / 2;
        for (int y=0;y<height;y++) for(int x=0;x<width;x++)
            output.setRGB(x,y,source.getRGB(x*factor+offset,y*factor+offset));
        return output;
    }

    public static final class View {
        public final ModelDefinition model; public final double yawDegrees;
        public View(ModelDefinition model,double yawDegrees){this.model=model;this.yawDegrees=yawDegrees;}
    }
    public static final class Viewport {
        public final double scale,centerX,groundY;
        public Viewport(double scale,double centerX,double groundY){this.scale=scale;this.centerX=centerX;this.groundY=groundY;}
    }

    private static void rejectTextures(ModelDefinition model) {
        if (model.faceTextures == null) return;
        for (int face = 0; face < model.faceCount; face++) {
            if (!hiddenFace(model, face) && model.faceTextures[face] != -1) {
                throw new UnsupportedOperationException("model " + model.id + " contains textured faces");
            }
        }
    }

    static boolean hiddenFace(ModelDefinition model, int face) {
        return model.faceRenderTypes != null && model.faceRenderTypes[face] == 2;
    }

    private static int recolor(short source, NpcDefinition530 npc) {
        for (int i = 0; i < npc.recolorFrom.length; i++) if (source == npc.recolorFrom[i])
            return Short.toUnsignedInt(npc.recolorTo[i]);
        return Short.toUnsignedInt(source);
    }

    private static int litColor(int packedHsl, ModelDefinition model, int face, NpcDefinition530 npc,
                                double[] lightDirection, double ambient, double diffuse) {
        double brightness=faceBrightness(model,face,npc,lightDirection,ambient,diffuse);
        return litColor(packedHsl,brightness);
    }
    private static int litColor(int packedHsl,double brightness){
        int base=packedHslToRgb(packedHsl);
        int red=(int)Math.round(((base>>>16)&255)*brightness);
        int green=(int)Math.round(((base>>>8)&255)*brightness);
        int blue=(int)Math.round((base&255)*brightness);
        return(red<<16)|(green<<8)|blue;
    }
    private static double faceBrightness(ModelDefinition model,int face,NpcDefinition530 npc,
                                         double[] lightDirection,double ambient,double diffuse){
        int ia=model.faceIndices1[face],ib=model.faceIndices2[face],ic=model.faceIndices3[face];
        double ax=model.vertexX[ib]-model.vertexX[ia],ay=-(model.vertexY[ib]-model.vertexY[ia]),az=model.vertexZ[ib]-model.vertexZ[ia];
        double bx=model.vertexX[ic]-model.vertexX[ia],by=-(model.vertexY[ic]-model.vertexY[ia]),bz=model.vertexZ[ic]-model.vertexZ[ia];
        double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        double lambert=length==0?0:Math.abs((nx*lightDirection[0]+ny*lightDirection[1]+nz*lightDirection[2])/length);
        double adjustment=npc.ambient/512.0+npc.contrast/4096.0;
        return clamp(ambient+diffuse*lambert+adjustment,0.15,1.0);
    }

    private static int texture(ModelDefinition model,int face,NpcDefinition530 npc){
        if(model.faceTextures==null||model.faceTextures[face]==-1)return-1;short source=model.faceTextures[face];
        for(int i=0;i<npc.retextureFrom.length;i++)if(source==npc.retextureFrom[i])return Short.toUnsignedInt(npc.retextureTo[i]);
        return Short.toUnsignedInt(source);
    }
    /** u0,v0,u1,v1,u2,v2. Revision-530 type 1-3 records use the pinned GL mapping equations. */
    static double[] textureCoordinates(ModelDefinition m,int face){
        if(m.textureCoords==null||m.textureCoords[face]==-1)return new double[]{0,0,1,0,0,1};
        int t=Byte.toUnsignedInt(m.textureCoords[face]);
        if(t>=m.numTextureFaces||m.textureRenderTypes==null||m.texIndices1==null)throw new IllegalArgumentException("model "+m.id+" face "+face+" has invalid texture mapping "+t);
        if(m.textureRenderTypes[t]!=0){if(m instanceof ModelDefinition530)return Revision530TextureMapping.coordinates((ModelDefinition530)m,face,t);return new double[]{0,0,1,0,0,1};}
        int ta=Short.toUnsignedInt(m.texIndices1[t]),tb=Short.toUnsignedInt(m.texIndices2[t]),tc=Short.toUnsignedInt(m.texIndices3[t]);
        if(ta>=m.vertexCount||tb>=m.vertexCount||tc>=m.vertexCount)throw new IllegalArgumentException("model "+m.id+" texture face "+t+" has invalid vertex mapping");
        double ax=m.vertexX[ta],ay=m.vertexY[ta],az=m.vertexZ[ta],bx=m.vertexX[tb]-ax,by=m.vertexY[tb]-ay,bz=m.vertexZ[tb]-az,cx=m.vertexX[tc]-ax,cy=m.vertexY[tc]-ay,cz=m.vertexZ[tc]-az;
        double nx=by*cz-bz*cy,ny=bz*cx-bx*cz,nz=bx*cy-by*cx;
        double ux=cy*nz-cz*ny,uy=cz*nx-cx*nz,uz=cx*ny-cy*nx,ud=ux*bx+uy*by+uz*bz;
        double vx=by*nz-bz*ny,vy=bz*nx-bx*nz,vz=bx*ny-by*nx,vd=vx*cx+vy*cy+vz*cz;
        if(Math.abs(ud)<1e-9||Math.abs(vd)<1e-9)return new double[]{0,0,1,0,0,1};double[] out=new double[6];int[] faces={m.faceIndices1[face],m.faceIndices2[face],m.faceIndices3[face]};
        for(int i=0;i<3;i++){double x=m.vertexX[faces[i]]-ax,y=m.vertexY[faces[i]]-ay,z=m.vertexZ[faces[i]]-az;out[i*2]=(ux*x+uy*y+uz*z)/ud;out[i*2+1]=(vx*x+vy*y+vz*z)/vd;}return out;
    }

    static int packedHslToRgb(int packed) {
        // Rasteriser.calculateBrightness(), without its deliberately random +/-0.015 display perturbation.
        double hue=((packed>>>10)&63)/64.0+0.0078125,saturation=((packed>>>7)&7)/8.0+0.0625,lightness=(packed&127)/128.0;
        double red,green,blue;
        if(saturation==0){red=green=blue=lightness;}else{
            double q=lightness<0.5?lightness*(1+saturation):lightness+saturation-lightness*saturation;
            double p=2*lightness-q;red=hueToRgb(p,q,hue+1.0/3.0);green=hueToRgb(p,q,hue);blue=hueToRgb(p,q,hue-1.0/3.0);
        }
        int rgb=((int)(Math.pow(red,PINNED_DEFAULT_BRIGHTNESS)*256)<<16)
            |((int)(Math.pow(green,PINNED_DEFAULT_BRIGHTNESS)*256)<<8)
            |(int)(Math.pow(blue,PINNED_DEFAULT_BRIGHTNESS)*256);
        return rgb==0?1:rgb;
    }
    /** Pinned GlModel.method4096 face color and material-byte modulation. */
    static int texturedModulation(int packedHsl,MaterialDefinition530 material,double brightness){
        int light=(int)Math.round(brightness*128.0),lit=packedHslToRgb(multiplyLightness(packedHsl,light));
        int grayscaleBlend=material.grayscaleBlend();
        if(grayscaleBlend!=0){
            int gray=light<0?0:light>127?0xffffff:light*0x020202;
            if(grayscaleBlend==256)lit=gray;
            else{
                int keep=256-grayscaleBlend;
                int red=(((gray>>>16)&255)*grayscaleBlend+((lit>>>16)&255)*keep)>>8;
                int green=(((gray>>>8)&255)*grayscaleBlend+((lit>>>8)&255)*keep)>>8;
                int blue=((gray&255)*grayscaleBlend+(lit&255)*keep)>>8;
                lit=(red<<16)|(green<<8)|blue;
            }
        }
        int boost=material.colorBoost();
        if(boost!=0){
            int scale=boost+256;
            int red=Math.min(65535,((lit>>>16)&255)*scale);
            int green=Math.min(65535,((lit>>>8)&255)*scale);
            int blue=Math.min(65535,(lit&255)*scale);
            lit=((red&0xff00)<<8)|(green&0xff00)|(blue>>>8);
        }
        return lit;
    }
    private static int multiplyLightness(int packedHsl,int light){int value=light*(packedHsl&127)>>7;if(value<2)value=2;else if(value>126)value=126;return(packedHsl&0xff80)+value;}
    private static double hueToRgb(double p,double q,double t){if(t<0)t+=1;if(t>1)t-=1;if(t<1.0/6.0)return p+(q-p)*6*t;if(t<.5)return q;if(t<2.0/3.0)return p+(q-p)*(2.0/3.0-t)*6;return p;}

    private static void rasterize(BufferedImage image,double[] zBuffer,int[] surfaces,int[] facets,double[] lighting,
                                  double[] x,double[] y,double[] z,int a,int b,int c,int argb,int surface,
                                  int facet,double brightness,int width,int height){
        double area=edge(x[a],y[a],x[b],y[b],x[c],y[c]);if(Math.abs(area)<.00001||(argb>>>24)==0)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(x[a],Math.min(x[b],x[c]))));
        int maxX=Math.min(width-1,(int)Math.ceil(Math.max(x[a],Math.max(x[b],x[c]))));
        int minY=Math.max(0,(int)Math.floor(Math.min(y[a],Math.min(y[b],y[c]))));
        int maxY=Math.min(height-1,(int)Math.ceil(Math.max(y[a],Math.max(y[b],y[c]))));
        for(int py=minY;py<=maxY;py++)for(int px=minX;px<=maxX;px++){
            double sx=px+.5,sy=py+.5,wa=edge(x[b],y[b],x[c],y[c],sx,sy)/area;
            double wb=edge(x[c],y[c],x[a],y[a],sx,sy)/area,wc=1-wa-wb;
            if(wa<-.000001||wb<-.000001||wc<-.000001)continue;
            double pixelDepth=wa*z[a]+wb*z[b]+wc*z[c];int index=py*width+px;
            if(pixelDepth>zBuffer[index]){zBuffer[index]=pixelDepth;image.setRGB(px,py,argb);surfaces[index]=surface;facets[index]=facet;lighting[index]=brightness;}
        }
    }
    private static void rasterizeTextured(BufferedImage image,double[] zBuffer,int[] surfaces,int[] facets,double[] lighting,
            double[] x,double[] y,double[] z,int a,int b,int c,double[] uv,TextureMaterial530 material,
            int modulation,int faceAlpha,int surface,int facet,double brightness,int width,int height){
        double area=edge(x[a],y[a],x[b],y[b],x[c],y[c]);if(Math.abs(area)<.00001||faceAlpha==0)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(x[a],Math.min(x[b],x[c])))),maxX=Math.min(width-1,(int)Math.ceil(Math.max(x[a],Math.max(x[b],x[c]))));
        int minY=Math.max(0,(int)Math.floor(Math.min(y[a],Math.min(y[b],y[c])))),maxY=Math.min(height-1,(int)Math.ceil(Math.max(y[a],Math.max(y[b],y[c]))));
        for(int py=minY;py<=maxY;py++)for(int px=minX;px<=maxX;px++){double sx=px+.5,sy=py+.5,wa=edge(x[b],y[b],x[c],y[c],sx,sy)/area,wb=edge(x[c],y[c],x[a],y[a],sx,sy)/area,wc=1-wa-wb;if(wa<-.000001||wb<-.000001||wc<-.000001)continue;
            double pd=wa*z[a]+wb*z[b]+wc*z[c];int index=py*width+px;if(pd<=zBuffer[index])continue;double u=wa*uv[0]+wb*uv[2]+wc*uv[4],v=wa*uv[1]+wb*uv[3]+wc*uv[5];int tx=Math.floorMod((int)Math.floor(u*material.size),material.size),ty=Math.floorMod((int)Math.floor(v*material.size),material.size);int texel=material.pixels[ty*material.size+tx];if(!material.definition.opaque&&(texel&0xffffff)==0)continue;
            int r=((texel>>>16)&255)*((modulation>>>16)&255)/255,g=((texel>>>8)&255)*((modulation>>>8)&255)/255,bl=(texel&255)*(modulation&255)/255;int src=(faceAlpha<<24)|(r<<16)|(g<<8)|bl;
            if(faceAlpha==255)image.setRGB(px,py,src);else image.setRGB(px,py,blend(src,image.getRGB(px,py)));zBuffer[index]=pd;surfaces[index]=surface;facets[index]=facet;lighting[index]=brightness;
        }
    }
    private static int blend(int src,int dst){int a=src>>>24,inv=255-a;int r=(((src>>>16)&255)*a+((dst>>>16)&255)*inv+127)/255,g=(((src>>>8)&255)*a+((dst>>>8)&255)*inv+127)/255,b=((src&255)*a+(dst&255)*inv+127)/255,oa=a+((dst>>>24)*inv+127)/255;return(oa<<24)|(r<<16)|(g<<8)|b;}
    private static double edge(double ax,double ay,double bx,double by,double px,double py){return(px-ax)*(by-ay)-(py-ay)*(bx-ax);}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}

    static final class RasterFrame {
        final BufferedImage image;
        final int[] surfaces;
        final int[] facets;
        final double[] lighting;
        RasterFrame(BufferedImage image,int[] surfaces){this(image,surfaces,surfaces,uniformLighting(surfaces.length));}
        RasterFrame(BufferedImage image,int[] surfaces,int[] facets,double[] lighting){this.image=image;this.surfaces=surfaces;this.facets=facets;this.lighting=lighting;}
        private static double[] uniformLighting(int size){double[] values=new double[size];Arrays.fill(values,.72);return values;}
    }
}
