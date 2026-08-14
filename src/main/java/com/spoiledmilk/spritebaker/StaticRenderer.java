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

    /** Legacy Phase-1 entry point. Its camera, lighting and 256px output remain fixed. */
    public BufferedImage render(List<ModelDefinition> models, NpcDefinition530 npc) {
        return render(models, npc, YAW_DEGREES, null);
    }

    /** Legacy Phase-2 entry point. */
    public BufferedImage render(List<ModelDefinition> models, NpcDefinition530 npc,
                                double yawDegrees, Viewport viewport) {
        return renderRaw(models, npc, yawDegrees, PITCH_DEGREES, viewport,
            WIDTH, HEIGHT, PADDING, 0, LIGHT_DIRECTION, AMBIENT_LIGHT, DIFFUSE_LIGHT);
    }

    /** Phase-3 output path: high-resolution raster followed by exact nearest-neighbor reduction. */
    public BufferedImage renderStyled(List<ModelDefinition> models, NpcDefinition530 npc,
                                      double baseYawDegrees, Viewport viewport,
                                      VisualSettings settings) {
        settings.validate();
        int factor = settings.supersample;
        BufferedImage highResolution = renderRaw(models, npc,
            baseYawDegrees + settings.yawOffsetDegrees, settings.pitchDegrees, viewport,
            settings.cellWidth * factor, settings.cellHeight * factor, settings.padding * factor,
            settings.verticalOffsetPixels * factor, settings.lightDirection(),
            settings.ambient, settings.diffuse);
        BufferedImage reduced = nearestNeighbor(highResolution, settings.cellWidth, settings.cellHeight, factor);
        return PaletteReducer.apply(reduced, settings);
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

    private BufferedImage renderRaw(List<ModelDefinition> models, NpcDefinition530 npc,
                                    double yawDegrees, double pitchDegrees, Viewport viewport,
                                    int width, int height, int padding, double verticalOffset,
                                    double[] lightDirection, double ambient, double diffuse) {
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
        double[] zBuffer = new double[width * height];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        vertexOffset = 0;
        for (ModelDefinition model : models) {
            rejectTextures(model);
            for (int face = 0; face < model.faceCount; face++) {
                int a = vertexOffset + model.faceIndices1[face];
                int b = vertexOffset + model.faceIndices2[face];
                int c = vertexOffset + model.faceIndices3[face];
                int rgb = litColor(recolor(model.faceColors[face], npc), model, face, npc,
                    lightDirection, ambient, diffuse);
                int alpha = model.faceTransparencies == null ? 255
                    : 255 - Byte.toUnsignedInt(model.faceTransparencies[face]);
                rasterize(image, zBuffer, screenX, screenY, depth, a, b, c,
                    (alpha << 24) | rgb, width, height);
            }
            vertexOffset += model.vertexCount;
        }
        return image;
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
        for (short texture : model.faceTextures) if (texture != -1)
            throw new UnsupportedOperationException("model " + model.id + " contains textured faces");
    }

    private static int recolor(short source, NpcDefinition530 npc) {
        for (int i = 0; i < npc.recolorFrom.length; i++) if (source == npc.recolorFrom[i])
            return Short.toUnsignedInt(npc.recolorTo[i]);
        return Short.toUnsignedInt(source);
    }

    private static int litColor(int packedHsl, ModelDefinition model, int face, NpcDefinition530 npc,
                                double[] lightDirection, double ambient, double diffuse) {
        int ia=model.faceIndices1[face],ib=model.faceIndices2[face],ic=model.faceIndices3[face];
        double ax=model.vertexX[ib]-model.vertexX[ia],ay=-(model.vertexY[ib]-model.vertexY[ia]),az=model.vertexZ[ib]-model.vertexZ[ia];
        double bx=model.vertexX[ic]-model.vertexX[ia],by=-(model.vertexY[ic]-model.vertexY[ia]),bz=model.vertexZ[ic]-model.vertexZ[ia];
        double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        double lambert=length==0?0:Math.abs((nx*lightDirection[0]+ny*lightDirection[1]+nz*lightDirection[2])/length);
        double adjustment=npc.ambient/512.0+npc.contrast/4096.0;
        double brightness=clamp(ambient+diffuse*lambert+adjustment,0.15,1.0);
        int base=packedHslToRgb(packedHsl);
        int red=(int)Math.round(((base>>>16)&255)*brightness);
        int green=(int)Math.round(((base>>>8)&255)*brightness);
        int blue=(int)Math.round((base&255)*brightness);
        return(red<<16)|(green<<8)|blue;
    }

    static int packedHslToRgb(int packed) {
        double hue=((packed>>>10)&63)/64.0,saturation=((packed>>>7)&7)/8.0,lightness=(packed&127)/128.0;
        double red,green,blue;
        if(saturation==0){red=green=blue=lightness;}else{
            double q=lightness<0.5?lightness*(1+saturation):lightness+saturation-lightness*saturation;
            double p=2*lightness-q;red=hueToRgb(p,q,hue+1.0/3.0);green=hueToRgb(p,q,hue);blue=hueToRgb(p,q,hue-1.0/3.0);
        }
        return((int)Math.round(red*255)<<16)|((int)Math.round(green*255)<<8)|(int)Math.round(blue*255);
    }
    private static double hueToRgb(double p,double q,double t){if(t<0)t+=1;if(t>1)t-=1;if(t<1.0/6.0)return p+(q-p)*6*t;if(t<.5)return q;if(t<2.0/3.0)return p+(q-p)*(2.0/3.0-t)*6;return p;}

    private static void rasterize(BufferedImage image,double[] zBuffer,double[] x,double[] y,double[] z,
                                  int a,int b,int c,int argb,int width,int height){
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
            if(pixelDepth>zBuffer[index]){zBuffer[index]=pixelDepth;image.setRGB(px,py,argb);}
        }
    }
    private static double edge(double ax,double ay,double bx,double by,double px,double py){return(px-ax)*(by-ay)-(py-ay)*(bx-ax);}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
