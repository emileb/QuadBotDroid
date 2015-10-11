package com.emtronics.quadbotdroid;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.List;

/**
 * Created by Emile Belanger on 04/09/2015.
 */
public class H264CameraServer {

    final static String TAG = "H264CameraServer";

    int width;
    int height;

    Camera camera;
    int frame = 0;

    SurfaceTexture surfaceTexture;

    final byte[] encodedData = new byte[100000];

    interface ReceiveData
    {
        void serverReceiveRata(byte[] data);
    }

    ReceiveData callback;

    boolean shutDown = false;

    H264CameraServer(Context ctx,ReceiveData callback,boolean frontCamera,int width,int height)
    {
        this.width = width;
        this.height = height;
        this.callback = callback;

        //Load C++ libraries
        libx264.load();
        libx264.initEncoder(width, height);

        if (frontCamera)
            camera = Camera.open(findFrontFacingCamera());
        else
            camera = Camera.open(findBackFacingCamera());

        //Setup colour space, this matches what the h264 encoder wants
        Camera.Parameters p = camera.getParameters();
        p.setPreviewFormat(ImageFormat.YV12);

        //List available sizes
        List<Camera.Size> sizes = p.getSupportedPreviewSizes();
        for(Camera.Size s: sizes)
        {
            Log.d(TAG, "Size = " + s.width + "x" + s.height);
        }

        p.setPreviewSize(width,height);
        camera.setParameters(p);

        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;
               // Log.d(TAG, "camera callback " + width + " " + height);

                libx264.setFrame(data);

                //int size = libx264.encodeFrame(encodedData);
                //Log.d(TAG, "size = " + size);
                //libx264.decodeFrame(encodedData);
                //libx264.fillBitmap(bitmap);
                //imageView.setImageBitmap(bitmap);
                frame++;
            }
        });

        //Need a dummy texture
        surfaceTexture = new SurfaceTexture(10);

        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutDown()
    {
        if (camera !=null)
        {
            camera.release();
            camera = null;
            shutDown = true;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    ZMQ.Socket server;
    public void start()
    {
        Thread thread = new Thread() {
            @Override
            public void run() {

                ZMQ.Context context = ZMQ.context(1);
                server = context.socket(ZMQ.REP);

                Log.d(TAG, "Starting server");
                server.bind("tcp://*:1234");
                Log.d(TAG, "..Server connected");

                int size = 0;
                while (!shutDown) {

                    //Wait for recv
                   // Log.d(TAG, "..receive");
                    server.setReceiveTimeOut(1000);
                    byte [] recv = server.recv(1000);


                    //Send the data
                   // Log.d(TAG, "..send");
                    if (recv != null) {
                        server.send(encodedData, 0, size, 0);

                        if (callback != null)
                            callback.serverReceiveRata(recv);

                        size = libx264.encodeFrame(encodedData);
                    }


                   // Log.d(TAG, "..encode");

                    //Log.d(TAG, "size = " + size + " " + encodedData[0])
                }
                Log.d(TAG, "Closing server");

                server.close();
            }
        };
        thread.start();


        camera.startPreview();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;

                break;
            }
        }
        return cameraId;
    }
}
