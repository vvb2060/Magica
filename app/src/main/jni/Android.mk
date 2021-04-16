LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := magica
LOCAL_SRC_FILES        := magica.c
LOCAL_CFLAGS           := -Wall -Wextra -Werror
LOCAL_CONLYFLAGS       := -std=c18
LOCAL_LDLIBS           := -llog
LOCAL_STATIC_LIBRARIES := libcap xhook
include $(BUILD_SHARED_LIBRARY)

include src/main/jni/libcap/Android.mk
include src/main/jni/xhook/Android.mk
