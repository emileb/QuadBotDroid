LOCAL_PATH := $(call my-dir)



include $(CLEAR_VARS)

LOCAL_MODULE := main


LOCAL_C_INCLUDES := \
$(TOP_DIR)/ffmpeg/include $(TOP_DIR)/ffmpeg/include/x264\

LOCAL_CFLAGS :=-D__STDC_CONSTANT_MACROS


LOCAL_CPPFLAGS := -O2 -g -fexceptions -fpermissive
LOCAL_SRC_FILES :=  main.cpp stream.cpp \
server/x264encoder.cpp server/frame_encoder.cpp server/x264decoder.cpp server/frame_decoder.cpp

LOCAL_STATIC_LIBRARIES := libavformat libavcodec  libavfilter libavutil libavdevice libavfilter libswresample libswscale libx264

LOCAL_LDLIBS :=  -llog -lm -lz -ljnigraphics
include $(BUILD_SHARED_LIBRARY)
