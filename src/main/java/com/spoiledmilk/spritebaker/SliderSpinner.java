package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

/** Compact slider with an editable numeric field representing the same bounded value. */
public final class SliderSpinner extends JPanel {
    private final JSlider slider;
    private final JSpinner spinner;
    private final int scale;
    private final boolean integral;
    private final List<ChangeListener> listeners=new ArrayList<>();
    private boolean syncing;

    public static SliderSpinner integer(int value,int minimum,int maximum,int spinnerStep){return new SliderSpinner(value,minimum,maximum,spinnerStep,1,true);}
    public static SliderSpinner decimal(double value,double minimum,double maximum,double spinnerStep){return new SliderSpinner(value,minimum,maximum,spinnerStep,100,false);}

    private SliderSpinner(Number value,Number minimum,Number maximum,Number spinnerStep,int scale,boolean integral){
        super(new BorderLayout(2,0));this.scale=scale;this.integral=integral;
        slider=new JSlider(toSlider(minimum.doubleValue()),toSlider(maximum.doubleValue()),toSlider(value.doubleValue()));
        spinner=integral?new JSpinner(new SpinnerNumberModel(value.intValue(),minimum.intValue(),maximum.intValue(),spinnerStep.intValue())):new JSpinner(new SpinnerNumberModel(value.doubleValue(),minimum.doubleValue(),maximum.doubleValue(),spinnerStep.doubleValue()));
        Dimension preferred=spinner.getPreferredSize();spinner.setPreferredSize(new Dimension(58,preferred.height));add(slider,BorderLayout.CENTER);add(spinner,BorderLayout.EAST);
        slider.addChangeListener(event->{if(syncing)return;syncing=true;spinner.setValue(integral?slider.getValue():slider.getValue()/(double)scale);syncing=false;fireChanged();});
        spinner.addChangeListener(event->{if(syncing)return;syncing=true;slider.setValue(toSlider(((Number)spinner.getValue()).doubleValue()));syncing=false;fireChanged();});
    }

    public Number value(){return (Number)spinner.getValue();}
    public void setValue(Number value){spinner.setValue(integral?value.intValue():value.doubleValue());}
    public void setMaximum(int maximum){if(!integral)throw new IllegalStateException("integer maximum required");slider.setMaximum(maximum);((SpinnerNumberModel)spinner.getModel()).setMaximum(maximum);if(value().intValue()>maximum)setValue(maximum);}
    public void addChangeListener(ChangeListener listener){listeners.add(listener);}
    JSlider slider(){return slider;}
    JSpinner spinner(){return spinner;}

    @Override public void setToolTipText(String text){super.setToolTipText(text);if(slider!=null)slider.setToolTipText(text);if(spinner!=null)spinner.setToolTipText(text);}

    private int toSlider(double value){return (int)Math.round(value*scale);}
    private void fireChanged(){javax.swing.event.ChangeEvent event=new javax.swing.event.ChangeEvent(this);for(ChangeListener listener:List.copyOf(listeners))listener.stateChanged(event);}
}
