package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SelectorMainLayoutTest {
    @Test void saveSettingsConsumesOnlyExistingMarginBelowShadowDepth()throws Exception{
        AtomicReference<Throwable> failure=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{try{
            Layout baseline=layout(false),withSave=layout(true);
            assertEquals(baseline.statusPreferred,withSave.statusPreferred,"status margin preferred size");
            assertEquals(baseline.statusMinimum,withSave.statusMinimum,"status margin minimum size");
            assertEquals(baseline.northPreferred,withSave.northPreferred,"customization area preferred size");
            assertEquals(baseline.northMinimum,withSave.northMinimum,"customization area minimum size");
            assertEquals(baseline.toolbarBounds,withSave.toolbarBounds,"two-row customization toolbar allocation");
            assertArrayEquals(baseline.controlBounds,withSave.controlBounds,"all existing control allocations");
            assertEquals(baseline.shadowBounds,withSave.shadowBounds,"Shadow depth cell allocation");
            assertEquals(baseline.sheetBounds,withSave.sheetBounds,"sprite-sheet allocation");
            Insets margin=withSave.status.getInsets();Rectangle button=withSave.save.getBounds();
            assertTrue(button.x>=withSave.shadowBounds.x,"button stays beneath the Shadow depth column");
            assertTrue(button.x+button.width<=withSave.shadowBounds.x+withSave.shadowBounds.width,"button stays within the Shadow depth column");
            assertTrue(button.y>=margin.top&&button.y+button.height<=withSave.status.getHeight()-margin.bottom,"button stays entirely inside the existing vertical margin");
        }catch(Throwable t){failure.set(t);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }

    private static Layout layout(boolean addSave){
        JPanel toolbar=new JPanel(new GridLayout(2,9,4,2));
        for(int i=0;i<18;i++)toolbar.add(control(i==17?"Shadow depth %":"Control "+i));
        JLabel status=new JLabel("Checking model and material compatibility…");
        status.setBorder(BorderFactory.createEmptyBorder(2,8,4,8));
        Dimension statusPreferred=status.getPreferredSize(),statusMinimum=status.getMinimumSize();
        JButton save=null;
        if(addSave){save=new JButton("Save settings");save.setMargin(new Insets(1,4,1,4));SelectorMain.SelectorFrame.placeInExistingMargin(status,save);}
        JPanel north=new JPanel(new BorderLayout());north.add(toolbar,BorderLayout.CENTER);north.add(status,BorderLayout.SOUTH);
        Dimension northPreferred=north.getPreferredSize(),northMinimum=north.getMinimumSize();
        JPanel sheet=new JPanel();JPanel root=new JPanel(new BorderLayout());root.add(north,BorderLayout.NORTH);root.add(sheet,BorderLayout.CENTER);
        root.setSize(1280,850);root.doLayout();north.doLayout();toolbar.doLayout();status.doLayout();
        Rectangle[] controlBounds=new Rectangle[toolbar.getComponentCount()];for(int i=0;i<controlBounds.length;i++)controlBounds[i]=toolbar.getComponent(i).getBounds();
        return new Layout(status,save,statusPreferred,statusMinimum,northPreferred,northMinimum,toolbar.getBounds(),controlBounds,toolbar.getComponent(17).getBounds(),sheet.getBounds());
    }

    private static JPanel control(String name){JPanel panel=new JPanel(new BorderLayout());panel.add(new JLabel(name),BorderLayout.NORTH);panel.add(SliderSpinner.integer(100,0,150,5),BorderLayout.CENTER);return panel;}

    private static final class Layout {
        final JLabel status;final JButton save;final Dimension statusPreferred,statusMinimum,northPreferred,northMinimum;final Rectangle toolbarBounds,shadowBounds,sheetBounds;final Rectangle[] controlBounds;
        Layout(JLabel status,JButton save,Dimension statusPreferred,Dimension statusMinimum,Dimension northPreferred,Dimension northMinimum,Rectangle toolbarBounds,Rectangle[] controlBounds,Rectangle shadowBounds,Rectangle sheetBounds){this.status=status;this.save=save;this.statusPreferred=statusPreferred;this.statusMinimum=statusMinimum;this.northPreferred=northPreferred;this.northMinimum=northMinimum;this.toolbarBounds=toolbarBounds;this.controlBounds=controlBounds;this.shadowBounds=shadowBounds;this.sheetBounds=sheetBounds;}
    }
}
