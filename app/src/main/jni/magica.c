#include <jni.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/capability.h>
#include <xhook.h>
#include "logging.h"
#include "android_filesystem_config.h"

#define arraysize(array) (sizeof(array)/sizeof(array[0]))

static void print_cap() {
    ssize_t length;
    cap_t cap_d = cap_get_proc();
    if (cap_d == NULL) {
        LOGE("Failed to get cap: %s", strerror(errno));
    } else {
        char *result = cap_to_text(cap_d, &length);
        LOGI("Capabilities: %s\n", result);
        cap_free(result);
        result = NULL;
        cap_free(cap_d);
    }
}

static void change_cap(cap_flag_value_t flag) {
    cap_t working = cap_get_proc();
    cap_flag_value_t status = CAP_CLEAR;
    cap_get_flag(working, CAP_SETPCAP, CAP_EFFECTIVE, &status);
    if (status == CAP_SET) {
        const cap_value_t raise_cap[] = {CAP_SETGID, CAP_SETUID, CAP_SETPCAP};
        cap_set_flag(working, CAP_EFFECTIVE, arraysize(raise_cap), raise_cap, flag);
        cap_set_flag(working, CAP_PERMITTED, arraysize(raise_cap), raise_cap, flag);
        cap_set_flag(working, CAP_INHERITABLE, arraysize(raise_cap), raise_cap, flag);
        if (cap_set_proc(working)) {
            PLOGE("change_cap: %d", flag);
        }
    } else {
        LOGI("Skip change_cap: %d", flag);
    }
    cap_free(working);
}

static int skip_capset(cap_user_header_t header __unused, cap_user_data_t data __unused) {
    LOGI("Skip capset");
    print_cap();
    return 0;
}

static void root(JNIEnv *env  __unused, jclass clazz __unused) {
    change_cap(CAP_SET);
    print_cap();
    if (setresuid(AID_ROOT, AID_ROOT, AID_ROOT)) {
        PLOGE("setresuid");
        if (getuid() % AID_USER_OFFSET >= AID_ISOLATED_START) {
            change_cap(CAP_CLEAR);
            print_cap();
        }
    } else if (geteuid() == AID_ROOT) {
        LOGI("We Are Root!!!");
        if (setresgid(AID_ROOT, AID_ROOT, AID_ROOT)) {
            PLOGE("setresgid");
        }
        gid_t groups[] = {AID_SYSTEM, AID_ADB, AID_LOG, AID_INPUT, AID_INET,
                          AID_NET_BT, AID_NET_BT_ADMIN, AID_SDCARD_R, AID_SDCARD_RW,
                          AID_NET_BW_STATS, AID_READPROC, AID_UHID, AID_EXT_DATA_RW,
                          AID_EXT_OBB_RW};
        if (setgroups(arraysize(groups), groups)) {
            PLOGE("setgroups");
        }
    }
}

static void signal_handler(int sig) {
    if (sig != SIGSYS) return;
    LOGW("received signal: SIGSYS, setresuid fail");
    if (getuid() % AID_USER_OFFSET >= AID_ISOLATED_START) {
        change_cap(CAP_CLEAR);
        print_cap();
    }
}

jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    jclass clazz;

    if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if ((clazz = (*env)->FindClass(env, "io/github/vvb2060/puellamagi/MagicaService")) == NULL) {
        return JNI_ERR;
    }

    JNINativeMethod methods[] = {{"root", "()V", root}};
    if ((*env)->RegisterNatives(env, clazz, methods, 1) < 0) {
        return JNI_ERR;
    }

    signal(SIGSYS, signal_handler);

    xhook_register(".*\\libandroid_runtime.so$", "capset", skip_capset, NULL);
    if (xhook_refresh(0)) PLOGE("xhook");

    return JNI_VERSION_1_6;
}
