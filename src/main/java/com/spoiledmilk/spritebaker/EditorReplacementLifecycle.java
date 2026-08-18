package com.spoiledmilk.spritebaker;

/** Distinguishes a programmatic editor replacement from an intentional close. */
final class EditorReplacementLifecycle {
    private boolean replacing;
    void beginReplacement(){replacing=true;}
    boolean shouldReturnToNpcBrowser(boolean transientDesktop){return transientDesktop&&!replacing;}
}
