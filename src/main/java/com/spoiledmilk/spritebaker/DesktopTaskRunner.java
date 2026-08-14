package com.spoiledmilk.spritebaker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** Serial background lane for cache access, rendering, and export; callbacks always return to Swing. */
public final class DesktopTaskRunner implements AutoCloseable {
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"sprite-baker-background");t.setDaemon(true);return t;});private final AtomicInteger generation=new AtomicInteger(),latestGeneration=new AtomicInteger();private final Listener listener;private Future<?> latest;private String latestLabel;private volatile boolean closed;
    public DesktopTaskRunner(Listener listener){this.listener=listener;}
    public synchronized <T> int submit(String label,Task<T> task,Consumer<T> success,Consumer<Exception> failure){if(closed)return-1;int ticket=generation.incrementAndGet();SwingUtilities.invokeLater(()->{if(!closed)listener.started(label);});executor.submit(()->{try{T value=task.run();SwingUtilities.invokeLater(()->{if(!closed){listener.finished(label);success.accept(value);}});}catch(Exception e){SwingUtilities.invokeLater(()->{if(!closed){listener.finished(label);failure.accept(e);}});}});return ticket;}
    public synchronized <T> int submitLatest(String label,Task<T> task,Consumer<T> success,Consumer<Exception> failure){if(closed)return-1;if(latest!=null&&latest.cancel(false)){String canceled=latestLabel;SwingUtilities.invokeLater(()->{if(!closed)listener.finished(canceled);});}int ticket=generation.incrementAndGet();latestGeneration.set(ticket);latestLabel=label;SwingUtilities.invokeLater(()->{if(!closed)listener.started(label);});latest=executor.submit(()->{try{T value=task.run();SwingUtilities.invokeLater(()->{if(!closed){listener.finished(label);if(ticket==latestGeneration.get())success.accept(value);}});}catch(Exception e){SwingUtilities.invokeLater(()->{if(!closed){listener.finished(label);if(ticket==latestGeneration.get())failure.accept(e);}});}});return ticket;}
    public int generation(){return generation.get();}
    @Override public synchronized void close(){closed=true;executor.shutdownNow();}
    @FunctionalInterface public interface Task<T>{T run()throws Exception;}
    public interface Listener{void started(String label);void finished(String label);}
}
