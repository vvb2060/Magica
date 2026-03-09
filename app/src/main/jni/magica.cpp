#include <jni.h>
#include <unistd.h>
#include <linux/capability.h>
#include <lsplt.hpp>
#include "logging.h"
#include "android_filesystem_config.h"

#define arraysize(array) (sizeof(array)/sizeof(array[0]))

static int skip_capset(cap_user_header_t header __unused, cap_user_data_t data __unused) {
    LOGD("Skip capset");
    return 0;
}

static void root(JNIEnv *env  __unused, jclass clazz __unused) {
    if (setresuid(AID_ROOT, AID_ROOT, AID_ROOT)) {
        PLOGE("setresuid");
    } else if (geteuid() == AID_ROOT) {
        LOGI("We Are Root!!!");
        if (setresgid(AID_ROOT, AID_ROOT, AID_ROOT)) {
            PLOGE("setresgid");
        }
        gid_t groups[] = {AID_SYSTEM, AID_ADB, AID_LOG, AID_INPUT, AID_INET,
                          AID_NET_BT, AID_NET_BT_ADMIN, AID_SDCARD_R, AID_SDCARD_RW,
                          AID_NET_BW_STATS, AID_READPROC, AID_UHID, AID_EXT_DATA_RW,
                          AID_EXT_OBB_RW, AID_READTRACEFS};
        if (setgroups(arraysize(groups), groups)) {
            PLOGE("setgroups");
        }
    }
}

static void signal_handler(int sig) {
    if (sig != SIGSYS) return;
    LOGW("received signal: SIGSYS, setresuid fail");
}

jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    jclass clazz;

    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if ((clazz = env->FindClass("io/github/vvb2060/puellamagi/MagicaService")) == nullptr) {
        return JNI_ERR;
    }

    JNINativeMethod methods[] = {{"root", "()V", (void *) root}};
    if (env->RegisterNatives(clazz, methods, 1) < 0) {
        return JNI_ERR;
    }

    signal(SIGSYS, signal_handler);

    auto maps = lsplt::MapInfo::Scan();
    bool hook_registered = false;
    for (const auto &map: maps) {
        if (map.path.ends_with("/libandroid_runtime.so")) {
            LOGV("Found: dev=%lu, inode=%lu, path=%s",
                 (unsigned long) map.dev, (unsigned long) map.inode, map.path.c_str());
            if (lsplt::RegisterHook(map.dev, map.inode, "capset", (void *) skip_capset, nullptr)) {
                hook_registered = true;
                LOGD("Hook registered for capset");
                break;
            } else {
                LOGE("Failed to register hook for capset");
            }
        }
    }

    if (hook_registered) {
        if (!lsplt::CommitHook()) {
            PLOGE("lsplt CommitHook failed");
        }
    } else {
        LOGE("libandroid_runtime.so not found or hook registration failed");
    }

    return JNI_VERSION_1_6;
}
