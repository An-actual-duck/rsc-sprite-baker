package com.spoiledmilk.spritebaker;

/** Distinguishes a programmatic editor replacement from an intentional desktop close. */
final class EditorReplacementLifecycle {
    private boolean replacing;
    void beginReplacement(){replacing=true;}
    boolean shouldExitDesktop(boolean transientDesktop){return transientDesktop&&!replacing;}
}
