
#include <jni.h>
#include <android/log.h>

extern "C"
{
#include <libavutil/opt.h>
#include <libavcodec/avcodec.h>
#include <libavutil/channel_layout.h>
#include <libavutil/common.h>
#include <libavutil/imgutils.h>
#include <libavutil/mathematics.h>
#include <libavutil/samplefmt.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>


#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO,"JNI", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "JNI", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR,"JNI", __VA_ARGS__))


int printf ( const char * format, ... )
{
    va_list argptr;
    va_start(argptr, format);

    char string[512];
    vsprintf(string, format, argptr);
    LOGI("%s",string);
    return 0;
}

int fprintf (FILE * x, const char * format, ... )
{
    va_list argptr;
    va_start(argptr, format);

    char string[512];
    vsprintf(string, format, argptr);
    LOGI("%s",string);
    return 0;
}
}

#define WIDTH       352
#define HEIGHT      288
#define FPS         25
#define BITRATE     400000
#define RTP_ADDRESS "127.0.0.1"
#define RTP_PORT    5678


static struct AVFormatContext* avctx;
static void stream_frame(uint8_t* payload, int size)
{
    // initalize a packet
    AVPacket p;
    av_init_packet(&p);
    p.data = payload;
    p.size = size;
    p.stream_index = 0;
    p.flags = AV_PKT_FLAG_KEY;
    p.pts = AV_NOPTS_VALUE;
    p.dts = AV_NOPTS_VALUE;

    // send it out
    av_interleaved_write_frame(avctx, &p);
}

static void setup_stream(int codec_id)
{
    avformat_network_init();

    // set up streaming context. a lot of error handling has been ommitted
    // for brevity, but this should be pretty standard.
    avctx = avformat_alloc_context();
    struct AVOutputFormat* fmt = av_guess_format("rtp", NULL, NULL);
    avctx->oformat = fmt;

    snprintf(avctx->filename, sizeof(avctx->filename), "rtp://%s:%d", RTP_ADDRESS, RTP_PORT);
    if (avio_open(&avctx->pb, avctx->filename, AVIO_FLAG_WRITE) < 0)
    {
        printf("url_fopen failed");
        return ;
    }
    struct AVStream* stream = av_new_stream(avctx, 1);

    // initalize codec
    AVCodecContext* c = stream->codec;
    c->codec_id = codec_id;
    c->codec_type = AVMEDIA_TYPE_VIDEO;
    c->flags = CODEC_FLAG_GLOBAL_HEADER;
    c->width = WIDTH;
    c->height = HEIGHT;
    c->time_base.den = FPS;
    c->time_base.num = 1;
    c->gop_size = FPS;
    c->bit_rate = BITRATE;
    // avctx->flags = AVFMT_FLAG_RTP_HINT;


    // Write header.
    avformat_write_header(avctx, NULL);
}

static void video_encode_example(const char *filename, int codec_id)
{
    AVCodec *codec;
    AVCodecContext *c= NULL;
    int i, ret, x, y, got_output;
    FILE *f;
    AVFrame *frame;
    AVPacket pkt;
    uint8_t endcode[] = { 0, 0, 1, 0xb7 };

    printf("Encode video file %s\n", filename);

    avcodec_register_all();

    /* find the mpeg1 video encoder */
    codec = avcodec_find_encoder(codec_id);
    if (!codec) {
        fprintf(stderr, "Codec not found\n");
        exit(1);
    }

    c = avcodec_alloc_context3(codec);
    if (!c) {
        fprintf(stderr, "Could not allocate video codec context\n");
        exit(1);
    }

    /* put sample parameters */
    c->bit_rate = 400000;
    /* resolution must be a multiple of two */
    c->width = 352;
    c->height = 288;
    /* frames per second */
    c->time_base = (AVRational){1,25};
    /* emit one intra frame every ten frames
     * check frame pict_type before passing frame
     * to encoder, if frame->pict_type is AV_PICTURE_TYPE_I
     * then gop_size is ignored and the output of encoder
     * will always be I frame irrespective to gop_size
     */
    c->gop_size = 10;
    c->max_b_frames = 1;
    c->pix_fmt = AV_PIX_FMT_YUV420P;

    if (codec_id == AV_CODEC_ID_H264)
        av_opt_set(c->priv_data, "preset", "slow", 0);

    /* open it */
    if (avcodec_open2(c, codec, NULL) < 0) {
        fprintf(stderr, "Could not open codec\n");
        exit(1);
    }

    f = fopen(filename, "wb");
    if (!f) {
        fprintf(stderr, "Could not open %s\n", filename);
        exit(1);
    }

    frame = av_frame_alloc();
    if (!frame) {
        fprintf(stderr, "Could not allocate video frame\n");
        exit(1);
    }
    frame->format = c->pix_fmt;
    frame->width  = c->width;
    frame->height = c->height;

    /* the image can be allocated by any means and av_image_alloc() is
     * just the most convenient way if av_malloc() is to be used */
    ret = av_image_alloc(frame->data, frame->linesize, c->width, c->height,
                         c->pix_fmt, 32);
    if (ret < 0) {
        fprintf(stderr, "Could not allocate raw picture buffer\n");
        exit(1);
    }

    setup_stream(codec_id);

    /* encode 1 second of video */
    for (i = 0; i < 25; i++) {
        av_init_packet(&pkt);
        pkt.data = NULL;    // packet data will be allocated by the encoder
        pkt.size = 0;

        fflush(stdout);
        /* prepare a dummy image */
        /* Y */
        for (y = 0; y < c->height; y++) {
            for (x = 0; x < c->width; x++) {
                frame->data[0][y * frame->linesize[0] + x] = x + y + i * 3;
            }
        }

        /* Cb and Cr */
        for (y = 0; y < c->height/2; y++) {
            for (x = 0; x < c->width/2; x++) {
                frame->data[1][y * frame->linesize[1] + x] = 128 + y + i * 2;
                frame->data[2][y * frame->linesize[2] + x] = 64 + x + i * 5;
            }
        }

        frame->pts = i;

        /* encode the image */
        ret = avcodec_encode_video2(c, &pkt, frame, &got_output);
        if (ret < 0) {
            fprintf(stderr, "Error encoding frame\n");
            exit(1);
        }

        if (got_output) {
            printf("Write frame %3d (size=%5d)\n", i, pkt.size);
            fwrite(pkt.data, 1, pkt.size, f);
            av_free_packet(&pkt);
        }
    }

    /* get the delayed frames */
    for (got_output = 1; got_output; i++) {
        fflush(stdout);

        ret = avcodec_encode_video2(c, &pkt, NULL, &got_output);
        if (ret < 0) {
            fprintf(stderr, "Error encoding frame\n");
            exit(1);
        }

        if (got_output) {
            printf("Write frame %3d (size=%5d)\n", i, pkt.size);
            fwrite(pkt.data, 1, pkt.size, f);
            av_free_packet(&pkt);
        }
    }

    /* add sequence end code to have a real mpeg file */
    fwrite(endcode, 1, sizeof(endcode), f);
    fclose(f);

    avcodec_close(c);
    av_free(c);
    av_freep(&frame->data[0]);
    av_frame_free(&frame);
    printf("\n");
}

int stream_test();

extern "C" jint
Java_com_emtronics_ffmpegserver_ffmpeg_loadFile( JNIEnv* env,
                                                 jobject thiz, jstring jfile)
{

    stream_test();

    return;


    const char *filename = "/sdcard/myvideo1.mp4";

    video_encode_example(filename,AV_CODEC_ID_MPEG1VIDEO);
    return 0;
}