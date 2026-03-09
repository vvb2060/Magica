package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ScrollView;

import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.github.vvb2060.puellamagi.databinding.ActivityMainBinding;

public final class MainActivity extends Activity {
    private IRemoteService server;
    private Shell shell;
    private ActivityMainBinding binding;
    private final List<String> console = new AppendCallbackList();
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            console.add(getString(R.string.service_connected));
            server = IRemoteService.Stub.asInterface(binder);
            try {
                var process = server.getRemoteProcess();
                shell = Shell.Builder.create()
                        .setFlags(Shell.FLAG_NON_ROOT_SHELL)
                        .build(new RemoteProcess(process));
            } catch (RemoteException e) {
                console.add(Log.getStackTraceString(e));
                return;
            }

            check();
            getRunningAppProcesses();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            server = null;
            console.add(getString(R.string.service_disconnected));
        }
    };

    private boolean bind() {
        try {
            return bindIsolatedService(
                    new Intent(this, MagicaService.class),
                    Context.BIND_AUTO_CREATE,
                    "magica",
                    getMainExecutor(),
                    connection
            );
        } catch (Exception e) {
            Log.e(TAG, "Can not bind service", e);
            return false;
        }
    }

    void getRunningAppProcesses() {
        try {
            var processes = server.getRunningAppProcesses();
            console.add("uid pid processName pkgList importance");
            for (var process : processes) {
                var str = String.format(Locale.ROOT, "%d %d %s %s %d",
                        process.uid, process.pid, process.processName,
                        Arrays.toString(process.pkgList), process.importance);
                console.add(str);
            }
        } catch (RemoteException | SecurityException e) {
            console.add(Log.getStackTraceString(e));
        }
    }

    void cmd(String... cmds) {
        shell.newJob().add(cmds).to(console, console).submit(out -> {
            if (!out.isSuccess()) {
                console.add(Arrays.toString(cmds) + getString(R.string.exec_failed));
            }
        });
    }

    void check() {
        cmd("id");
        cmd("cat /proc/self/status");
        if (shell.isRoot()) {
            console.add(getString(R.string.root_shell_opened));
        } else {
            console.add(getString(R.string.cannot_open_root_shell));
            return;
        }

        checkAdbRoot();
    }

    void checkAdbRoot() {
        var debuggable = SystemProperties.getBoolean("ro.debuggable", false);
        var adbRoot = SystemProperties.getBoolean("service.adb.root", false);
        if (debuggable && adbRoot) {
            console.add(getString(R.string.adb_root_enabled));
            binding.install.setVisibility(View.GONE);
        } else if (debuggable) {
            console.add(getString(R.string.adb_root_debuggable));
            binding.install.setVisibility(View.GONE);
        } else {
            console.add(getString(R.string.adb_root_disabled));
            binding.install.setVisibility(View.VISIBLE);
            binding.install.setEnabled(true);
        }
    }

    private void resetprop() {
        ApplicationInfo info;
        try {
            info = getPackageManager().getApplicationInfo("com.topjohnwu.magisk", 0);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                info = getPackageManager().getApplicationInfo("io.github.vvb2060.magisk", 0);
            } catch (PackageManager.NameNotFoundException ex) {
                console.add(getString(R.string.magisk_package_not_installed));
                console.add(getString(R.string.requires_latest_magisk_app));
                return;
            }
        }

        var magisk = info.nativeLibraryDir + "/libmagisk.so";
        cmd("ln -fs " + magisk + " /dev/resetprop");
        var context = ShellUtils.fastCmd(shell, "/dev/resetprop -Z ro.debuggable");
        shell.newJob()
                .add("chmod 0644 /dev/__properties__/properties_serial",
                        "chmod 0644 /dev/__properties__/" + context,
                        "/dev/resetprop -n ro.debuggable 1",
                        "rm /dev/resetprop",
                        "chmod 0444 /dev/__properties__/properties_serial",
                        "chmod 0444 /dev/__properties__/" + context,
                        "setprop service.adb.root 1",
                        "setprop ctl.restart adbd")
                .to(console, console)
                .submit(out -> checkAdbRoot());
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        var rootView = binding.getRoot();
        if (Build.VERSION.SDK_INT >= 35) {
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                var systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                return insets;
            });
        }
        binding.install.setOnClickListener(v -> {
            binding.install.setEnabled(false);
            resetprop();
        });
        setContentView(rootView);
        console.add(getString(R.string.start_service, Boolean.toString(bind())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            unbindService(connection);
        }
    }

    class AppendCallbackList extends CallbackList<String> {
        @Override
        public void onAddElement(String s) {
            binding.console.append(s);
            binding.console.append("\n");
            binding.sv.postDelayed(() -> binding.sv.fullScroll(ScrollView.FOCUS_DOWN), 10);
        }
    }
}
