package com.spoiledmilk.spritebaker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Debounced metadata-filtered browser with on-demand compatibility diagnosis. */
final class NpcBrowserDialog extends JDialog {
    static final int RESULT_CAP=250, DEBOUNCE_MILLIS=300;
    static final String EMPTY_INSTRUCTION="Type an NPC name or exact ID, or select one or more metadata filters.";

    private final Path cachePath;
    private final Consumer<NpcCatalogEntry> selection;
    private final DefaultListModel<NpcCatalogEntry> model=new DefaultListModel<>();
    private final JList<NpcCatalogEntry> list=new JList<>(model);
    private final JTextField query=new JTextField(26);
    private final JTextArea detail=new JTextArea(EMPTY_INSTRUCTION);
    private final JProgressBar progress=new JProgressBar();
    private final JComboBox<NpcSearchCriteria.MatchMode> matchMode=new JComboBox<>(NpcSearchCriteria.MatchMode.values());
    private final Map<NpcSearchCriteria.Tag,JCheckBox> filters=new EnumMap<>(NpcSearchCriteria.Tag.class);
    private final Timer debounce;
    private final NpcSearchState searchState=new NpcSearchState();
    private NpcCatalog catalog;
    private SwingWorker<List<NpcCatalogEntry>,int[]> searchWorker;
    private SwingWorker<NpcCompatibility,Void> diagnosisWorker;
    private boolean selectedNpc,closed;

