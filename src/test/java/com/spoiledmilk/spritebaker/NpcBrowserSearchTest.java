package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NpcBrowserSearchTest {
    @Test void startsWithAnExplicitSearchInstructionAndNoImplicitQuery(){
        assertTrue(NpcBrowserDialog.EMPTY_INSTRUCTION.contains("press Search or Enter"));
        assertEquals(250,NpcBrowserDialog.RESULT_CAP);
        assertNull(NpcCatalog.exactId(""));
    }

    @Test void onlyAnEntireUnsignedDecimalQuerySelectsOneExactId(){
        assertEquals(1615,NpcCatalog.exactId(" 1615 "));
        assertEquals(0,NpcCatalog.exactId("0"));
        assertNull(NpcCatalog.exactId("1615 demon"));
        assertNull(NpcCatalog.exactId("-1"));
        assertNull(NpcCatalog.exactId("999999999999999999999"));
    }
}
