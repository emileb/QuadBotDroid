# QuadBotDroid

Android app for my robot, see here:

https://www.youtube.com/watch?v=-CQwEM3J5V4

There is this single app which runs on both the robot (server) and the VR phone (client).

This an Android Studio project.

Push into 'QuadbotDroid\app\src\main\jni\main' and type ndk-build to compile the C code.


## Server

The server connects to the USB->Serial device connected to the OTG port.

It opens the camera an encodes the frames in to H264 HALs.

It open a TCP port and waits for a client to connect.

## Client

Connects to the TCP server.

Receives the H264 NALs and decodes them to images to be displayed on screen.

Reads the gyro sensor and gamepad and sends servo/motor postions to the server

## Libraries used

FFMPEG + libx264 - video en/decoding - https://www.ffmpeg.org/

ZeroMQ (Java) - TCP networking - https://github.com/zeromq/jeromq

usb-serial-for-android - The USB library to interface to hardware - https://github.com/mik3y/usb-serial-for-android

raw_rgb_straming - Code used to help with encoding NALs - https://github.com/filippobrizzi/raw_rgb_straming

