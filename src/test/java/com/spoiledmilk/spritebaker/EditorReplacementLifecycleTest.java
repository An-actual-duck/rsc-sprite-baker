package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EditorReplacementLifecycleTest {
    @Test void userCloseExitsTransientDesktop(){assertTrue(new EditorReplacementLifecycle().shouldExitDesktop(true));}
    @Test void programmaticReplacementNeverExitsDesktop(){EditorReplacementLifecycle lifecycle=new EditorReplacementLifecycle();lifecycle.beginReplacement();assertFalse(lifecycle.shouldExitDesktop(true));assertFalse(lifecycle.shouldExitDesktop(false));}
}
