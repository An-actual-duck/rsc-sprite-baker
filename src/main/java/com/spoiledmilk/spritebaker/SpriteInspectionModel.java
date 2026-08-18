package com.spoiledmilk.spritebaker;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** Preview-only integer zoom and background composition for the large inspector. */
public final class SpriteInspectionModel {
    private int zoom=3;
    private BufferedImage sprite;
    private Color background=new Color(96,96,96);
    private boolean mirror;
    private String label="No final preview available";

    public int zoom(){return zoom;}
    public String label(){return label;}
    public boolean available(){return sprite!=null;}
    public void zoom(int value){if(value<1||value>4)throw new IllegalArgumentException("inspection zoom must be 1..4");zoom=value;}
    public void update(BufferedImage image,Color color,boolean mirrored,String text){
        if(image==null||color==null)throw new NullPointerException();sprite=image;background=color;mirror=mirrored;label=text==null?"":text;
    }
    public void clear(String text){sprite=null;label=text==null?"":text;}
    public BufferedImage presented(){return sprite==null?null:integerScale(PreviewCompositor.over(sprite,background,mirror),zoom);}

    static BufferedImage integerScale(BufferedImage source,int zoom){
        if(source==null)throw new NullPointerException();if(zoom<1||zoom>4)throw new IllegalArgumentException("inspection zoom must be 1..4");
        BufferedImage output=new BufferedImage(source.getWidth()*zoom,source.getHeight()*zoom,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
            int pixel=source.getRGB(x,y);
            for(int dy=0;dy<zoom;dy++)for(int dx=0;dx<zoom;dx++)output.setRGB(x*zoom+dx,y*zoom+dy,pixel);
        }
        return output;
    }
}
