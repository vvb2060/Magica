LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := magica
LOCAL_SRC_FILES        := magica.cpp
LOCAL_LDLIBS           := -llog
LOCAL_STATIC_LIBRARIES := lsplt
include $(BUILD_SHARED_LIBRARY)

include src/main/jni/lsplt/Android.mk
