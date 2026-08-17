package com.spoiledmilk.spritebaker;

/** Pinned revision-530 GL texture-coordinate generation for render types 1-3. */
final class Revision530TextureMapping {
    private static final float TAU=6.2831855F,PI=3.1415927F,ROTATION_UNIT=0.024543693F;
    private Revision530TextureMapping() { }

    static double[] coordinates(ModelDefinition530 model,int face,int texture){
        require(model,texture);
        int type=Byte.toUnsignedInt(model.textureRenderTypes[texture]);
        if(type<1||type>3)throw new IllegalArgumentException("texture face "+texture+" has unsupported render type "+type);
        int[] center=center(model,texture);
        float sx,sy,sz;
        short rawX=model.textureScaleX[texture];
        if(type==1){
            if(rawX==0){sx=1F;sz=1F;}else if(rawX>0){sx=1F;sz=(float)rawX/1024F;}else{sz=1F;sx=(float)-rawX/1024F;}
            sy=64F/(float)Short.toUnsignedInt(model.textureScaleY[texture]);
        }else if(type==2){
            sx=64F/(float)Short.toUnsignedInt(model.textureScaleX[texture]);
            sy=64F/(float)Short.toUnsignedInt(model.textureScaleY[texture]);
            sz=64F/(float)Short.toUnsignedInt(model.textureScaleZ[texture]);
        }else{
            sx=(float)model.textureScaleX[texture]/1024F;
            sy=(float)model.textureScaleY[texture]/1024F;
            sz=(float)model.textureScaleZ[texture]/1024F;
        }
        float[] matrix=matrix(model.texIndices1[texture],model.texIndices2[texture],model.texIndices3[texture],Byte.toUnsignedInt(model.textureRotation[texture]),sx,sy,sz);
        int[] vertices={model.faceIndices1[face],model.faceIndices2[face],model.faceIndices3[face]};
        float[] uv=new float[6];int direction=model.textureDirection[texture];float translation=(float)model.textureTranslation[texture]/256F;
        if(type==1){
            float period=(float)Short.toUnsignedInt(model.textureScaleZ[texture])/1024F;
            for(int i=0;i<3;i++){float[] pair=cylindrical(model,vertices[i],center,matrix,period,direction,translation);uv[i*2]=pair[0];uv[i*2+1]=pair[1];}
            wrap(uv,period,direction);
        }else if(type==2){
            int a=vertices[0],b=vertices[1],c=vertices[2];
            int abx=model.vertexX[b]-model.vertexX[a],aby=model.vertexY[b]-model.vertexY[a],abz=model.vertexZ[b]-model.vertexZ[a];
            int acx=model.vertexX[c]-model.vertexX[a],acy=model.vertexY[c]-model.vertexY[a],acz=model.vertexZ[c]-model.vertexZ[a];
            int nx=aby*acz-acy*abz,ny=abz*acx-acz*abx,nz=abx*acy-acx*aby;
            float px=((float)nx*matrix[0]+(float)ny*matrix[1]+(float)nz*matrix[2])/(64F/(float)Short.toUnsignedInt(model.textureScaleX[texture]));
            float py=((float)nx*matrix[3]+(float)ny*matrix[4]+(float)nz*matrix[5])/(64F/(float)Short.toUnsignedInt(model.textureScaleY[texture]));
            float pz=((float)nx*matrix[6]+(float)ny*matrix[7]+(float)nz*matrix[8])/(64F/(float)Short.toUnsignedInt(model.textureScaleZ[texture]));
            int projection=dominant(px,py,pz);float cubeU=(float)model.textureCubeU[texture]/256F,cubeV=(float)model.textureCubeV[texture]/256F;
            for(int i=0;i<3;i++){float[] pair=cube(model,vertices[i],center,projection,matrix,direction,translation,cubeU,cubeV);uv[i*2]=pair[0];uv[i*2+1]=pair[1];}
        }else{
            for(int i=0;i<3;i++){float[] pair=spherical(model,vertices[i],center,matrix,direction,translation);uv[i*2]=pair[0];uv[i*2+1]=pair[1];}
            wrap(uv,1F,direction);
        }
        return new double[]{uv[0],uv[1],uv[2],uv[3],uv[4],uv[5]};
    }

