package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DirectionAssignmentSelectionTest {
    @Test void browsingSourceNeverMovesDestinationAndCellSelectionOffersCanonicalConvenience(){
        DirectionAssignmentSelection selection=new DirectionAssignmentSelection();
        selection.selectDestination(2,3);
        assertEquals(3,selection.sourceDirection());
        selection.browseSource(4);
        assertEquals(2,selection.destinationRow());
        assertEquals(3,selection.destinationColumn());
        assertEquals("Browsing source: Away",selection.sourceLabel());
        assertEquals("Destination: Right step / Diagonal away",selection.destinationLabel());
    }

    @Test void previewUsesBrowsingDirectionForAlternativeAndPersistedDirectionForCell(){
        TargetSheet sheet=new TargetSheet();sheet.override(0,0,pose(),4);
        DirectionAssignmentSelection selection=new DirectionAssignmentSelection();selection.selectDestination(0,0);selection.browseSource(2);
        assertEquals(2,selection.previewDirection(sheet,true));
        assertEquals(4,selection.previewDirection(sheet,false));
    }

    private static PoseSelection pose(){PoseSelection pose=new PoseSelection();pose.sequenceId=1;return pose;}
}
