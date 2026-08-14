package com.spoiledmilk.spritebaker;

/** Clean decoder for the parallel-array material table in index 26/archive 0/file 0. */
public final class MaterialTable530Decoder {
    public MaterialDefinition530[] decode(byte[] data) {
        BinaryInput in=new BinaryInput(data);int count=in.u16();boolean[] present=new boolean[count];
        for(int i=0;i<count;i++)present[i]=in.u8()==1;
        boolean[][] flags=new boolean[4][count];
        for(int f=0;f<4;f++)for(int i=0;i<count;i++)if(present[i])flags[f][i]=in.u8()==1;
        int[][] values=new int[4][count];
        for(int v=0;v<4;v++)for(int i=0;i<count;i++)if(present[i])values[v][i]=in.i8();
        int[] colors=new int[count];for(int i=0;i<count;i++)if(present[i])colors[i]=in.u16();
        if(in.remaining()!=0)throw new IllegalArgumentException("material table has "+in.remaining()+" trailing bytes");
        MaterialDefinition530[] out=new MaterialDefinition530[count];
        for(int i=0;i<count;i++)out[i]=new MaterialDefinition530(i,present[i],flags[0][i],flags[1][i],flags[2][i],flags[3][i],values[0][i],values[1][i],values[2][i],values[3][i],colors[i]);
        return out;
    }
}
