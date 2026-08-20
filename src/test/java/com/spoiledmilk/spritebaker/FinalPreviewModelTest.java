package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class FinalPreviewModelTest {
    @Test void selectedCellAlwaysSuppliesItsExactAssignedFrameAndEffectiveDirection(){
        TargetSheet sheet=new TargetSheet();DirectionAssignmentSelection selection=new DirectionAssignmentSelection();
        sheet.override(0,0,pose(10,0,0),4);sheet.override(1,0,pose(20,3,60));sheet.override(2,0,pose(20,7,140));
        selection.selectDestination(0,0);selection.browseSource(2);
        assertPose(FinalPreviewModel.assignedPose(sheet,selection),10,0,0);assertEquals(4,FinalPreviewModel.assignedDirection(sheet,selection));
        selection.selectDestination(1,0);assertPose(FinalPreviewModel.assignedPose(sheet,selection),20,3,60);
        selection.selectDestination(2,0);assertPose(FinalPreviewModel.assignedPose(sheet,selection),20,7,140);
    }

    @Test void twoTimesNearestNeighborDisplayIsCrispAndNeverMutatesExportPixels(){
        BufferedImage source=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,0xff112233);source.setRGB(1,0,0xff445566);source.setRGB(0,1,0xff778899);source.setRGB(1,1,0xffaabbcc);
        BufferedImage display=FinalPreviewModel.displayImage(source,false);
        assertEquals(4,display.getWidth());assertEquals(4,display.getHeight());
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)assertEquals(source.getRGB(x/2,y/2),display.getRGB(x,y));
        assertEquals(2,source.getWidth());assertEquals(2,source.getHeight());assertEquals(0xff112233,source.getRGB(0,0));assertEquals(0xffaabbcc,source.getRGB(1,1));
        BufferedImage mirrored=FinalPreviewModel.displayImage(source,true);assertEquals(source.getRGB(1,0),mirrored.getRGB(0,0));assertEquals(source.getRGB(0,0),mirrored.getRGB(3,0));
    }

    @Test void enabledChoiceDoublesTheExistingPreviewAndLeavesSourcePixelsUntouched(){
        int[] pixels={0xff111100,0xff221100,0xff331100,0xff112200,0xff222200,0xff332200};BufferedImage source=new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,3,2,pixels,0,3);
        BufferedImage normal=FinalPreviewModel.displayImage(source,false,false);BufferedImage doubled=FinalPreviewModel.displayImage(source,false,true);
        assertTrue(FinalPreviewModel.DEFAULT_DOUBLED);
        assertEquals(6,normal.getWidth());assertEquals(4,normal.getHeight());assertEquals(12,doubled.getWidth());assertEquals(8,doubled.getHeight());
        for(int y=0;y<2;y++)for(int x=0;x<3;x++){int expected=pixels[y*3+x];assertEquals(expected,source.getRGB(x,y));for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)assertEquals(expected,normal.getRGB(x*2+dx,y*2+dy));for(int dy=0;dy<4;dy++)for(int dx=0;dx<4;dx++)assertEquals(expected,doubled.getRGB(x*4+dx,y*4+dy));}
        assertEquals(3,source.getWidth());assertEquals(2,source.getHeight());
    }

    @Test void selectedSpritePreviewRemovesTransparentCellMarginsBeforeScaling(){
        BufferedImage source=new BufferedImage(8,9,BufferedImage.TYPE_INT_ARGB);source.setRGB(2,4,0xff123456);source.setRGB(4,6,0xffabcdef);
        BufferedImage normal=FinalPreviewModel.displaySprite(source,false,false);BufferedImage enlarged=FinalPreviewModel.displaySprite(source,false,true);
        assertEquals(6,normal.getWidth());assertEquals(6,normal.getHeight());assertEquals(12,enlarged.getWidth());assertEquals(12,enlarged.getHeight());
        assertEquals(0xff123456,enlarged.getRGB(0,0));assertEquals(0xffabcdef,enlarged.getRGB(11,11));assertEquals(8,source.getWidth());assertEquals(9,source.getHeight());assertEquals(0,source.getRGB(0,0));
    }

    private static PoseSelection pose(int sequence,int frame,long millis){PoseSelection pose=new PoseSelection();pose.sequenceId=sequence;pose.frameIndex=frame;pose.timeMillis=millis;return pose;}
    private static void assertPose(PoseSelection pose,int sequence,int frame,long millis){assertEquals(sequence,pose.sequenceId);assertEquals(frame,pose.frameIndex);assertEquals(millis,pose.timeMillis);}
}
