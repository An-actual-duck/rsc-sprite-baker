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

    @Test void responsiveDisplayUsesLargestIntegerScaleThatFitsViewport(){
        assertEquals(2,FinalPreviewModel.largestIntegerScale(128,128,620,300));
        assertEquals(4,FinalPreviewModel.largestIntegerScale(128,128,900,520));
        assertEquals(3,FinalPreviewModel.largestIntegerScale(64,128,350,390));
        assertEquals(1,FinalPreviewModel.largestIntegerScale(128,128,100,100));
        assertThrows(IllegalArgumentException.class,()->FinalPreviewModel.largestIntegerScale(0,128,500,500));
    }

    @Test void responsiveNearestNeighborScalingRemainsCrispAndLeavesSourceUntouched(){
        BufferedImage source=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,0xff102030);source.setRGB(1,0,0xffa0b0c0);
        BufferedImage display=FinalPreviewModel.displayImage(source,false,7,4);
        assertEquals(6,display.getWidth());assertEquals(3,display.getHeight());
        for(int y=0;y<3;y++)for(int x=0;x<6;x++)assertEquals(source.getRGB(x/3,0),display.getRGB(x,y));
        assertEquals(2,source.getWidth());assertEquals(1,source.getHeight());assertEquals(0xff102030,source.getRGB(0,0));assertEquals(0xffa0b0c0,source.getRGB(1,0));
    }

    private static PoseSelection pose(int sequence,int frame,long millis){PoseSelection pose=new PoseSelection();pose.sequenceId=sequence;pose.frameIndex=frame;pose.timeMillis=millis;return pose;}
    private static void assertPose(PoseSelection pose,int sequence,int frame,long millis){assertEquals(sequence,pose.sequenceId);assertEquals(frame,pose.frameIndex);assertEquals(millis,pose.timeMillis);}
}
