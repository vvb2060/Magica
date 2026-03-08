package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

public final class CommandReceiver extends BroadcastReceiver {
    public static final String ACTION_EXEC = "io.github.vvb2060.puellamagi.action.EXEC";
    public static final String EXTRA_COMMAND = "cmd";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_EXEC.equals(intent.getAction())) {
            return;
        }

        var command = intent.getStringExtra(EXTRA_COMMAND);
        try {
            var result = CommandExecutor.execute(command);
            setResultCode(0);
            setResultData(result);
        } catch (IllegalArgumentException | IOException e) {
            var message = "exec failed: " + e.getMessage();
            Log.w(TAG, message, e);
            setResultCode(1);
            setResultData(message);
        }
    }
}

