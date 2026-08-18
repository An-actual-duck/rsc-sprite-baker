package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;

class NpcBrowserSearchStateTest {
    @Test void onlyNewestAsynchronousRequestTokenMayApply(){
        NpcSearchState state=new NpcSearchState();long first=state.supersede(),second=state.supersede();
        assertFalse(state.isCurrent(first));assertTrue(state.isCurrent(second));assertEquals(second,state.current());
    }

    @Test void debounceCoalescesRepeatedTypingIntoOneAction()throws Exception{
        CountDownLatch fired=new CountDownLatch(1);AtomicInteger count=new AtomicInteger();
        Timer timer=NpcBrowserDialog.createDebounce(()->{count.incrementAndGet();fired.countDown();});
        SwingUtilities.invokeAndWait(()->{timer.restart();timer.restart();timer.restart();});
        assertFalse(fired.await(NpcBrowserDialog.DEBOUNCE_MILLIS/2,TimeUnit.MILLISECONDS));
        assertTrue(fired.await(2,TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(()->{});
        assertEquals(1,count.get());assertFalse(timer.isRepeats());
    }

    @Test void initialStateHasExplicitInstructionAndNoImplicitCriteria(){
        assertFalse(NpcBrowserDialog.EMPTY_INSTRUCTION.isBlank());
        assertTrue(new NpcSearchCriteria("",NpcSearchCriteria.MatchMode.ALL,java.util.Set.of()).isEmpty());
    }
}