    private static void require(ModelDefinition530 model,int texture){
        if(model.textureScaleX==null||model.textureScaleY==null||model.textureScaleZ==null
            ||model.textureRotation==null||model.textureDirection==null||model.textureTranslation==null)
            throw new IllegalArgumentException("texture face "+texture+" is missing revision-530 complex mapping data");
    }
    private static int[] center(ModelDefinition530 model,int texture){
        int minX=Integer.MAX_VALUE,maxX=-2147483647,minY=Integer.MAX_VALUE,maxY=-2147483647,minZ=Integer.MAX_VALUE,maxZ=-2147483647;boolean found=false;
        for(int face=0;face<model.faceCount;face++)if(model.textureCoords!=null&&model.textureCoords[face]!=-1&&Byte.toUnsignedInt(model.textureCoords[face])==texture){
            int[] vs={model.faceIndices1[face],model.faceIndices2[face],model.faceIndices3[face]};
            for(int v:vs){found=true;int x=model.vertexX[v],y=model.vertexY[v],z=model.vertexZ[v];minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}
        }
        if(!found)throw new IllegalArgumentException("texture face "+texture+" is not referenced by any face");
        return new int[]{(minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2};
    }
    private static float[] matrix(int p,int m,int n,int rotation,float sx,float sy,float sz){
        float cos=(float)Math.cos((float)rotation*ROTATION_UNIT),sin=(float)Math.sin((float)rotation*ROTATION_UNIT);
        float[] yaw={cos,0F,sin,0F,1F,0F,-sin,0F,cos};float[] aligned=new float[9];
        float axisY=(float)m/32767F,axisSin=-((float)Math.sqrt(1F-axisY*axisY)),oneMinus=1F-axisY;
        float length=(float)Math.sqrt(p*p+n*n),axisX=1F,axisZ=0F;
        if(length==0F&&axisY==0F)aligned=yaw;
        else{
            if(length!=0F){axisX=(float)-n/length;axisZ=(float)p/length;}
            float[] tilt={axisY+axisX*axisX*oneMinus,axisZ*axisSin,axisZ*axisX*oneMinus,-axisZ*axisSin,axisY,axisX*axisSin,axisX*axisZ*oneMinus,-axisX*axisSin,axisY+axisZ*axisZ*oneMinus};
            for(int row=0;row<3;row++)for(int col=0;col<3;col++)aligned[row*3+col]=yaw[row*3]*tilt[col]+yaw[row*3+1]*tilt[3+col]+yaw[row*3+2]*tilt[6+col];
        }
        for(int i=0;i<3;i++)aligned[i]*=sx;for(int i=3;i<6;i++)aligned[i]*=sy;for(int i=6;i<9;i++)aligned[i]*=sz;return aligned;
    }
    private static float[] cylindrical(ModelDefinition530 m,int v,int[] c,float[] a,float period,int direction,float translation){
        float[] q=transform(m,v,c,a);float u=(float)Math.atan2(q[0],q[2])/TAU+0.5F;if(period!=1F)u*=period;float w=q[1]+translation+0.5F;return rotate(u,w,direction);
    }
    private static float[] spherical(ModelDefinition530 m,int v,int[] c,float[] a,int direction,float translation){
        float[] q=transform(m,v,c,a);float length=(float)Math.sqrt(q[0]*q[0]+q[1]*q[1]+q[2]*q[2]);float u=(float)Math.atan2(q[0],q[2])/TAU+0.5F,w=(float)Math.asin(q[1]/length)/PI+translation+0.5F;return rotate(u,w,direction);
    }
    private static float[] cube(ModelDefinition530 m,int v,int[] c,int projection,float[] a,int direction,float translation,float cubeU,float cubeV){
        float[] q=transform(m,v,c,a);float u,w;
        if(projection==0){u=q[0]+translation+0.5F;w=cubeV+0.5F-q[2];}
        else if(projection==1){u=q[0]+translation+0.5F;w=q[2]+cubeV+0.5F;}
        else if(projection==2){u=translation+0.5F-q[0];w=cubeU+0.5F-q[1];}
        else if(projection==3){u=q[0]+translation+0.5F;w=cubeU+0.5F-q[1];}
        else if(projection==4){u=q[2]+cubeV+0.5F;w=cubeU+0.5F-q[1];}
        else{u=cubeV+0.5F-q[2];w=cubeU+0.5F-q[1];}
        return rotate(u,w,direction);
    }
    private static float[] transform(ModelDefinition530 m,int v,int[] c,float[] a){int x=m.vertexX[v]-c[0],y=m.vertexY[v]-c[1],z=m.vertexZ[v]-c[2];return new float[]{(float)x*a[0]+(float)y*a[1]+(float)z*a[2],(float)x*a[3]+(float)y*a[4]+(float)z*a[5],(float)x*a[6]+(float)y*a[7]+(float)z*a[8]};}
    private static float[] rotate(float u,float v,int direction){float swap;if(direction==1){swap=u;u=-v;v=swap;}else if(direction==2){u=-u;v=-v;}else if(direction==3){swap=u;u=v;v=-swap;}return new float[]{u,v};}
    private static int dominant(float x,float y,float z){float ax=x<0?-x:x,ay=y<0?-y:y,az=z<0?-z:z;if(ay>ax&&ay>az)return y>0?0:1;if(az>ax&&az>ay)return z>0?2:3;return x>0?4:5;}
    private static void wrap(float[] uv,float period,int direction){float half=period/2F;if((direction&1)==0){adjust(uv,2,0,half,period);adjust(uv,4,0,half,period);}else{adjust(uv,3,1,half,period);adjust(uv,5,1,half,period);}}
    private static void adjust(float[] uv,int index,int base,float half,float period){if(uv[index]-uv[base]>half)uv[index]-=period;else if(uv[base]-uv[index]>half)uv[index]+=period;}
}
