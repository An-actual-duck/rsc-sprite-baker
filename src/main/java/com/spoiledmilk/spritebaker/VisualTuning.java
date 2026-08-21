package com.spoiledmilk.spritebaker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Deterministic output-space color tuning shared by editor previews and export. */
public final class VisualTuning {
    private VisualTuning(){ }

    public static BufferedImage colorVariation(BufferedImage source,int[] surfaces,int[] facets,double amount){
        if(amount==1)return source;if(surfaces.length!=source.getWidth()*source.getHeight()||facets.length!=surfaces.length)throw new IllegalArgumentException("visual metadata dimensions do not match image");
        int width=source.getWidth(),height=source.getHeight();BufferedImage output=copy(source);int[] red=new int[9],green=new int[9],blue=new int[9];
        for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++){
            int index=y*width+x,center=source.getRGB(x,y);if((center>>>24)==0)continue;int count=0;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int neighbor=(y+dy)*width+x+dx;if(surfaces[neighbor]!=surfaces[index]||facets[neighbor]!=facets[index])continue;int pixel=source.getRGB(x+dx,y+dy);if((pixel>>>24)==0)continue;red[count]=(pixel>>>16)&255;green[count]=(pixel>>>8)&255;blue[count]=pixel&255;count++;}
            if(count<5)continue;Arrays.sort(red,0,count);Arrays.sort(green,0,count);Arrays.sort(blue,0,count);int middle=count/2;
            int r=scaleDeviation(red[middle],(center>>>16)&255,amount),g=scaleDeviation(green[middle],(center>>>8)&255,amount),b=scaleDeviation(blue[middle],center&255,amount);
            output.setRGB(x,y,(center&0xff000000)|(r<<16)|(g<<8)|b);
        }
        return output;
    }

    public static BufferedImage colorIntensity(BufferedImage source,double amount){
        if(amount==1)return source;BufferedImage output=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){int pixel=source.getRGB(x,y),alpha=pixel>>>24;if(alpha==0)continue;float[] hsb=Color.RGBtoHSB((pixel>>>16)&255,(pixel>>>8)&255,pixel&255,null);int rgb=Color.HSBtoRGB(hsb[0],(float)Math.min(1,hsb[1]*amount),hsb[2]);output.setRGB(x,y,(alpha<<24)|(rgb&0xffffff));}
        return output;
    }

    private static int scaleDeviation(int base,int value,double amount){return Math.max(0,Math.min(255,(int)Math.round(base+(value-base)*amount)));}
    private static BufferedImage copy(BufferedImage source){BufferedImage output=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);output.setRGB(0,0,source.getWidth(),source.getHeight(),source.getRGB(0,0,source.getWidth(),source.getHeight(),null,0,source.getWidth()),0,source.getWidth());return output;}
}
