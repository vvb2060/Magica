package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        var action = intent.getAction();
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }
        try {
            context.startService(new Intent(context, MagicaService.class));
            Log.i(TAG, "MagicaService started from boot action: " + action);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to start MagicaService from boot action: " + action, e);
        }
        try {
            context.startService(new Intent(context, CommandService.class));
            Log.i(TAG, "CommandService started from boot action: " + action);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to start CommandService from boot action: " + action, e);
        }
    }
}

