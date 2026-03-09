LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE            := lsplt
LOCAL_SRC_FILES         := elf_util.cc lsplt.cc
LOCAL_C_INCLUDES        := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(BUILD_STATIC_LIBRARY)
