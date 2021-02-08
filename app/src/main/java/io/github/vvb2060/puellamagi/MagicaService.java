package io.github.vvb2060.puellamagi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;
import java.io.IOException;

public final class MagicaService extends Service {
    final IRemoteService.Stub binder = new IRemoteService.Stub() {

        @Override
        public IRemoteProcess exec(String[] command, String[] envp, String dir) {
            try {
                root();
                var file = dir == null ? null : new File(dir);
                var proc = Runtime.getRuntime().exec(command, envp, file);
                return new RemoteProcessHolder(proc);
            } catch (IOException e) {
                return null;
            }
        }
    };

    native static void root();

    public IBinder onBind(Intent intent) {
        return binder;
    }
}
