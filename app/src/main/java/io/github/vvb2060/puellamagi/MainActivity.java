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
            console.add("服务已连接");
            App.server = IRemoteService.Stub.asInterface(binder);
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            shell = Shell.Builder.create().setFlags(Shell.FLAG_NON_ROOT_SHELL).build();
            check();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            App.server = null;
            console.add("服务连接断开");
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
            if (!out.isSuccess()) console.add(Arrays.toString(cmds) + " 执行失败");
        });
    }

    void check() {
        cmd("id");
        if (shell.isRoot()) {
            console.add("已打开 Root Shell");
        } else {
            console.add("无法打开 Root Shell");
            return;
        }

        var cmd = "ps -A 2>/dev/null | grep magiskd | grep -qv grep";
        var magiskd = ShellUtils.fastCmdResult(shell, cmd);
        if (magiskd) {
            console.add("magiskd 正在运行，点按下方按钮杀死magiskd，丢失Root权限，重启设备恢复");
            killMagiskd();
        } else {
            console.add("magiskd 未运行，准备刷入Magisk");
            installMagisk();
        }
    }


    @SuppressLint("SetTextI18n")
    void killMagiskd() {
        binding.install.setOnClickListener(v -> {
            var cmd = "kill -9 $(pidof magiskd)";
            if (ShellUtils.fastCmdResult(shell, cmd)) console.add("已杀死 magiskd");
            else console.add("杀死 magiskd 失败");
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
            console.add("没有安装完整版Magisk软件包(com.topjohnwu.magisk)");
            console.add("需要安装最新(Canary) Magisk应用");
            return;
        }

        var cmd = "mkdir -p /dev/tmp/magica; unzip -o " + info.publicSourceDir +
                " META-INF/com/google/android/update-binary -d /dev/tmp/magica;" +
                "sh /dev/tmp/magica/META-INF/com/google/android/update-binary dummy 1 " + info.publicSourceDir;

        try {
            var apk = new ZipFile(info.publicSourceDir);
            var update = apk.getEntry("META-INF/com/google/android/update-binary");
            if (update != null) {
                console.add("点按下方按钮刷入Magisk");
                binding.install.setOnClickListener(v -> {
                    shell.newJob().add(cmd).to(console).submit(out -> {
                        if (out.isSuccess()) {
                            console.add("完成，点按下方按钮重启");
                            binding.install.setOnClickListener(a -> cmd("reboot"));
                            binding.install.setText("Reboot");
                            binding.install.setEnabled(true);
                        } else {
                            console.add("刷入失败");
                        }
                    });
                    binding.install.setEnabled(false);
                });
                binding.install.setText("Flash Magisk");
                binding.install.setVisibility(View.VISIBLE);
            } else {
                console.add("需要安装最新(Canary) Magisk应用");
            }
        } catch (IOException e) {
            Log.e(TAG, "installMagisk", e);
            console.add("无法解压Magisk安装包");
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        console.add("启动服务：" + bind());
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
