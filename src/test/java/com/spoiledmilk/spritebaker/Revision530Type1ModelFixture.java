package com.spoiledmilk.spritebaker;

import java.io.ByteArrayOutputStream;

/** Independently generated neutral revision-530 type-1 fixtures. */
final class Revision530Type1ModelFixture {
    private Revision530Type1ModelFixture() { }

    static byte[] oneFace(int textureType, int optionBits) {
        boolean info=(optionBits&1)!=0,priority=(optionBits&2)!=0,alpha=(optionBits&4)!=0;
        boolean faceBones=(optionBits&8)!=0,faceTextures=(optionBits&16)!=0,vertexBones=(optionBits&32)!=0;
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(textureType);                         // texture render types
        bytes(out,0,1,3);                              // vertex flags
        if(info)out.write(1);                          // flat face
        out.write(1);                                  // face-index compression type
        if(priority)out.write(0xfe);                   // signed priority
        if(faceBones)out.write(9);
        if(vertexBones)bytes(out,3,4,5);
        if(alpha)out.write(0x80);                      // signed alpha
        bytes(out,64,65,65);                           // indices 0,1,2
        if(faceTextures)bytes(out,0,1);                // texture 0 (stored +1)
        if(faceTextures)out.write(1);                  // mapping 0 (stored +1)
        bytes(out,0x12,0x34);                          // packed face color
        bytes(out,104,24);                             // X deltas +40,-40
        out.write(104);                                // Y delta +40
        if(textureType==0)shorts(out,0,1,2);
        else{
            shorts(out,0x8001,0x7fff,0xffff);          // complex P/M/N
            shorts(out,0x1234,0x8000,0xffff);          // scale X/Y/Z
            out.write(0xfe);                           // rotation
            out.write(0x81);                           // direction
            out.write(0x7f);                           // translation
            if(textureType==2)bytes(out,0x80,0x7e);    // cube U/V
        }
        shorts(out,3,1);                               // vertices, faces
        bytes(out,1,info?1:0,priority?255:7,alpha?1:0,faceBones?1:0,faceTextures?1:0,vertexBones?1:0);
        shorts(out,2,1,0,3,faceTextures?1:0);           // delta/index/coordinate lengths
        bytes(out,0xff,0xff);
        return out.toByteArray();
    }

    static byte[] complexSignature(int textureCount) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<textureCount;i++)out.write(2);
        bytes(out,0,1,3,1,64,65,65,0x12,0x34,104,24,104);
        for(int i=0;i<textureCount;i++)shorts(out,1,2,3);
        for(int i=0;i<textureCount;i++)shorts(out,4,5,6);
        for(int i=0;i<textureCount;i++)out.write(i);
        for(int i=0;i<textureCount;i++)out.write(0x80+i);
        for(int i=0;i<textureCount;i++)bytes(out,0x40+i,0x20+i,0x10+i);
        shorts(out,3,1);bytes(out,textureCount,0,0,0,0,0,0);shorts(out,2,1,0,3,0);bytes(out,0xff,0xff);
        return out.toByteArray();
    }

    static int footer(byte[] data){return data.length-23;}
    static void putU16(byte[] data,int offset,int value){data[offset]=(byte)(value>>>8);data[offset+1]=(byte)value;}
    private static void shorts(ByteArrayOutputStream out,int... values){for(int value:values)bytes(out,value>>>8,value);}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
