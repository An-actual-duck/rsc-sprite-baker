package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class DesktopTaskRunnerTest {
    @Test void workRunsOffEventThreadAndCallbackReturnsToIt()throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicBoolean workOnEdt=new AtomicBoolean(true),callbackOnEdt=new AtomicBoolean();DesktopTaskRunner runner=new DesktopTaskRunner(new DesktopTaskRunner.Listener(){public void started(String label){}public void finished(String label){}});SwingUtilities.invokeAndWait(()->runner.submit("neutral",()->{workOnEdt.set(SwingUtilities.isEventDispatchThread());return 7;},value->{callbackOnEdt.set(SwingUtilities.isEventDispatchThread());assertEquals(7,value);done.countDown();},error->{done.countDown();fail(error);}));assertTrue(done.await(5,TimeUnit.SECONDS));runner.close();assertFalse(workOnEdt.get());assertTrue(callbackOnEdt.get());}
}
