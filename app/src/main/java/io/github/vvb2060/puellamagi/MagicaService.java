package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import io.github.vvb2060.magica.lib.MagicaRoot;

import java.io.IOException;
import java.util.List;

public final class MagicaService extends Service {
    private Process process;
    private final IRemoteService.Stub binder = new IRemoteService.Stub() {

        @Override
        public IRemoteProcess getRemoteProcess() {
            return new RemoteProcessHolder(process);
        }

        @Override
        public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() {
            return getSystemService(ActivityManager.class).getRunningAppProcesses();
        }
    };

    public IBinder onBind(Intent intent) {
        try {
            MagicaRoot.root();
            process = Runtime.getRuntime().exec("sh");
            return binder;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}
