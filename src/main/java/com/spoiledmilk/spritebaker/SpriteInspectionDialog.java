package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Modeless, resizable view of final export pixels at exact integer zoom. */
final class SpriteInspectionDialog extends JDialog {
    private final SpriteInspectionModel model=new SpriteInspectionModel();
    private final JLabel image=new JLabel("No final preview available",JLabel.CENTER);
    private final JComboBox<String> zoom=new JComboBox<>(new String[]{"1×","2×","3×","4×"});

    SpriteInspectionDialog(Window owner,Runnable closed){
        super(owner,"Large final sprite inspection",ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);setLayout(new BorderLayout());
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(new JLabel("Pixel zoom"));zoom.setSelectedIndex(model.zoom()-1);zoom.addActionListener(e->{model.zoom(zoom.getSelectedIndex()+1);refresh();});controls.add(zoom);controls.add(new JLabel("Nearest-neighbor integer scaling"));add(controls,BorderLayout.NORTH);
        image.setHorizontalTextPosition(JLabel.CENTER);image.setVerticalTextPosition(JLabel.BOTTOM);add(new JScrollPane(image),BorderLayout.CENTER);
        setSize(720,680);setLocationRelativeTo(owner);addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){closed.run();}});
    }

    void update(BufferedImage sprite,java.awt.Color background,boolean mirror,String label){model.update(sprite,background,mirror,label);refresh();}
    void clear(String label){model.clear(label);refresh();}
    private void refresh(){BufferedImage shown=model.presented();image.setIcon(shown==null?null:new ImageIcon(shown));image.setText(model.label());}
}
