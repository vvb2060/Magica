LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := magica
LOCAL_SRC_FILES        := magica.c
LOCAL_SHARED_LIBRARIES += bytehook
LOCAL_CFLAGS           := -Wall -Wextra -Werror
LOCAL_CONLYFLAGS       := -std=c18
LOCAL_LDLIBS           := -llog
LOCAL_STATIC_LIBRARIES := libcap
include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/bytehook)

include src/main/jni/libcap/Android.mk

