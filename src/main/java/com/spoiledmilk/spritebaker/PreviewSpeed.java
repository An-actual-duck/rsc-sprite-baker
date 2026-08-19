package com.spoiledmilk.spritebaker;

/** Ephemeral final-preview rate; it never participates in source or export timing. */
public enum PreviewSpeed {
    HALF("0.5×",1,2), THREE_QUARTER("0.75×",3,4), NORMAL("1×",1,1);

    public static final PreviewSpeed DEFAULT=HALF;
    private final String label;
    private final int numerator,denominator;

    PreviewSpeed(String label,int numerator,int denominator){this.label=label;this.numerator=numerator;this.denominator=denominator;}

    public long animationMillis(long realMillis){return scale(Math.max(0,realMillis),numerator,denominator);}
    public long realMillis(long animationMillis){return scale(Math.max(0,animationMillis),denominator,numerator);}
    @Override public String toString(){return label;}

    private static long scale(long value,int numerator,int denominator){
        return value/denominator*numerator+(value%denominator)*numerator/denominator;
    }
}
