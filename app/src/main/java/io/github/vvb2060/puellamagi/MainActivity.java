package io.github.vvb2060.puellamagi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import io.github.vvb2060.puellamagi.databinding.ActivityMainBinding;

import static io.github.vvb2060.puellamagi.App.TAG;

public final class MainActivity extends Activity {
    private Shell shell;
    private ActivityMainBinding binding;
    private final List<String> console = new AppendCallbackList();
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            console.add(getString(R.string.service_connected));
            App.server = IRemoteService.Stub.asInterface(binder);
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            shell = Shell.Builder.create().setFlags(Shell.FLAG_NON_ROOT_SHELL).build();
            check();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            App.server = null;
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

    void cmd(String... cmds) {
        shell.newJob().add(cmds).to(console).submit(out -> {
            if (!out.isSuccess()) console.add(Arrays.toString(cmds) + getString(R.string.exec_failed));
        });
    }

    void check() {
        cmd("id");
        if (shell.isRoot()) {
            console.add(getString(R.string.root_shell_opened));
        } else {
            console.add(getString(R.string.cannot_open_root_shell));
            return;
        }

        var cmd = "ps -A 2>/dev/null | grep magiskd | grep -qv grep";
        var magiskd = ShellUtils.fastCmdResult(shell, cmd);
        if (magiskd) {
            console.add(getString(R.string.magiskd_running));
            killMagiskd();
        } else {
            console.add(getString(R.string.magiskd_not_running));
            installMagisk();
        }
    }


    @SuppressLint("SetTextI18n")
    void killMagiskd() {
        binding.install.setOnClickListener(v -> {
            var cmd = "kill -9 $(pidof magiskd)";
            if (ShellUtils.fastCmdResult(shell, cmd)) console.add(getString(R.string.magiskd_killed));
            else console.add(getString(R.string.magiskd_failed_to_kill));
            binding.install.setEnabled(false);
        });
        binding.install.setText("Kill magiskd");
        binding.install.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    void installMagisk() {
        ApplicationInfo info;
        try {
            info = getPackageManager().getApplicationInfo("com.topjohnwu.magisk", 0);
        } catch (PackageManager.NameNotFoundException e) {
            console.add(getString(R.string.magisk_package_not_installed));
            console.add(getString(R.string.requires_latest_magisk_app));
            return;
        }

        var cmd = "mkdir -p /dev/tmp/magica; unzip -o " + info.publicSourceDir +
                " META-INF/com/google/android/update-binary -d /dev/tmp/magica;" +
                "sh /dev/tmp/magica/META-INF/com/google/android/update-binary dummy 1 " + info.publicSourceDir;

        try {
            var apk = new ZipFile(info.publicSourceDir);
            var update = apk.getEntry("META-INF/com/google/android/update-binary");
            if (update != null) {
                console.add(getString(R.string.tap_to_install_magisk));
                binding.install.setOnClickListener(v -> {
                    shell.newJob().add(cmd).to(console).submit(out -> {
                        if (out.isSuccess()) {
                            console.add(getString(R.string.tap_to_reboot));
                            binding.install.setOnClickListener(a -> cmd("reboot"));
                            binding.install.setText("Reboot");
                            binding.install.setEnabled(true);
                        } else {
                            console.add(getString(R.string.failed_to_install));
                        }
                    });
                    binding.install.setEnabled(false);
                });
                binding.install.setText("Install Magisk");
                binding.install.setVisibility(View.VISIBLE);
            } else {
                console.add(getString(R.string.requires_latest_magisk_app));
            }
        } catch (IOException e) {
            Log.e(TAG, "installMagisk", e);
            console.add(getString(R.string.cannot_extra_magisk));
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        console.add(getString(R.string.start_service, Boolean.toString(bind())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
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
