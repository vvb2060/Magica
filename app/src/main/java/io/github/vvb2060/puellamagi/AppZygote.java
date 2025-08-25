package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.util.Log;

public final class AppZygote implements ZygotePreload {
    public void doPreload(ApplicationInfo appInfo) {
        long pid = Process.myPid();
        Log.e(TAG, "PID of preloading App Zygote is " + pid);

        System.loadLibrary("magica");
    }
}
