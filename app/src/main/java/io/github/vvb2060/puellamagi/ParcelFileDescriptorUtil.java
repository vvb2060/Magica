package io.github.vvb2060.puellamagi;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ParcelFileDescriptorUtil {

    public static ParcelFileDescriptor pipeFrom(InputStream inputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        var outputStream= new ParcelFileDescriptor.AutoCloseOutputStream(writeSide);
        new TransferThread(inputStream,outputStream).start();

        return readSide;
    }

    public static ParcelFileDescriptor pipeTo(OutputStream outputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        var inputStream=new ParcelFileDescriptor.AutoCloseInputStream(readSide);
        new TransferThread(inputStream, outputStream).start();

        return writeSide;
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            var buf = new byte[8192];
            int len;
            try {
                while ((len = mIn.read(buf)) > 0) {
                    mOut.write(buf, 0, len);
                    mOut.flush();
                }
            } catch (IOException e) {
                Log.e(App.TAG, "TransferThread", e);
            } finally {
                try {
                    mIn.close();
                } catch (IOException e) {
                    Log.e(App.TAG, "TransferThread: mIn", e);
                }
                try {
                    mOut.close();
                } catch (IOException e) {
                    Log.e(App.TAG, "TransferThread: mOut", e);
                }
            }
        }
    }
}
