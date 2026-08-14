package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Swing animation-selection MVP; intentionally separate from the Phase-1 CLI entry point. */
public final class SelectorMain {
    private SelectorMain(){ }
    public static void main(String[] args)throws Exception{
        Arguments a=Arguments.parse(args); SpriteProject project=Files.exists(a.project)?SpriteProject.load(a.project):new SpriteProject();
        if(project.npcId==0)project.npcId=a.npcId;
        AnimationWorkspace workspace=new AnimationWorkspace(a.cache,project.npcId);
        if(project.standingSequenceId<0)project.standingSequenceId=workspace.bas==null?workspace.npc.standingAnimation:workspace.bas.standingAnimation;
        if(project.walkingSequenceId<0)project.walkingSequenceId=workspace.bas==null?-1:workspace.bas.walkingAnimation;
        SwingUtilities.invokeLater(()->new SelectorFrame(workspace,project,a.project,a.output).setVisible(true));
    }

    private static final class SelectorFrame extends JFrame {
        private final AnimationWorkspace workspace; private final SpriteProject project; private final Path projectPath,output;
        private final StaticRenderer renderer=new StaticRenderer(); private final DefaultListModel<FrameChoice> timelineModel=new DefaultListModel<>();
        private final JList<FrameChoice> timeline=new JList<>(timelineModel); private final JSlider scrubber=new JSlider();
        private final JSpinner sequenceId=new JSpinner(new SpinnerNumberModel(0,0,65535,1)); private final JComboBox<String> role=new JComboBox<>(new String[]{"Standing","Walking","Combat","Other"});
        private final CellPanel[][] cellPanels=new CellPanel[3][6]; private PoseSelection selectedPose; private int selectedRow=0,selectedColumn=0; private Sequence530 loaded;
        SelectorFrame(AnimationWorkspace workspace,SpriteProject project,Path projectPath,Path output){
            super("RSC Sprite Baker — animation selector");this.workspace=workspace;this.project=project;this.projectPath=projectPath;this.output=output;
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){try{workspace.close();}catch(IOException ignored){}}});
            setLayout(new BorderLayout());JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sourcePanel(),sheetPanel());split.setResizeWeight(.38);add(split,BorderLayout.CENTER);add(actions(),BorderLayout.SOUTH);
            setSize(1500,900);setLocationByPlatform(true);sequenceId.setValue(Math.max(0,project.standingSequenceId));loadSequence();
        }
        private JPanel sourcePanel(){
            JPanel panel=new JPanel(new BorderLayout());JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(new JLabel("Source sequence"));controls.add(sequenceId);controls.add(role);JButton load=new JButton("Load timeline");load.addActionListener(e->loadSequence());controls.add(load);panel.add(controls,BorderLayout.NORTH);
            timeline.setLayoutOrientation(JList.HORIZONTAL_WRAP);timeline.setVisibleRowCount(-1);timeline.setFixedCellWidth(112);timeline.setFixedCellHeight(142);
            timeline.setCellRenderer((list,value,index,isSelected,focus)->{JLabel label=new JLabel(value.toString(),value.icon,JLabel.CENTER);label.setHorizontalTextPosition(JLabel.CENTER);label.setVerticalTextPosition(JLabel.BOTTOM);label.setOpaque(true);label.setBackground(isSelected?new Color(255,220,150):list.getBackground());return label;});
            timeline.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&timeline.getSelectedValue()!=null){scrubber.setValue((int)timeline.getSelectedValue().timeMillis);selectAt(scrubber.getValue());}});
            panel.add(new JScrollPane(timeline),BorderLayout.CENTER);JPanel bottom=new JPanel();bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS));scrubber.addChangeListener(e->selectAt(scrubber.getValue()));bottom.add(new JLabel("Rendered timeline / 20 ms client-time scrubber"));bottom.add(scrubber);panel.add(bottom,BorderLayout.SOUTH);return panel;
        }
        private JPanel sheetPanel(){
            JPanel outer=new JPanel(new BorderLayout());JPanel grid=new JPanel(new GridLayout(4,7,4,4));grid.add(new JLabel("Pose / view"));for(String label:TargetSheet.COLUMN_LABELS)grid.add(new JLabel(label));
            for(int r=0;r<3;r++){grid.add(new JLabel(TargetSheet.ROW_LABELS[r]));for(int c=0;c<6;c++){CellPanel cp=new CellPanel(r,c);cellPanels[r][c]=cp;grid.add(cp);}}
            outer.add(grid,BorderLayout.CENTER);JPanel rowActions=new JPanel(new FlowLayout(FlowLayout.LEFT));
            for(int r=0;r<3;r++){final int row=r;JButton b=new JButton("Set "+TargetSheet.ROW_LABELS[r]+" row");b.addActionListener(e->{if(selectedPose!=null){project.sheet.assignShared(row,selectedPose);project.sheet.suggestCombatStandingFromSide();refreshCells();}});rowActions.add(b);}
            JButton assign=new JButton("Replace selected cell");assign.addActionListener(e->{if(selectedPose!=null){project.sheet.override(selectedRow,selectedColumn,selectedPose);refreshCells();}});rowActions.add(assign);
            JButton clear=new JButton("Use shared row pose");clear.addActionListener(e->{project.sheet.clearOverride(selectedRow,selectedColumn);refreshCells();});rowActions.add(clear);outer.add(rowActions,BorderLayout.SOUTH);return outer;
        }
        private JPanel actions(){JPanel p=new JPanel(new FlowLayout(FlowLayout.RIGHT));JCheckBox mirror=new JCheckBox("Mirrored direction preview",project.mirroredPreview);mirror.addActionListener(e->{project.mirroredPreview=mirror.isSelected();refreshCells();});p.add(mirror);
            JButton suggest=new JButton("Suggest empty cells");suggest.setToolTipText("Suggestions fill empty cells only and never replace selections");suggest.addActionListener(e->suggest());p.add(suggest);
            JButton save=new JButton("Save project");save.addActionListener(e->run(()->project.save(projectPath),"Saved "+projectPath));p.add(save);
            JButton export=new JButton("Export 18-frame PNG + manifest");export.addActionListener(e->run(()->{project.save(projectPath);new SheetExporter().export(workspace,project,output);},"Exported to "+output));p.add(export);return p;}
        private void loadSequence(){try{int id=(Integer)sequenceId.getValue();loaded=workspace.cache.loadSequence(id);String selected=(String)role.getSelectedItem();if("Standing".equals(selected))project.standingSequenceId=id;else if("Walking".equals(selected))project.walkingSequenceId=id;else if("Combat".equals(selected))project.combatSequenceId=id;
                timelineModel.clear();for(int i=0;i<loaded.frameIds.length;i++){long time=AnimationTimeline.frameStartMillis(loaded,i);PoseSelection pose=workspace.selectionAt(id,time,"timeline");BufferedImage image=renderer.render(List.of(workspace.pose(pose,false)),workspace.npc,90,null);timelineModel.addElement(new FrameChoice(i,time,icon(image,96,false)));}
                scrubber.setMaximum((int)Math.max(0,loaded.totalMillis()-1));scrubber.setValue(0);if(!timelineModel.isEmpty())timeline.setSelectedIndex(0);
            }catch(Exception e){error(e);}}
        private void selectAt(long millis){if(loaded==null)return;try{selectedPose=workspace.selectionAt(loaded.id,millis,"timeline");}catch(IOException e){error(e);}}
        private void suggest(){try{
            if(project.standingSequenceId>=0){PoseSelection p=workspace.selectionAt(project.standingSequenceId,0,"suggestion");for(int c=0;c<5;c++)project.sheet.suggest(0,c,p);}
            if(project.walkingSequenceId>=0){Sequence530 w=workspace.cache.loadSequence(project.walkingSequenceId);PoseSelection l=workspace.selectionAt(w.id,w.totalMillis()/3,"suggestion"),r=workspace.selectionAt(w.id,w.totalMillis()*2/3,"suggestion");for(int c=0;c<5;c++){project.sheet.suggest(1,c,l);project.sheet.suggest(2,c,r);}}
            if(project.combatSequenceId>=0){Sequence530 a=workspace.cache.loadSequence(project.combatSequenceId);project.sheet.suggest(0,5,workspace.selectionAt(a.id,0,"suggestion"));project.sheet.suggest(1,5,workspace.selectionAt(a.id,a.totalMillis()/3,"suggestion"));project.sheet.suggest(2,5,workspace.selectionAt(a.id,a.totalMillis()*2/3,"suggestion"));}
            project.sheet.suggestCombatStandingFromSide();refreshCells();
            }catch(Exception e){error(e);}}
        private void refreshCells(){for(int r=0;r<3;r++)for(int c=0;c<6;c++)cellPanels[r][c].refresh();}
        private final class CellPanel extends JPanel {final int row,col;final JLabel preview=new JLabel("Empty",JLabel.CENTER);final JCheckBox lock=new JCheckBox("Lock");CellPanel(int row,int col){this.row=row;this.col=col;setLayout(new BorderLayout());setPreferredSize(new Dimension(135,190));add(preview,BorderLayout.CENTER);add(lock,BorderLayout.SOUTH);lock.addActionListener(e->project.sheet.cells[row][col].locked=lock.isSelected());MouseAdapter select=new MouseAdapter(){public void mouseClicked(MouseEvent e){selectedRow=row;selectedColumn=col;refreshCells();}};addMouseListener(select);preview.addMouseListener(select);refresh();}
            void refresh(){TargetSheet.Cell cell=project.sheet.cells[row][col];lock.setSelected(cell.locked);setBorder(BorderFactory.createLineBorder(row==selectedRow&&col==selectedColumn?Color.ORANGE:Color.GRAY,row==selectedRow&&col==selectedColumn?3:1));if(cell.pose==null){preview.setIcon(null);preview.setText("Empty");return;}try{BufferedImage image=renderer.render(List.of(workspace.pose(cell.pose,project.tweening)),workspace.npc,new double[]{0,45,90,135,180,90}[col],null);preview.setIcon(icon(image,112,project.mirroredPreview));preview.setText("S"+cell.pose.sequenceId+" F"+cell.pose.frameIndex+" +"+cell.pose.cycleOffset);preview.setHorizontalTextPosition(JLabel.CENTER);preview.setVerticalTextPosition(JLabel.BOTTOM);}catch(Exception e){preview.setText("Error: "+e.getMessage());}}
        }
        private void run(Action action,String success){try{action.run();JOptionPane.showMessageDialog(this,success);}catch(Exception e){error(e);}}
        private void error(Exception e){JOptionPane.showMessageDialog(this,e.toString(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private interface Action{void run()throws Exception;}
    private static final class FrameChoice {final int index;final long timeMillis;final ImageIcon icon;FrameChoice(int i,long t,ImageIcon icon){index=i;timeMillis=t;this.icon=icon;}public String toString(){return "Frame "+index+" @ "+timeMillis+" ms";}}
    private static ImageIcon icon(BufferedImage source,int size,boolean mirror){BufferedImage transformed=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=transformed.createGraphics();if(mirror)g.drawImage(source,source.getWidth(),0,-source.getWidth(),source.getHeight(),null);else g.drawImage(source,0,0,null);g.dispose();Image scaled=transformed.getScaledInstance(size,size,Image.SCALE_SMOOTH);return new ImageIcon(scaled);}
    private static final class Arguments{Path cache,project,output;int npcId=72;static Arguments parse(String[] args){Arguments a=new Arguments();for(int i=0;i<args.length;i+=2){if(i+1>=args.length)usage();switch(args[i]){case"--cache":a.cache=Path.of(args[i+1]);break;case"--project":a.project=Path.of(args[i+1]);break;case"--output-dir":a.output=Path.of(args[i+1]);break;case"--npc":a.npcId=Integer.parseInt(args[i+1]);break;default:usage();}}if(a.cache==null||a.project==null||a.output==null)usage();return a;}static void usage(){throw new IllegalArgumentException("usage: --cache PATH --project FILE --output-dir PATH [--npc ID]");}}
}
