package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.InvocationTargetException;

public final class CommandReceiver extends BroadcastReceiver {
    public static final String ACTION_EXEC = "io.github.vvb2060.puellamagi.action.EXEC";
    public static final String EXTRA_COMMAND = "cmd";

    // Keys used to pass command results through the ResultReceiver bundle.
    static final String KEY_RESULT = "result";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_EXEC.equals(intent.getAction())) {
            return;
        }
        Log.d(TAG, String.valueOf(intent));
        try {
            String mSentFromPackage = (String) HiddenApiBypass.invoke(BroadcastReceiver.class, this, "getSentFromPackage");
            int mSentFromUid = (int) HiddenApiBypass.invoke(BroadcastReceiver.class, this, "getSentFromUid");
            Log.d(TAG, "packageName: " + mSentFromPackage + "(" + mSentFromUid + ")");
            if (mSentFromUid != context.getApplicationInfo().uid && mSentFromUid != 0 && mSentFromUid != 1000 && mSentFromUid != 2000) {
                Log.w(TAG, "Ignoring EXEC broadcast from unauthorized UID: " + mSentFromUid);
                return;
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Failed to get sender", e);
            return;
        }

        var command = intent.getStringExtra(EXTRA_COMMAND);

        // BroadcastReceivers are NOT allowed to bind to services.
        // Instead, we start MagicaService (isolatedProcess + useAppZygote, already rooted)
        // and pass a ResultReceiver so the service can send the command output back.
        var pending = goAsync();
        var receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                pending.setResultCode(resultCode);
                if (resultData != null) {
                    pending.setResultData(resultData.getString(KEY_RESULT, ""));
                }
                pending.finish();
            }
        };

        // Target CommandService (non-isolated): startForegroundService is allowed from
        // background, unlike startService. CommandService holds a persistent binding to
        // the isolated MagicaService (which cannot be started directly) and proxies the
        // command there.
        var serviceIntent = new Intent(context, CommandService.class);
        serviceIntent.setAction(CommandService.ACTION_EXEC);
        serviceIntent.putExtra(CommandService.EXTRA_COMMAND, command);
        serviceIntent.putExtra(CommandService.EXTRA_RESULT_RECEIVER, receiver);
        try {
            context.startForegroundService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start CommandService", e);
            pending.setResultCode(1);
            pending.setResultData("exec failed: " + e.getMessage());
            pending.finish();
        }
    }
}