    NpcBrowserDialog(Window owner,Path cachePath,Consumer<NpcCatalogEntry> selection){
        super(owner,"Browse NPCs",ModalityType.MODELESS);
        this.cachePath=cachePath;
        this.selection=selection;
        debounce=createDebounce(this::startSearch);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(6,6));
        add(searchPanel(),BorderLayout.NORTH);
        list.setVisibleRowCount(18);
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())diagnose(list.getSelectedValue());});
        add(new JScrollPane(list),BorderLayout.CENTER);
        add(bottomPanel(),BorderLayout.SOUTH);
        setSize(980,700);
        setLocationRelativeTo(owner);
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){closeCatalog();}});
        openCatalog();
    }

    boolean selectedNpc(){return selectedNpc;}
    static Timer createDebounce(Runnable action){Timer timer=new Timer(DEBOUNCE_MILLIS,e->action.run());timer.setRepeats(false);return timer;}

    private JPanel searchPanel(){
        JPanel panel=new JPanel(new BorderLayout(4,4));
        JPanel textRow=new JPanel(new FlowLayout(FlowLayout.LEFT));
        textRow.add(new JLabel("Name or exact NPC ID"));
        textRow.add(query);
        query.addActionListener(e->{debounce.stop();supersedeWorkers();startSearch();});
        query.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){criteriaChanged(true);}
            public void removeUpdate(DocumentEvent e){criteriaChanged(true);}
            public void changedUpdate(DocumentEvent e){criteriaChanged(true);}
        });
        textRow.add(new JLabel("Match selected tags"));
        matchMode.addActionListener(e->criteriaChanged(false));
        textRow.add(matchMode);
        panel.add(textRow,BorderLayout.NORTH);

        JPanel tags=new JPanel(new GridLayout(0,4,8,3));
        tags.setBorder(BorderFactory.createTitledBorder("Metadata filters"));
        for(NpcSearchCriteria.Tag tag:NpcSearchCriteria.Tag.values()){
            JCheckBox box=new JCheckBox(tag.label);
            box.addActionListener(e->criteriaChanged(false));
            filters.put(tag,box);
            tags.add(box);
        }
        panel.add(tags,BorderLayout.CENTER);
        return panel;
    }

    private JPanel bottomPanel(){
        JPanel bottom=new JPanel(new BorderLayout());
        detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setOpaque(false);detail.setPreferredSize(new Dimension(900,72));
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
                try{
                    catalog=get();showProgress("",false);
                    if(criteria().isEmpty())showEmptyInstruction();else startSearch();
                }catch(Exception e){detail.setText(root(e));showProgress("",false);}
            }
        }.execute();
    }

    private void criteriaChanged(boolean typed){
        supersedeWorkers();
        model.clear();
        NpcSearchCriteria criteria=criteria();
        if(criteria.isEmpty()){debounce.stop();showProgress("",false);showEmptyInstruction();return;}
        detail.setText(typed?"Waiting for typing…":"Applying filters…");
        if(catalog==null)return;
        if(typed)debounce.restart();else{debounce.stop();startSearch();}
    }

    private void startSearch(){
        if(catalog==null||closed)return;
        NpcSearchCriteria criteria=criteria();
        if(criteria.isEmpty()){showEmptyInstruction();return;}
        long token=searchState.current();
        showProgress(criteria.exactId()==null?"Searching definitions…":"Loading exact NPC ID…",true);
        searchWorker=new SwingWorker<>(){
            protected List<NpcCatalogEntry> doInBackground()throws Exception{
                return catalog.search(criteria,RESULT_CAP,(done,total)->publish(new int[]{done,total}),this::isCancelled);
            }
            protected void process(List<int[]> chunks){
                if(!searchState.isCurrent(token))return;
                int[] last=chunks.get(chunks.size()-1);progress.setIndeterminate(false);progress.setMaximum(Math.max(1,last[1]));progress.setValue(last[0]);progress.setString("Searching "+last[0]+" / "+last[1]);
            }
            protected void done(){
                if(!searchState.isCurrent(token)||isCancelled())return;
                try{
                    List<NpcCatalogEntry> found=get();showResults(found);showProgress("",false);
                    detail.setText(found.isEmpty()?"No NPCs match the current text and metadata filters.":found.size()+" result(s)"+(found.size()==RESULT_CAP?" (result cap reached)":"")+". Select one for full compatibility diagnosis.");
                }catch(CancellationException ignored){}catch(Exception e){detail.setText(root(e));showProgress("",false);}
            }
        };
        searchWorker.execute();
    }

    private void diagnose(NpcCatalogEntry entry){
        if(entry==null||catalog==null||closed)return;
        if(diagnosisWorker!=null)diagnosisWorker.cancel(true);
        long token=searchState.current();
        showProgress("Checking NPC "+entry.id+"…",true);
        diagnosisWorker=new SwingWorker<>(){
            protected NpcCompatibility doInBackground()throws Exception{return catalog.assess(entry.id);}
            protected void done(){
                if(!searchState.isCurrent(token)||isCancelled()||list.getSelectedValue()!=entry)return;
                try{NpcCompatibility result=get();entry.compatibility(result.category.display);list.repaint();detail.setText(result.summary()+" | standing "+result.standingSequenceId+", walking "+result.walkingSequenceId+" | materials "+result.materialIds);}
                catch(CancellationException ignored){}catch(Exception e){detail.setText("Unsupported: "+root(e));}
                finally{showProgress("",false);}
            }
        };
        diagnosisWorker.execute();
    }

    private NpcSearchCriteria criteria(){
        java.util.EnumSet<NpcSearchCriteria.Tag> selected=java.util.EnumSet.noneOf(NpcSearchCriteria.Tag.class);
        filters.forEach((tag,box)->{if(box.isSelected())selected.add(tag);});
        return new NpcSearchCriteria(query.getText(),(NpcSearchCriteria.MatchMode)matchMode.getSelectedItem(),selected);
    }

    private void supersedeWorkers(){
        searchState.supersede();
        if(searchWorker!=null)searchWorker.cancel(true);
        if(diagnosisWorker!=null)diagnosisWorker.cancel(true);
    }
    private void showEmptyInstruction(){model.clear();detail.setText(EMPTY_INSTRUCTION);}
    private void showResults(List<NpcCatalogEntry> results){model.clear();for(NpcCatalogEntry entry:results)model.addElement(entry);}
    private void showProgress(String text,boolean active){progress.setIndeterminate(active);progress.setVisible(active);progress.setString(text);}
    private void closeCatalog(){
        closed=true;debounce.stop();supersedeWorkers();NpcCatalog closing=catalog;catalog=null;
        if(closing!=null)new SwingWorker<Void,Void>(){protected Void doInBackground(){try{closing.close();}catch(Exception ignored){}return null;}}.execute();
    }
    private static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.toString();}
}
