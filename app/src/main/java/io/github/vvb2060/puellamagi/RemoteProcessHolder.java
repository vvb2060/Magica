package io.github.vvb2060.puellamagi;

import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class RemoteProcessHolder extends IRemoteProcess.Stub {

    private final Process process;
    private ParcelFileDescriptor in;
    private ParcelFileDescriptor out;
    private ParcelFileDescriptor err;

    public RemoteProcessHolder(Process process) {
        this.process = process;
    }

    @Override
    public ParcelFileDescriptor getOutputStream() {
        if (out == null) {
            try {
                out = ParcelFileDescriptorUtil.pipeTo(process.getOutputStream());
            } catch (IOException e) {
                return null;
            }
        }
        return out;
    }

    @Override
    public ParcelFileDescriptor getInputStream() {
        if (in == null) {
            try {
                in = ParcelFileDescriptorUtil.pipeFrom(process.getInputStream());
            } catch (IOException e) {
                return null;
            }
        }
        return in;
    }

    @Override
    public ParcelFileDescriptor getErrorStream() {
        if (err == null) {
            try {
                err = ParcelFileDescriptorUtil.pipeFrom(process.getErrorStream());
            } catch (IOException e) {
                return null;
            }
        }
        return err;
    }

    @Override
    public int waitFor() {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            return -1;
        }
    }

    @Override
    public int exitValue() {
        int exit;
        try {
            exit = process.exitValue();
        } catch (IllegalThreadStateException e) {
            exit = -1;
        }
        return exit;
    }

    @Override
    public void destroy() {
        process.destroy();
    }

    @Override
    public boolean isAlive() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

}
