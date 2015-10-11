#include "frame_common.h"
#include "x264encoder.h"

#include <jni.h>
#include <android/log.h>


#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"JNI", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "JNI", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR,"JNI", __VA_ARGS__))

#define LOGI(...)

static int width,height;

static DataPacked *data;
static x264Encoder *x264_encoder;

static char *frame;

//Pack the NALs created by x264 into a single packet.
static unsigned int pack_rgb_data(DataPacked *data){
    unsigned int tmp_size = 0;

    x264_nal_t nal;
    while(x264_encoder->isNalsAvailableInOutputQueue()){
        nal = x264_encoder->getNalUnit();
        memcpy(&(data->data_[tmp_size]), nal.p_payload, nal.i_payload);
        tmp_size += nal.i_payload;
    }
    data->color_size_ = tmp_size;
    //Size of DataPacked after data insert
    return sizeof(uint32_t) * 2 + data->color_size_;
}


extern "C"
{

jint
Java_com_emtronics_quadbotdroid_libx264_initEncoder( JNIEnv* env,
                                                     jobject thiz,jint w,jint h)
{
    LOGI("initEncoder %d %d",w,h);

    width = w;
    height = h;

    frame = new char[w*h*3];

    data = new DataPacked();

    data->frame_id_ = 0x11223344;

    x264_encoder = new x264Encoder();

    x264_encoder->initialize(w, h);
}

jint
Java_com_emtronics_quadbotdroid_libx264_setFrame(JNIEnv* env,
                                                    jobject thiz,
                                                    jobject data)
{
    char * javaBuffer = (char*)env->GetByteArrayElements(data,NULL);
    int lengthOfArray = env->GetArrayLength( data);
   // LOGI("setFrame %d",lengthOfArray);
    memcpy(frame,javaBuffer,lengthOfArray);

    env->ReleaseByteArrayElements(data, javaBuffer, JNI_ABORT);
}

jint
Java_com_emtronics_quadbotdroid_libx264_encodeFrame(JNIEnv* env,
                                                    jobject thiz,
                                                    jobject encodedBufferOut)
{
    LOGI("encodeFrame");

    x264_encoder->encodeFrame(frame, width*height*3);

    unsigned int message_size = pack_rgb_data(data);

    char * javaBuffer = (char*)env->GetByteArrayElements(encodedBufferOut,NULL);

    memcpy(javaBuffer,data,message_size);

    env->ReleaseByteArrayElements(encodedBufferOut, javaBuffer, 0);
    return message_size;
}

}