package io.github.vvb2060.magica.lib;

public final class MagicaRoot {
    static {
        System.loadLibrary("magica");
    }

    /**
     * Escalate privilege to root on Android 10+ with SELinux permissive.
     * This method uses capability manipulation to gain root access.
     */
    public static native void root();

    private MagicaRoot() {
    }
}

