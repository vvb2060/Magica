package io.github.vvb2060.puellamagi;

import io.github.vvb2060.puellamagi.IRemoteProcess;

interface IRemoteService {
    IRemoteProcess getRemoteProcess();
    List<RunningAppProcessInfo> getRunningAppProcesses();
}
