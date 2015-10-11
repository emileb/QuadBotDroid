package com.emtronics.quadbotdroid;

import android.graphics.Bitmap;

/**
 * Created by Emile Belanger on 01/09/2015.
 */
public class libx264 {

    public static void load()
    {
        System.loadLibrary("main");
    }

    native static int initEncoder(int w,int h);

    native static int encodeFrame(byte[] dataOut);

    native static int initDecoder(int w,int h);

    native static int decodeFrame(byte[]  dataIn);

    native static int fillBitmap(Bitmap bitmap);

    native static int  setFrame(byte[] data);
}
