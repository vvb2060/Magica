package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * A non-isolated persistent service that holds a binding to MagicaService and
 * executes commands on behalf of CommandReceiver.
 * <p>
 * BroadcastReceivers cannot bind to services, and isolated services (MagicaService)
 * cannot be started via startService. This service bridges the gap:
 *   CommandReceiver --startForegroundService--> CommandService --IPC--> MagicaService (isolated+rooted)
 */
public final class CommandService extends Service {
    public static final String ACTION_EXEC = "io.github.vvb2060.puellamagi.action.CMD_EXEC";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";

    private static final String CHANNEL_ID = "magica_cmd";
    private static final int NOTIFICATION_ID = 1;

    private IRemoteService remoteService;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            remoteService = IRemoteService.Stub.asInterface(binder);
            Log.i(TAG, "CommandService: MagicaService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
            Log.w(TAG, "CommandService: MagicaService disconnected, rebinding...");
            // Rebind so we're ready for the next command.
            bindToMagicaService();
        }
    };

    private void bindToMagicaService() {
        try {
            bindIsolatedService(
                    new Intent(this, MagicaService.class),
                    Context.BIND_AUTO_CREATE,
                    "magica",
                    getMainExecutor(),
                    connection
            );
        } catch (Exception e) {
            Log.e(TAG, "CommandService: failed to bind MagicaService", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        bindToMagicaService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_EXEC.equals(intent.getAction())) {
            var command = intent.getStringExtra(EXTRA_COMMAND);
            var receiver = (ResultReceiver) intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
            execCommand(command, receiver);
        }
        return START_STICKY;
    }

    private void execCommand(String command, ResultReceiver receiver) {
        if (remoteService == null) {
            Log.e(TAG, "CommandService: remoteService not ready yet");
            sendResult(receiver, 1, "exec failed: MagicaService not connected");
            return;
        }
        try {
            var result = remoteService.execCommand(command);
            sendResult(receiver, 0, result);
        } catch (Exception e) {
            var message = "exec failed: " + e.getMessage();
            Log.w(TAG, message, e);
            sendResult(receiver, 1, message);
        }
    }

    private static void sendResult(ResultReceiver receiver, int code, String data) {
        if (receiver == null) return;
        var bundle = new Bundle();
        bundle.putString(CommandReceiver.KEY_RESULT, data);
        receiver.send(code, bundle);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            unbindService(connection);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void createNotificationChannel() {
        var channel = new NotificationChannel(
                CHANNEL_ID, "Magica", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Magica")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true)
                .build();
    }
}

