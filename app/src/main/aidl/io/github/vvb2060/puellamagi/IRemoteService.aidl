package io.github.vvb2060.puellamagi;

import io.github.vvb2060.puellamagi.IRemoteProcess;

interface IRemoteService {
    IRemoteProcess exec(in String[] command, in String[] envp, String dir);
}
