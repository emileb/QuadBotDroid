extern "C"
{
#include <stdio.h>
#include <stdint.h>
#include <unistd.h>
#include <x264.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
}

#define WIDTH       640
#define HEIGHT      480
#define FPS         30
#define BITRATE     400000
#define RTP_ADDRESS "127.0.0.1"
//#define RTP_ADDRESS "192.168.0.8"
#define RTP_TYPE "rtp"
#define RTP_PORT    9999

struct AVFormatContext* avctx;
struct x264_t* encoder;
struct SwsContext* imgctx;

uint8_t test = 0x80;


void create_sample_picture(x264_picture_t* picture)
{
    // create a frame to store in
    x264_picture_alloc(picture, X264_CSP_I420, WIDTH, HEIGHT);

    // fake image generation
    // disregard how wrong this is; just writing a quick test
    int strides = WIDTH / 8;
    uint8_t* data = malloc(WIDTH * HEIGHT * 3);
    memset(data, test, WIDTH * HEIGHT * 3);
    test = (test << 1) | (test >> (8 - 1));

    // scale the image
    sws_scale(imgctx, (const uint8_t* const*) &data, &strides, 0, HEIGHT,
              picture->img.plane, picture->img.i_stride);

    free(data);
}

int encode_frame(x264_picture_t* picture, x264_nal_t** nals)
{
    // encode a frame
    x264_picture_t pic_out;
    int num_nals=0;
    int frame_size = x264_encoder_encode(encoder, nals, &num_nals, picture, &pic_out);

    // ignore bad frames
    if (frame_size < 0)
    {
        printf("bad frame");
        return frame_size;
    }

    return num_nals;
}

void stream_frame(uint8_t* payload, int size)
{
    // initalize a packet
    AVPacket p;
    av_init_packet(&p);
    p.data = payload;
    p.size = size;
    p.stream_index = 0; //Needs to be > 0 for TCP???
    p.flags = AV_PKT_FLAG_KEY;
    p.pts = AV_NOPTS_VALUE;
    p.dts = AV_NOPTS_VALUE;
   // p.pts = 5;
   // p.dts = 5;
    // send it out
    av_interleaved_write_frame(avctx, &p);
    //av_write_frame(avctx, &p);
}

int stream_test()
{
    // initalize ffmpeg
    avcodec_register_all();
    av_register_all();
    avformat_network_init();

    // set up image scaler
    // (in-width, in-height, in-format, out-width, out-height, out-format, scaling-method, 0, 0, 0)
    imgctx = sws_getContext(WIDTH, HEIGHT, PIX_FMT_MONOWHITE,
                            WIDTH, HEIGHT, PIX_FMT_YUV420P,
                            SWS_FAST_BILINEAR, NULL, NULL, NULL);

    // set up encoder presets
    x264_param_t param;
    x264_param_default_preset(&param, "ultrafast", "zerolatency");

    param.i_threads = 3;
    param.i_width = WIDTH;
    param.i_height = HEIGHT;
    param.i_fps_num = FPS;
    param.i_fps_den = 1;
    param.i_keyint_max = FPS;
    param.b_intra_refresh = 0;
    param.rc.i_bitrate = BITRATE;
    param.b_repeat_headers = 1; // whether to repeat headers or write just once
    param.b_annexb = 1;         // place start codes (1) or sizes (0)

    // initalize
    x264_param_apply_profile(&param, "high");
    encoder = x264_encoder_open(&param);

    // at this point, x264_encoder_headers can be used, but it has had no effect

    // set up streaming context. a lot of error handling has been ommitted
    // for brevity, but this should be pretty standard.
    avctx = avformat_alloc_context();
    struct AVOutputFormat* fmt = av_guess_format("rtp", NULL, NULL);
    avctx->oformat = fmt;


    snprintf(avctx->filename, sizeof(avctx->filename), "%s://%s:%d",RTP_TYPE, RTP_ADDRESS, RTP_PORT);

    printf("url = %s",avctx->filename);

    if (avio_open(&avctx->pb, avctx->filename, AVIO_FLAG_WRITE) < 0)
    {
        printf("url_fopen failed");
        return 1;
    }


    struct AVStream* stream = av_new_stream(avctx, 1);

    // initalize codec
    AVCodecContext* c = stream->codec;
    c->codec_id = CODEC_ID_H264;
    c->codec_type = AVMEDIA_TYPE_VIDEO;
    c->flags = CODEC_FLAG_GLOBAL_HEADER;
    c->width = WIDTH;
    c->height = HEIGHT;
    c->time_base.den = FPS;
    c->time_base.num = 1;
    c->gop_size = FPS;
    c->bit_rate = BITRATE;
//    avctx->flags = AVFMT_FLAG_RTP_HINT;

    // write the header
    //av_write_header(avctx);
    // send it out
    avformat_write_header(avctx, NULL);

    // make some frames
    for (int frame = 0; frame < 10000; frame++)
    {
        // create a sample moving frame
        x264_picture_t* pic = (x264_picture_t*) malloc(sizeof(x264_picture_t));
        create_sample_picture(pic);

        // encode the frame
        x264_nal_t* nals;
        int num_nals = encode_frame(pic, &nals);

        if (num_nals < 0)
            printf("invalid frame size: %d\n", num_nals);

        // send out NALs
        for (int i = 0; i < num_nals; i++)
        {
            printf("nal=%d, size = %d",i,nals[i].i_payload);
            stream_frame(nals[i].p_payload, nals[i].i_payload);
        }

        // free up resources
        x264_picture_clean(pic);
        free(pic);

        // stream at approx 30 fps
        printf("frame %d\n", frame);
        usleep(33333);
    }

    return 0;
}