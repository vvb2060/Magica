LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE            := libcap
LOCAL_CFLAGS            := -Wno-pointer-arith -Wno-tautological-compare -Wno-unused-parameter
LOCAL_CFLAGS            += -Wno-unused-result -Wno-unused-variable
LOCAL_SRC_FILES         := cap_alloc.c cap_extint.c cap_file.c
LOCAL_SRC_FILES         += cap_flag.c cap_proc.c cap_text.c
LOCAL_C_INCLUDES        := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(BUILD_STATIC_LIBRARY)
