TOP_DIR := $(call my-dir)


LOCAL_PATH := $(call my-dir)


include $(TOP_DIR)/ffmpeg/Android.mk

include $(TOP_DIR)/main/Android.mk
