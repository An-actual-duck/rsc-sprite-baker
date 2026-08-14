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
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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
import javax.swing.KeyStroke;
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
        private final JLabel actualPreview=new JLabel("Select a pose",JLabel.CENTER);
        private JSpinner cellWidth,cellHeight,supersample,modelScale,pitch,yawOffset,vertical,ambient,diffuse,lightAzimuth,lightElevation,ditherStrength;
        private JComboBox<String> palette,dithering,preset; private boolean syncingControls;
        SelectorFrame(AnimationWorkspace workspace,SpriteProject project,Path projectPath,Path output){
            super("RSC Sprite Baker — animation selector");this.workspace=workspace;this.project=project;this.projectPath=projectPath;this.output=output;
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){try{workspace.close();}catch(IOException ignored){}}});
            setLayout(new BorderLayout());add(visualPanel(),BorderLayout.NORTH);JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sourcePanel(),sheetPanel());split.setResizeWeight(.38);add(split,BorderLayout.CENTER);add(actions(),BorderLayout.SOUTH);
            setSize(1700,980);setLocationByPlatform(true);installShortcuts();sequenceId.setValue(Math.max(0,project.standingSequenceId));loadSequence();
        }
        private JPanel visualPanel(){JPanel panel=new JPanel(new GridLayout(2,8,4,2));VisualSettings v=project.visual;
            preset=new JComboBox<>(new String[]{"Unmodified studio","RSC crisp","RSC restrained","RSC coarse","Custom"});preset.setSelectedItem(v.preset);panel.add(labeled("Preset",preset));
            cellWidth=spinner(v.cellWidth,16,512,8);cellHeight=spinner(v.cellHeight,16,512,8);supersample=spinner(v.supersample,1,8,1);modelScale=spinner(v.modelScale,.25,4,.05);pitch=spinner(v.pitchDegrees,-45,60,1);yawOffset=spinner(v.yawOffsetDegrees,-45,45,1);vertical=spinner(v.verticalOffsetPixels,-128,128,1);
            ambient=spinner(v.ambient,0,1,.02);diffuse=spinner(v.diffuse,0,1,.02);lightAzimuth=spinner(v.lightAzimuthDegrees,-180,180,5);lightElevation=spinner(v.lightElevationDegrees,-90,90,5);ditherStrength=spinner(v.ditherStrength,0,1,.05);
            palette=new JComboBox<>(new String[]{PaletteReducer.UNMODIFIED,PaletteReducer.RSC_64,PaletteReducer.RSC_27});palette.setSelectedItem(v.palette);dithering=new JComboBox<>(new String[]{PaletteReducer.NO_DITHER,PaletteReducer.ORDERED_4X4});dithering.setSelectedItem(v.dithering);
            panel.add(labeled("Cell W",cellWidth));panel.add(labeled("H",cellHeight));panel.add(labeled("SS",supersample));panel.add(labeled("Scale",modelScale));panel.add(labeled("Pitch",pitch));panel.add(labeled("Yaw +",yawOffset));panel.add(labeled("Y +up",vertical));panel.add(labeled("Ambient",ambient));panel.add(labeled("Directional",diffuse));panel.add(labeled("Light az",lightAzimuth));panel.add(labeled("el",lightElevation));panel.add(labeled("Palette",palette));panel.add(labeled("Dither",dithering));panel.add(labeled("Strength",ditherStrength));
            java.util.List<JSpinner> spinners=java.util.List.of(cellWidth,cellHeight,supersample,modelScale,pitch,yawOffset,vertical,ambient,diffuse,lightAzimuth,lightElevation,ditherStrength);for(JSpinner spinner:spinners)spinner.addChangeListener(e->visualChanged());palette.addActionListener(e->visualChanged());dithering.addActionListener(e->visualChanged());preset.addActionListener(e->{if(!syncingControls&&!"Custom".equals(preset.getSelectedItem())){project.visual.applyPreset((String)preset.getSelectedItem());syncVisualControls();refreshCells();}});return panel;}
        private static JPanel labeled(String text,JComponent component){JPanel p=new JPanel(new BorderLayout());p.add(new JLabel(text),BorderLayout.NORTH);p.add(component,BorderLayout.CENTER);return p;}
        private static JSpinner spinner(Number value,Comparable<?> min,Comparable<?> max,Number step){return new JSpinner(new SpinnerNumberModel(value,(Comparable)min,(Comparable)max,step));}
        private void syncVisualControls(){syncingControls=true;VisualSettings v=project.visual;cellWidth.setValue(v.cellWidth);cellHeight.setValue(v.cellHeight);supersample.setValue(v.supersample);modelScale.setValue(v.modelScale);pitch.setValue(v.pitchDegrees);yawOffset.setValue(v.yawOffsetDegrees);vertical.setValue(v.verticalOffsetPixels);ambient.setValue(v.ambient);diffuse.setValue(v.diffuse);lightAzimuth.setValue(v.lightAzimuthDegrees);lightElevation.setValue(v.lightElevationDegrees);palette.setSelectedItem(v.palette);dithering.setSelectedItem(v.dithering);ditherStrength.setValue(v.ditherStrength);syncingControls=false;}
        private void visualChanged(){if(syncingControls)return;VisualSettings v=project.visual;v.cellWidth=(Integer)cellWidth.getValue();v.cellHeight=(Integer)cellHeight.getValue();v.supersample=(Integer)supersample.getValue();v.modelScale=((Number)modelScale.getValue()).doubleValue();v.pitchDegrees=((Number)pitch.getValue()).doubleValue();v.yawOffsetDegrees=((Number)yawOffset.getValue()).doubleValue();v.verticalOffsetPixels=((Number)vertical.getValue()).doubleValue();v.ambient=((Number)ambient.getValue()).doubleValue();v.diffuse=((Number)diffuse.getValue()).doubleValue();v.lightAzimuthDegrees=((Number)lightAzimuth.getValue()).doubleValue();v.lightElevationDegrees=((Number)lightElevation.getValue()).doubleValue();v.palette=(String)palette.getSelectedItem();v.dithering=(String)dithering.getSelectedItem();v.ditherStrength=((Number)ditherStrength.getValue()).doubleValue();v.preset="Custom";syncingControls=true;preset.setSelectedItem("Custom");syncingControls=false;refreshCells();}
        private JPanel sourcePanel(){
            JPanel panel=new JPanel(new BorderLayout());JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(new JLabel("Source sequence"));controls.add(sequenceId);controls.add(role);JButton load=new JButton("Load timeline");load.addActionListener(e->loadSequence());controls.add(load);panel.add(controls,BorderLayout.NORTH);
            timeline.setLayoutOrientation(JList.HORIZONTAL_WRAP);timeline.setVisibleRowCount(-1);timeline.setFixedCellWidth(112);timeline.setFixedCellHeight(142);
            timeline.setCellRenderer((list,value,index,isSelected,focus)->{JLabel label=new JLabel(value.toString(),value.icon,JLabel.CENTER);label.setHorizontalTextPosition(JLabel.CENTER);label.setVerticalTextPosition(JLabel.BOTTOM);label.setOpaque(true);label.setBackground(isSelected?new Color(255,220,150):list.getBackground());return label;});
            timeline.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&timeline.getSelectedValue()!=null){scrubber.setValue((int)timeline.getSelectedValue().timeMillis);selectAt(scrubber.getValue());}});timeline.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)assignSelectedCell();}});
            panel.add(new JScrollPane(timeline),BorderLayout.CENTER);JPanel bottom=new JPanel();bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS));scrubber.addChangeListener(e->selectAt(scrubber.getValue()));bottom.add(new JLabel("Rendered timeline / 20 ms client-time scrubber"));bottom.add(scrubber);panel.add(bottom,BorderLayout.SOUTH);return panel;
        }
        private JPanel sheetPanel(){
            JPanel outer=new JPanel(new BorderLayout());JPanel grid=new JPanel(new GridLayout(4,7,4,4));grid.add(new JLabel("Pose / view"));for(String label:TargetSheet.COLUMN_LABELS)grid.add(new JLabel(label));
            for(int r=0;r<3;r++){grid.add(new JLabel(TargetSheet.ROW_LABELS[r]));for(int c=0;c<6;c++){CellPanel cp=new CellPanel(r,c);cellPanels[r][c]=cp;grid.add(cp);}}
            outer.add(grid,BorderLayout.CENTER);JPanel rowActions=new JPanel(new FlowLayout(FlowLayout.LEFT));actualPreview.setBorder(BorderFactory.createTitledBorder("Actual-size selected-cell preview"));rowActions.add(actualPreview);
            for(int r=0;r<3;r++){final int row=r;JButton b=new JButton("Set "+TargetSheet.ROW_LABELS[r]+" row");b.addActionListener(e->{if(selectedPose!=null){project.sheet.assignShared(row,selectedPose);project.sheet.suggestCombatStandingFromSide();refreshCells();}});rowActions.add(b);}
            JButton assign=new JButton("Replace selected cell [Enter]");assign.addActionListener(e->assignSelectedCell());rowActions.add(assign);
            JButton clear=new JButton("Use shared row pose");clear.addActionListener(e->{project.sheet.clearOverride(selectedRow,selectedColumn);refreshCells();});rowActions.add(clear);outer.add(rowActions,BorderLayout.SOUTH);return outer;
        }
        private JPanel actions(){JPanel p=new JPanel(new FlowLayout(FlowLayout.RIGHT));JCheckBox mirror=new JCheckBox("Mirrored direction preview",project.mirroredPreview);mirror.addActionListener(e->{project.mirroredPreview=mirror.isSelected();refreshCells();});p.add(mirror);
            JButton suggest=new JButton("Suggest empty cells");suggest.setToolTipText("Suggestions fill empty cells only and never replace selections");suggest.addActionListener(e->suggest());p.add(suggest);
            JButton save=new JButton("Save project");save.addActionListener(e->run(()->project.save(projectPath),"Saved "+projectPath));p.add(save);
            JButton export=new JButton("Export 18-frame PNG + manifest");export.addActionListener(e->run(()->{project.save(projectPath);new SheetExporter().export(workspace,project,output);},"Exported to "+output));p.add(export);return p;}
        private void loadSequence(){try{int id=(Integer)sequenceId.getValue();loaded=workspace.cache.loadSequence(id);String selected=(String)role.getSelectedItem();if("Standing".equals(selected))project.standingSequenceId=id;else if("Walking".equals(selected))project.walkingSequenceId=id;else if("Combat".equals(selected))project.combatSequenceId=id;
                timelineModel.clear();for(int i=0;i<loaded.frameIds.length;i++){long time=AnimationTimeline.frameStartMillis(loaded,i);PoseSelection pose=workspace.selectionAt(id,time,"timeline");BufferedImage image=renderer.renderStyled(List.of(workspace.pose(pose,false)),workspace.npc,90,null,project.visual);timelineModel.addElement(new FrameChoice(i,time,icon(image,96,false)));}
                scrubber.setMaximum((int)Math.max(0,loaded.totalMillis()-1));scrubber.setValue(0);if(!timelineModel.isEmpty())timeline.setSelectedIndex(0);
            }catch(Exception e){error(e);}}
        private void selectAt(long millis){if(loaded==null)return;try{selectedPose=workspace.selectionAt(loaded.id,millis,"timeline");refreshActualPreview(sharedViewport());}catch(IOException e){error(e);}}
        private void suggest(){try{
            if(project.standingSequenceId>=0){PoseSelection p=workspace.selectionAt(project.standingSequenceId,0,"suggestion");for(int c=0;c<5;c++)project.sheet.suggest(0,c,p);}
            if(project.walkingSequenceId>=0){Sequence530 w=workspace.cache.loadSequence(project.walkingSequenceId);PoseSelection l=workspace.selectionAt(w.id,w.totalMillis()/3,"suggestion"),r=workspace.selectionAt(w.id,w.totalMillis()*2/3,"suggestion");for(int c=0;c<5;c++){project.sheet.suggest(1,c,l);project.sheet.suggest(2,c,r);}}
            if(project.combatSequenceId>=0){Sequence530 a=workspace.cache.loadSequence(project.combatSequenceId);project.sheet.suggest(0,5,workspace.selectionAt(a.id,0,"suggestion"));project.sheet.suggest(1,5,workspace.selectionAt(a.id,a.totalMillis()/3,"suggestion"));project.sheet.suggest(2,5,workspace.selectionAt(a.id,a.totalMillis()*2/3,"suggestion"));}
            project.sheet.suggestCombatStandingFromSide();refreshCells();
            }catch(Exception e){error(e);}}
        private void refreshCells(){if(cellPanels[0][0]==null)return;try{StaticRenderer.Viewport viewport=sharedViewport();for(int r=0;r<3;r++)for(int c=0;c<6;c++)cellPanels[r][c].refresh(viewport);refreshActualPreview(viewport);}catch(Exception e){error(e);}}
        private StaticRenderer.Viewport sharedViewport()throws IOException{java.util.List<StaticRenderer.View> views=new java.util.ArrayList<>();double[] yaw={0,45,90,135,180,90};for(int r=0;r<3;r++)for(int c=0;c<6;c++){PoseSelection pose=project.sheet.cells[r][c].pose;if(pose!=null)views.add(new StaticRenderer.View(workspace.pose(pose,project.tweening),yaw[c]));}if(views.isEmpty()&&selectedPose!=null)views.add(new StaticRenderer.View(workspace.pose(selectedPose,project.tweening),yaw[selectedColumn]));return views.isEmpty()?null:renderer.fitStyled(views,workspace.npc,project.visual);}
        private void refreshActualPreview(StaticRenderer.Viewport viewport){PoseSelection pose=project.sheet.cells[selectedRow][selectedColumn].pose;if(pose==null)pose=selectedPose;if(pose==null){actualPreview.setIcon(null);actualPreview.setText("Select a pose");return;}try{BufferedImage image=renderer.renderStyled(List.of(workspace.pose(pose,project.tweening)),workspace.npc,new double[]{0,45,90,135,180,90}[selectedColumn],viewport,project.visual);actualPreview.setText(null);actualPreview.setIcon(iconActual(image,project.mirroredPreview));}catch(Exception e){actualPreview.setText("Error");}}
        private void assignSelectedCell(){if(selectedPose!=null){project.sheet.override(selectedRow,selectedColumn,selectedPose);refreshCells();}}
        private void setSharedRow(int row){if(selectedPose!=null){project.sheet.assignShared(row,selectedPose);project.sheet.suggestCombatStandingFromSide();refreshCells();}}
        private void installShortcuts(){bind("ENTER","assign",this::assignSelectedCell);bind("ctrl 1","row0",()->setSharedRow(0));bind("ctrl 2","row1",()->setSharedRow(1));bind("ctrl 3","row2",()->setSharedRow(2));bind("L","lock",()->{TargetSheet.Cell cell=project.sheet.cells[selectedRow][selectedColumn];cell.locked=!cell.locked;refreshCells();});bind("BACK_SPACE","shared",()->{project.sheet.clearOverride(selectedRow,selectedColumn);refreshCells();});}
        private void bind(String stroke,String name,Runnable action){getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke),name);getRootPane().getActionMap().put(name,new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){action.run();}});}
        private final class CellPanel extends JPanel {final int row,col;final JLabel preview=new JLabel("Empty",JLabel.CENTER);final JCheckBox lock=new JCheckBox("Lock");CellPanel(int row,int col){this.row=row;this.col=col;setLayout(new BorderLayout());setPreferredSize(new Dimension(135,190));add(preview,BorderLayout.CENTER);add(lock,BorderLayout.SOUTH);lock.addActionListener(e->project.sheet.cells[row][col].locked=lock.isSelected());MouseAdapter select=new MouseAdapter(){public void mouseClicked(MouseEvent e){selectedRow=row;selectedColumn=col;if(e.getClickCount()==2)assignSelectedCell();else refreshCells();}};addMouseListener(select);preview.addMouseListener(select);refresh(null);}
            void refresh(StaticRenderer.Viewport viewport){TargetSheet.Cell cell=project.sheet.cells[row][col];lock.setSelected(cell.locked);Color state=cell.locked?new Color(180,55,55):cell.override?new Color(50,110,200):cell.pose!=null&&"suggestion".equals(cell.pose.source)?new Color(150,75,180):cell.pose!=null?new Color(50,145,85):Color.GRAY;javax.swing.border.Border stateBorder=BorderFactory.createTitledBorder(BorderFactory.createLineBorder(state,2),cell.pose==null?"Empty":cell.locked?"LOCKED":cell.override?"OVERRIDE":"suggestion".equals(cell.pose.source)?"SUGGESTION":"SHARED");setBorder(row==selectedRow&&col==selectedColumn?BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.ORANGE,3),stateBorder):stateBorder);if(cell.pose==null){preview.setIcon(null);preview.setText("Double-click to assign");return;}try{BufferedImage image=renderer.renderStyled(List.of(workspace.pose(cell.pose,project.tweening)),workspace.npc,new double[]{0,45,90,135,180,90}[col],viewport,project.visual);preview.setIcon(icon(image,112,project.mirroredPreview));preview.setText("S"+cell.pose.sequenceId+" F"+cell.pose.frameIndex+" +"+cell.pose.cycleOffset);preview.setHorizontalTextPosition(JLabel.CENTER);preview.setVerticalTextPosition(JLabel.BOTTOM);setToolTipText(cell.pose.source+"; "+(cell.locked?"locked":"editable"));}catch(Exception e){preview.setText("Error: "+e.getMessage());}}
        }
        private void run(Action action,String success){try{action.run();JOptionPane.showMessageDialog(this,success);}catch(Exception e){error(e);}}
        private void error(Exception e){JOptionPane.showMessageDialog(this,e.toString(),"Error",JOptionPane.ERROR_MESSAGE);}
    }
    private interface Action{void run()throws Exception;}
    private static final class FrameChoice {final int index;final long timeMillis;final ImageIcon icon;FrameChoice(int i,long t,ImageIcon icon){index=i;timeMillis=t;this.icon=icon;}public String toString(){return "Frame "+index+" @ "+timeMillis+" ms";}}
    private static ImageIcon icon(BufferedImage source,int size,boolean mirror){BufferedImage transformed=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=transformed.createGraphics();if(mirror)g.drawImage(source,source.getWidth(),0,-source.getWidth(),source.getHeight(),null);else g.drawImage(source,0,0,null);g.dispose();Image scaled=transformed.getScaledInstance(size,size,Image.SCALE_SMOOTH);return new ImageIcon(scaled);}
    private static ImageIcon iconActual(BufferedImage source,boolean mirror){if(!mirror)return new ImageIcon(source);BufferedImage transformed=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=transformed.createGraphics();g.drawImage(source,source.getWidth(),0,-source.getWidth(),source.getHeight(),null);g.dispose();return new ImageIcon(transformed);}
    private static final class Arguments{Path cache,project,output;int npcId=72;static Arguments parse(String[] args){Arguments a=new Arguments();for(int i=0;i<args.length;i+=2){if(i+1>=args.length)usage();switch(args[i]){case"--cache":a.cache=Path.of(args[i+1]);break;case"--project":a.project=Path.of(args[i+1]);break;case"--output-dir":a.output=Path.of(args[i+1]);break;case"--npc":a.npcId=Integer.parseInt(args[i+1]);break;default:usage();}}if(a.cache==null||a.project==null||a.output==null)usage();return a;}static void usage(){throw new IllegalArgumentException("usage: --cache PATH --project FILE --output-dir PATH [--npc ID]");}}
}
