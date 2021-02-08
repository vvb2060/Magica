package io.github.vvb2060.puellamagi;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;


public class RemoteProcess extends Process {
    private final IRemoteProcess mRemote;
    private OutputStream os;
    private InputStream is;
    private InputStream es;

    public RemoteProcess(IRemoteProcess remote) {
        mRemote = remote;
    }

    @Override
    @Nullable
    public OutputStream getOutputStream() {
        if (os == null) {
            try {
                os = new ParcelFileDescriptor.AutoCloseOutputStream(mRemote.getOutputStream());
            } catch (RemoteException e) {
                return null;
            }
        }
        return os;
    }

    @Override
    @Nullable
    public InputStream getInputStream() {
        if (is == null) {
            try {
                is = new ParcelFileDescriptor.AutoCloseInputStream(mRemote.getInputStream());
            } catch (RemoteException e) {
                return null;
            }
        }
        return is;
    }

    @Override
    @Nullable
    public InputStream getErrorStream() {
        if (es == null) {
            try {
                es = new ParcelFileDescriptor.AutoCloseInputStream(mRemote.getErrorStream());
            } catch (RemoteException e) {
                return null;
            }
        }
        return es;
    }

    @Override
    public int waitFor() throws InterruptedException {
        try {
            var exit = mRemote.waitFor();
            if (exit == -1) throw new InterruptedException();
            return exit;
        } catch (RemoteException e) {
            return -1;
        }
    }

    @Override
    public int exitValue() {
        try {
            if (isAlive()) throw new IllegalThreadStateException();
            var exit = mRemote.exitValue();
            if (exit == -1) throw new IllegalThreadStateException();
            return exit;
        } catch (RemoteException e) {
            return -1;
        }
    }

    @Override
    public void destroy() {
        try {
            mRemote.destroy();
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public boolean isAlive() {
        try {
            return mRemote.isAlive();
        } catch (RemoteException e) {
            return false;
        }
    }
}
