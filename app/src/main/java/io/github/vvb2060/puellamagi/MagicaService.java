package io.github.vvb2060.puellamagi;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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

    native static void root();


    public IBinder onBind(Intent intent) {
        root();
        try {
            process = Runtime.getRuntime().exec("sh");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return binder;
    }
}
