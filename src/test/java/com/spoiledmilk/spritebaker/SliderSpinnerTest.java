package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SliderSpinnerTest {
    @Test void sliderAndEditableValueStaySynchronizedWithoutOpeningAWindow()throws Exception{
        SwingUtilities.invokeAndWait(()->{SliderSpinner control=SliderSpinner.decimal(.55,0,1,.05);assertEquals(.55,control.value().doubleValue());control.slider().setValue(20);assertEquals(.20,control.value().doubleValue());control.spinner().setValue(.333);assertEquals(.333,control.value().doubleValue());assertEquals(33,control.slider().getValue());});
    }

    @Test void dynamicIntegerMaximumClampsBothRepresentations()throws Exception{
        SwingUtilities.invokeAndWait(()->{SliderSpinner control=SliderSpinner.integer(40,0,100,1);control.setMaximum(25);assertEquals(25,control.value().intValue());assertEquals(25,control.slider().getMaximum());assertEquals(25,control.slider().getValue());});
    }
}
