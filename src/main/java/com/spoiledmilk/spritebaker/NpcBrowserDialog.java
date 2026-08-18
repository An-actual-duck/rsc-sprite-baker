package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Explicit-search NPC browser with on-demand compatibility diagnosis. */
final class NpcBrowserDialog extends JDialog {
    static final int RESULT_CAP=250;
    static final String EMPTY_INSTRUCTION="Enter part of an NPC name, or one exact numeric NPC ID, then press Search or Enter.";

    private final Path cachePath;
    private final Consumer<NpcCatalogEntry> selection;
    private final DefaultListModel<NpcCatalogEntry> model=new DefaultListModel<>();
    private final JList<NpcCatalogEntry> list=new JList<>(model);
    private final JTextField query=new JTextField(30);
    private final JButton search=new JButton("Search");
    private final JTextArea detail=new JTextArea(EMPTY_INSTRUCTION);
    private final JProgressBar progress=new JProgressBar();
    private NpcCatalog catalog;
    private SwingWorker<List<NpcCatalogEntry>,int[]> searchWorker;
    private SwingWorker<NpcCompatibility,Void> diagnosisWorker;
    private long requestGeneration;
    private boolean selectedNpc,closed;

    NpcBrowserDialog(Window owner,Path cachePath,Consumer<NpcCatalogEntry> selection){
        super(owner,"Browse NPCs",ModalityType.MODELESS);
        this.cachePath=cachePath;
        this.selection=selection;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(6,6));
        add(searchPanel(),BorderLayout.NORTH);
        list.setVisibleRowCount(18);
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())diagnose(list.getSelectedValue());});
        add(new JScrollPane(list),BorderLayout.CENTER);
        add(bottomPanel(),BorderLayout.SOUTH);
        setSize(900,650);
        setLocationRelativeTo(owner);
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){closeCatalog();}});
        openCatalog();
    }

    boolean selectedNpc(){return selectedNpc;}

    private JPanel searchPanel(){
        JPanel panel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("NPC name or exact ID"));
        panel.add(query);
        query.addActionListener(e->startSearch());
        search.addActionListener(e->startSearch());
        search.setEnabled(false);
        panel.add(search);
        return panel;
    }

    private JPanel bottomPanel(){
        JPanel bottom=new JPanel(new BorderLayout());
        detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setOpaque(false);detail.setPreferredSize(new Dimension(820,64));
        bottom.add(detail,BorderLayout.CENTER);
        progress.setStringPainted(true);progress.setVisible(false);
        bottom.add(progress,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton choose=new JButton("Load Selected NPC");
        choose.addActionListener(e->{NpcCatalogEntry entry=list.getSelectedValue();if(entry!=null){selectedNpc=true;selection.accept(entry);dispose();}});
        actions.add(choose);
        JButton close=new JButton("Close");close.addActionListener(e->dispose());actions.add(close);
        bottom.add(actions,BorderLayout.SOUTH);
        return bottom;
    }

    private void openCatalog(){
        showProgress("Indexing NPC IDs…",true);
        new SwingWorker<NpcCatalog,Void>(){
            protected NpcCatalog doInBackground()throws Exception{return new NpcCatalog(cachePath);}
            protected void done(){
                if(closed)return;
                try{catalog=get();search.setEnabled(true);showProgress("",false);showEmptyInstruction();}
                catch(Exception e){detail.setText(root(e));showProgress("",false);}
            }
        }.execute();
    }

    private void startSearch(){
        if(catalog==null||closed)return;
        String text=query.getText().trim();
        long token=supersedeWorkers();
        if(text.isEmpty()){showProgress("",false);showEmptyInstruction();return;}
        model.clear();
        detail.setText("Searching…");
        showProgress(NpcCatalog.exactId(text)==null?"Searching definitions…":"Loading exact NPC ID…",true);
        searchWorker=new SwingWorker<>(){
            protected List<NpcCatalogEntry> doInBackground()throws Exception{
                return catalog.search(text,RESULT_CAP,(done,total)->publish(new int[]{done,total}),this::isCancelled);
            }
            protected void process(List<int[]> chunks){
                if(token!=requestGeneration)return;
                int[] last=chunks.get(chunks.size()-1);progress.setIndeterminate(false);progress.setMaximum(Math.max(1,last[1]));progress.setValue(last[0]);progress.setString("Searching "+last[0]+" / "+last[1]);
            }
            protected void done(){
                if(token!=requestGeneration)return;
                try{
                    if(isCancelled())return;
                    List<NpcCatalogEntry> found=get();showResults(found);
                    detail.setText(found.isEmpty()?"No NPCs match that search.":found.size()+" result(s)"+(found.size()==RESULT_CAP?" (result cap reached)":"")+". Select one for full compatibility diagnosis.");
                }catch(CancellationException ignored){}catch(Exception e){detail.setText(root(e));}
                finally{if(token==requestGeneration)showProgress("",false);}
            }
        };
        searchWorker.execute();
    }

    private void diagnose(NpcCatalogEntry entry){
        if(entry==null||catalog==null||closed)return;
        if(diagnosisWorker!=null)diagnosisWorker.cancel(true);
        long token=++requestGeneration;
        if(searchWorker!=null)searchWorker.cancel(true);
        showProgress("Checking NPC "+entry.id+"…",true);
        diagnosisWorker=new SwingWorker<>(){
            protected NpcCompatibility doInBackground()throws Exception{return catalog.assess(entry.id);}
            protected void done(){
                if(token!=requestGeneration||isCancelled()||list.getSelectedValue()!=entry)return;
                try{NpcCompatibility result=get();entry.compatibility(result.category.display);list.repaint();detail.setText(result.summary()+" | standing "+result.standingSequenceId+", walking "+result.walkingSequenceId+" | materials "+result.materialIds);}
                catch(CancellationException ignored){}catch(Exception e){detail.setText("Unsupported: "+root(e));}
                finally{if(token==requestGeneration)showProgress("",false);}
            }
        };
        diagnosisWorker.execute();
    }

    private long supersedeWorkers(){
        requestGeneration++;
        if(searchWorker!=null)searchWorker.cancel(true);
        if(diagnosisWorker!=null)diagnosisWorker.cancel(true);
        return requestGeneration;
    }
    private void showEmptyInstruction(){model.clear();detail.setText(EMPTY_INSTRUCTION);}
    private void showResults(List<NpcCatalogEntry> results){model.clear();for(NpcCatalogEntry entry:results)model.addElement(entry);}
    private void showProgress(String text,boolean active){progress.setIndeterminate(active);progress.setVisible(active);progress.setString(text);}
    private void closeCatalog(){
        closed=true;search.setEnabled(false);supersedeWorkers();NpcCatalog closing=catalog;catalog=null;
        if(closing!=null)new SwingWorker<Void,Void>(){protected Void doInBackground(){try{closing.close();}catch(Exception ignored){}return null;}}.execute();
    }
    private static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.toString();}
}
