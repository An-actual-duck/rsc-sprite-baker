package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/** Lazy name/ID browser with on-demand model/material and animation diagnosis. */
final class NpcBrowserDialog extends JDialog {
    private final Path cachePath;private final Consumer<NpcCatalogEntry> selection;private final DefaultListModel<NpcCatalogEntry> model=new DefaultListModel<>();private final JList<NpcCatalogEntry> list=new JList<>(model);private final JTextField query=new JTextField(24);private final JTextArea detail=new JTextArea("Search by name or enter an exact NPC ID.");private final JProgressBar progress=new JProgressBar();private NpcCatalog catalog;private boolean working;
    NpcBrowserDialog(Window owner,Path cachePath,Consumer<NpcCatalogEntry> selection){super(owner,"Browse NPCs",ModalityType.MODELESS);this.cachePath=cachePath;this.selection=selection;setDefaultCloseOperation(DISPOSE_ON_CLOSE);setLayout(new BorderLayout(6,6));JPanel search=new JPanel(new FlowLayout(FlowLayout.LEFT));search.add(new JLabel("Name or ID"));search.add(query);JButton find=new JButton("Search");find.addActionListener(e->search());query.addActionListener(e->search());search.add(find);JButton initial=new JButton("First 100");initial.addActionListener(e->loadFirst());search.add(initial);add(search,BorderLayout.NORTH);list.setVisibleRowCount(18);list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())diagnose(list.getSelectedValue());});add(new JScrollPane(list),BorderLayout.CENTER);JPanel bottom=new JPanel(new BorderLayout());detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setOpaque(false);detail.setPreferredSize(new Dimension(760,70));bottom.add(detail,BorderLayout.CENTER);progress.setStringPainted(true);bottom.add(progress,BorderLayout.NORTH);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton choose=new JButton("Load Selected NPC");choose.addActionListener(e->{NpcCatalogEntry entry=list.getSelectedValue();if(entry!=null){selection.accept(entry);dispose();}});actions.add(choose);JButton close=new JButton("Close");close.addActionListener(e->dispose());actions.add(close);bottom.add(actions,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);setSize(850,640);setLocationRelativeTo(owner);addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){closeCatalog();}});openCatalog();}
    private void openCatalog(){busy("Indexing NPC IDs…",true);new SwingWorker<NpcCatalog,Void>(){protected NpcCatalog doInBackground()throws Exception{return new NpcCatalog(cachePath);}protected void done(){try{catalog=get();detail.setText("Cache contains "+catalog.size()+" NPC definition IDs. Definitions load only as needed.");busy("",false);loadFirst();}catch(Exception e){detail.setText(e.toString());busy("",false);}}}.execute();}
    private void loadFirst(){if(catalog==null||working)return;busy("Loading first NPCs…",true);new SwingWorker<List<NpcCatalogEntry>,Void>(){protected List<NpcCatalogEntry> doInBackground(){java.util.ArrayList<NpcCatalogEntry> out=new java.util.ArrayList<>();for(int id:catalog.ids(0,100))try{out.add(catalog.load(id));}catch(Exception ignored){}return out;}protected void done(){try{showResults(get());}catch(Exception e){detail.setText(e.toString());}finally{busy("",false);}}}.execute();}
    private void search(){if(catalog==null||working)return;String text=query.getText();busy("Searching definitions…",true);new SwingWorker<List<NpcCatalogEntry>,int[]>(){protected List<NpcCatalogEntry> doInBackground()throws Exception{return catalog.search(text,250,(done,total)->publish(new int[]{done,total}));}protected void process(List<int[]> chunks){int[] last=chunks.get(chunks.size()-1);progress.setMaximum(Math.max(1,last[1]));progress.setValue(last[0]);progress.setString("Searching "+last[0]+" / "+last[1]);}protected void done(){try{List<NpcCatalogEntry> found=get();showResults(found);detail.setText(found.size()+" result(s). Select one to check model, material, and animation compatibility.");}catch(Exception e){detail.setText(e.toString());}finally{busy("",false);}}}.execute();}
    private void diagnose(NpcCatalogEntry entry){if(entry==null||working)return;busy("Checking NPC "+entry.id+"…",true);new SwingWorker<String,Void>(){protected String doInBackground()throws Exception{try(AnimationWorkspace workspace=new AnimationWorkspace(cachePath,entry.id)){TextureDiagnostics530.Report report=TextureDiagnostics530.analyze(workspace.baseModel,workspace.npc,workspace.textures);List<CombatCandidate> candidates=AnimationDiscovery.combatCandidates(workspace);int stand=workspace.bas==null?workspace.npc.standingAnimation:workspace.bas.standingAnimation,walk=workspace.bas==null?workspace.npc.walkingAnimation:workspace.bas.walkingAnimation;return"Compatibility: "+report.summary()+" | standing "+stand+", walking "+walk+" | likely combat candidates (review required): "+candidates.stream().limit(4).map(c->Integer.toString(c.sequenceId)).collect(java.util.stream.Collectors.joining(", "));}}protected void done(){try{detail.setText(get());}catch(Exception e){detail.setText("Unsupported: "+root(e));}finally{busy("",false);}}}.execute();}
    private void showResults(List<NpcCatalogEntry> results){model.clear();for(NpcCatalogEntry entry:results)model.addElement(entry);}
    private void busy(String text,boolean active){working=active;progress.setIndeterminate(active);progress.setVisible(active);progress.setString(text);list.setEnabled(!active);query.setEnabled(!active);}
    private void closeCatalog(){if(catalog!=null)try{catalog.close();}catch(Exception ignored){}}
    private static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.toString();}
}
