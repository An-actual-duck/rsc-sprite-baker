package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SheetExporterProvenanceTest {
    @Test void manifestCellRecordsDestinationEffectiveSourceAndExportYaw(){
        TargetSheet sheet=new TargetSheet();PoseSelection pose=new PoseSelection();pose.sequenceId=10;pose.frameIndex=0;pose.source="test";sheet.override(0,4,pose,1);
        Sequence530 sequence=new Sequence530(10);sequence.frameIds=new int[]{123};sequence.durations=new int[]{2};
        Framemap530 framemap=new Framemap530(77,new int[0],new boolean[0],new int[0],new int[0][]);
        Frame530 frame=new Frame530(123,framemap,new int[0],new int[0],new int[0],new int[0],new int[0],new int[0]);
        Map<String,Object> trace=SheetExporter.cellTrace(sheet,0,4,sequence,frame);
        assertEquals(4,trace.get("column"));assertEquals("Away",trace.get("columnLabel"));
        assertEquals(1,trace.get("sourceDirection"));assertEquals("Facing diagonal",trace.get("sourceDirectionLabel"));
        assertEquals(true,trace.get("directionOverride"));assertEquals(45.0,trace.get("yawDegrees"));
    }

    @Test void manifestOrientationRecordsThePixelsAppliedToPreviewPlaybackAndExport(){
        Map<String,Object> enabled=SheetExporter.orientationTrace(true),disabled=SheetExporter.orientationTrace(false);
        assertEquals(true,enabled.get("horizontalInversion"));assertEquals("right",enabled.get("defaultFacing"));assertEquals("preview, playback, and exported PNG",enabled.get("appliesTo"));
        assertEquals(false,disabled.get("horizontalInversion"));assertEquals("renderer-native",disabled.get("defaultFacing"));
    }
}
