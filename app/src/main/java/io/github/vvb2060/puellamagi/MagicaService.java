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
    public static final String ACTION_EXEC_WHITELIST = "io.github.vvb2060.puellamagi.action.EXEC_WHITELIST";
    public static final String EXTRA_COMMAND = "command";

    private final Object lock = new Object();
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

        @Override
        public String execWhitelistedCommand(String command) {
            try {
                return CommandExecutor.execute(command);
            } catch (IllegalArgumentException | IOException e) {
                return "exec failed: " + e.getMessage();
            }
        }
    };

    private void ensureRootShell() throws IOException {
        synchronized (lock) {
            if (process != null && process.isAlive()) {
                return;
            }
            MagicaRoot.root();
            process = Runtime.getRuntime().exec("sh");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            ensureRootShell();
            if (intent != null && ACTION_EXEC_WHITELIST.equals(intent.getAction())) {
                var result = CommandExecutor.execute(intent.getStringExtra(EXTRA_COMMAND));
                Log.i(TAG, "exec: " + result);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Rejected non-whitelisted command", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize root shell in onStartCommand", e);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        try {
            ensureRootShell();
            return binder;
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize root shell in onBind", e);
            return null;
        }
    }

    @Override
    public void onDestroy() {
        synchronized (lock) {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
        super.onDestroy();
    }
}
