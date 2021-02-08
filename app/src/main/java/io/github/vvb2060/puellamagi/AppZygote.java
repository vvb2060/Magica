package io.github.vvb2060.puellamagi;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;

public final class AppZygote implements ZygotePreload {
    public void doPreload(ApplicationInfo appInfo) {
        System.loadLibrary("magica");
    }
}
