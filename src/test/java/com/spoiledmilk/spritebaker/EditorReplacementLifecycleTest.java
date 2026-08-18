package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EditorReplacementLifecycleTest {
    @Test void userCloseReturnsTransientDesktopToNpcBrowser(){assertTrue(new EditorReplacementLifecycle().shouldReturnToNpcBrowser(true));}
    @Test void programmaticReplacementNeverReopensBrowser(){EditorReplacementLifecycle lifecycle=new EditorReplacementLifecycle();lifecycle.beginReplacement();assertFalse(lifecycle.shouldReturnToNpcBrowser(true));assertFalse(lifecycle.shouldReturnToNpcBrowser(false));}
}
