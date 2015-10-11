package com.emtronics.quadbotdroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.ImageView;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Created by Emile Belanger on 04/09/2015.
 */
public class H264Client {

    final static String TAG = "H264Client";

    int width = 640;
    int height = 480;

    final byte[] encodedData = new byte[100000];

    final Activity ctx;

    Bitmap bitmap;
    Matrix m = new Matrix();

    boolean stop = false;

    H264Client(Activity ctx,int width,int height)
    {
        this.width = width;
        this.height = height;

        this.ctx = ctx;

        bitmap =  Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        m.preScale(-1, 1);

    }

    byte[] bytesToSend = new byte[0];

    void setSendData(byte[] bytes)
    {
        bytesToSend = bytes;
    }

    ImageView imageView1,imageView2;
    public void start(final String address,ImageView iv1,ImageView iv2)
    {
        imageView1 = iv1;
        imageView2 = iv2;

        imageView1.setScaleX(-1);
        imageView2.setScaleX(-1);

        Thread thread = new Thread() {
            @Override
            public void run() {

                //Load C++ libraries
                libx264.load();
                libx264.initDecoder(width, height);

                ZContext context = new ZContext();

                ZMQ.Socket client = context.createSocket(ZMQ.REQ);
                assert (client != null);


                Log.d(TAG, "Connecting to server " + address);
                client.connect(address);
                Log.d(TAG, "..connected");
                byte[] bytes = new byte[10000];

                while (!stop) {

                    client.send(bytesToSend);

                  //  int ammount = client.recvByteBuffer(encodedData, 0);
                   // int ammount = client.recv(bytes,0,10000,0);
                   // encodedData.put(bytes);

                    int ammount = client.recv(encodedData,0,encodedData.length,0);

                    //int ammount =  libx264.encodeFrame(encodedData);

                    //NOTE!! There are 4 extra empty bytes at the beinging for some reason?
                    //This is fixed the C++ code in decodeFrame by adding 4 on to the pointer..

                   // Log.d(TAG, "Received " + ammount + " " + encodedData[0]);

                    libx264.decodeFrame(encodedData);
                    libx264.fillBitmap(bitmap);

                    ctx.runOnUiThread(new Runnable() {
                        public void run() {
                           // imageView.setScaleType(ImageView.ScaleType.MATRIX);
                            //imageView.setImageMatrix(m);
                            imageView1.setImageBitmap(bitmap);
                            imageView2.setImageBitmap(bitmap);
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void stop()
    {
        stop = true;
    }
}
