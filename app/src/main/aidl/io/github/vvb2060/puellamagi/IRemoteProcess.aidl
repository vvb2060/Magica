package io.github.vvb2060.puellamagi;

interface IRemoteProcess {
    ParcelFileDescriptor  getOutputStream();
    ParcelFileDescriptor getInputStream();
    ParcelFileDescriptor getErrorStream();
    int waitFor();
    int exitValue();
    void destroy();
    boolean isAlive();
}
