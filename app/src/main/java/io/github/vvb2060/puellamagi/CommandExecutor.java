package io.github.vvb2060.puellamagi;

import io.github.vvb2060.magica.lib.MagicaRoot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final class CommandExecutor {

    private CommandExecutor() {
    }

    static String normalize(String command) {
        if (command == null) {
            return "";
        }
        return command.trim();
    }

    static String execute(String command) throws IOException {
        var normalized = normalize(command);

        MagicaRoot.root();
        var process = Runtime.getRuntime().exec(new String[]{"sh", "-c", normalized});
        var stdout = readAll(process.getInputStream());
        var stderr = readAll(process.getErrorStream());

        try {
            int code = process.waitFor();
            var output = stdout.isEmpty() ? stderr : stdout;
            if (output.isEmpty()) {
                output = "<empty output>";
            }
            return "exit=" + code + ", output=" + output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted", e);
        } finally {
            process.destroy();
        }
    }

    private static String readAll(java.io.InputStream in) throws IOException {
        var sb = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

