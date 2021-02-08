LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE            := xhook
LOCAL_SRC_FILES         := xhook.c xh_core.c xh_elf.c
LOCAL_SRC_FILES         += xh_log.c xh_util.c xh_version.c
LOCAL_CFLAGS            := -Wall -Wextra -Wshadow -Werror
LOCAL_CONLYFLAGS        := -std=c11
LOCAL_C_INCLUDES        := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_EXPORT_LDLIBS     := -llog
include $(BUILD_STATIC_LIBRARY)
