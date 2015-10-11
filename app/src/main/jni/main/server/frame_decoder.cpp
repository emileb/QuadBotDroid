#include "frame_common.h"
#include "x264decoder.h"

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"JNI", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "JNI", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR,"JNI", __VA_ARGS__))

#define LOGI(...)

static int width,height;

static DataPacked *data;
static x264Decoder *x264_decoder;

static char * frame;

extern "C"
{

jint
Java_com_emtronics_quadbotdroid_libx264_initDecoder(JNIEnv *env,
                                                    jobject thiz, jint w, jint h) {
    LOGI("initDecoder %d %d",w,h);
    width = w;
    height = h;

    frame = new char[w*h*3];

    x264_decoder = new x264Decoder();
    x264_decoder->initialize(width, height);
}



jint
Java_com_emtronics_quadbotdroid_libx264_decodeFrame( JNIEnv* env,
                                                     jobject thiz,
                                                     jobject encodedBuffer)
{
    char * javaBuffer = (char*)env->GetByteArrayElements(encodedBuffer,NULL);

    //The network library adds 4 bad bytes to the beginning??
    //encoded_buffer +=4;

    data = (DataPacked *)javaBuffer;

    LOGI("decodeFrame, color_size_ = %d",data->color_size_);

    return  x264_decoder->decodeFrame(data->data_, data->color_size_, frame);

    env->ReleaseByteArrayElements(encodedBuffer, javaBuffer, 0);
}


jint
Java_com_emtronics_quadbotdroid_libx264_fillBitmap( JNIEnv* env,
                                                     jobject thiz,
                                                     jobject bitmap)
{
    LOGI("fillBitmap");
    char* destination = 0;

    AndroidBitmapInfo  info;

    if ((AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed !");
        return 1;
    }

    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);

    if (destination == 0)
    {
        LOGE("Failed to lock bitmap");
        return 1;
    }
   // memcpy(destination,frame,width*height*3);


    uint32_t* line;
    char * framePrt = frame;
    for(int yy = 0; yy < info.height; yy++){
        line = (uint32_t*)destination;
        for(int xx =0; xx < info.width; xx++){

            int red, green, blue;
            //manipulate each value
            red = *framePrt++;
            green = *framePrt++;
            blue = *framePrt++;

            // set the new pixel back in
            line[xx] =
                    0xFF000000 |
                    ((red << 16) & 0x00FF0000) |
                    ((green << 8) & 0x0000FF00) |
                    (blue & 0x000000FF);
        }

        destination = (char*)destination + info.stride;
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}

}