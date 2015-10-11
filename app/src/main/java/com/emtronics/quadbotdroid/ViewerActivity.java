package com.emtronics.quadbotdroid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class ViewerActivity extends ActionBarActivity implements SensorEventListener {

    final static String TAG = "ViewerActivity";

    ViewerActivity this_;

    ImageView imageView1;
    ImageView imageView2;

    LinearLayout buttonsLayout;

    private SensorManager mSensorManager;
    private Sensor mOrientation;

    H264Client client;

    boolean zeroMe = false;

    byte[] bytesToSend = new byte[8 + 4 + 4]; //8 servos, direction, speed

    Motion motionController = new Motion();
    ControlsState controlState = new ControlsState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        this_ = this;

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //ActionBar actionBar = getActionBar();
//        actionBar.hide();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);


        // surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        imageView1 = (ImageView)findViewById(R.id.imageView1);
        imageView2 = (ImageView)findViewById(R.id.imageView2);

        buttonsLayout = (LinearLayout)findViewById(R.id.buttons_layout);

        LinearLayout imagesLayout = (LinearLayout)findViewById(R.id.images_layout);

        imagesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonsLayout.getVisibility() == View.VISIBLE)
                    buttonsLayout.setVisibility(View.INVISIBLE);
                else
                    buttonsLayout.setVisibility(View.VISIBLE);
            }
        });

        Button b = (Button)findViewById(R.id.server_connect_button);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (client != null)
                    client.stop();

                client = new H264Client(this_,GD.width,GD.height);
                client.start("tcp://192.168.43.128:1234",imageView1,imageView2);
                //client.start("tcp://192.168.0.101:1234",imageView1,imageView2);
                client.setSendData(bytesToSend);
                //client.start("tcp://localhost:1234",imageView);

            }
        });

        b = (Button)findViewById(R.id.zero_button);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zeroMe = true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {
            Log.d(TAG,"onKey: " + event.getKeyCode());

            if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A)
                zeroMe = true;

            return true;
        }
        else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {


        Log.d("", "onGenericMotionEvent " + controlState.velocity);


        if (Math.abs(event.getAxisValue(MotionEvent.AXIS_X)) > 0.1)
        {
            controlState.velocity = (int)(event.getAxisValue(MotionEvent.AXIS_X) * 150);
            motionController.processControls(Motion.QB_Mode.MODE_360,controlState);
        }
        else {
            controlState.rightX = (int)(event.getAxisValue(MotionEvent.AXIS_Z) * 127);
            controlState.velocity = (int)(event.getAxisValue(MotionEvent.AXIS_GAS) * 150)
                    -(int)(event.getAxisValue(MotionEvent.AXIS_BRAKE) * 150);
            motionController.processControls(Motion.QB_Mode.MODE_NORMAL,controlState);
        }




        bytesToSend[0] = (byte)motionController.BR;
        bytesToSend[1] = (byte)motionController.FR;//
        bytesToSend[2] = (byte)motionController.FL;
        bytesToSend[3] = (byte)motionController.BL;

        bytesToSend[8] = (byte)motionController.R_dir;
        bytesToSend[9] = (byte)motionController.R_dir;
        bytesToSend[10] = (byte)motionController.L_dir;
        bytesToSend[11] = (byte)motionController.L_dir;

       //Log.d("", "onGenericMotionEvent " +motionController.R_speed);
        bytesToSend[12] = (byte)motionController.R_speed;
        bytesToSend[13] = (byte)motionController.R_speed;
        bytesToSend[14] = (byte)motionController.L_speed;
        bytesToSend[15] = (byte)motionController.L_speed;
       // Log.d("", "onGenericMotionEvent " + bytesToSend[12]);
        return true;
    }

    float[] rotMat = new float[9];
    float[] vals = new float[3];

    double azimuth_offset,pitch_offset;

    double PI = Math.PI;
    double HALF_PI = Math.PI/2;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        SensorManager.getRotationMatrixFromVector(rotMat, sensorEvent.values);
        SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_X, SensorManager.AXIS_Y, rotMat);
        SensorManager.getOrientation(rotMat, vals);

        float azimuth_angle = (float)(vals[0] + PI);
        float pitch_angle = vals[2];
        float roll_angle = vals[1];

        if (zeroMe)
        {
            azimuth_offset = azimuth_angle;
            pitch_offset = pitch_angle;
            zeroMe = false;
        }

        double azimuth_scaled = azimuth_angle - azimuth_offset;

        azimuth_scaled += PI;

        //Wrap around after offset
        if (azimuth_scaled < 0)
            azimuth_scaled += 2*PI;
        else if (azimuth_scaled > 2*PI)
            azimuth_scaled -= 2*PI;



        double pitch_scaled = pitch_angle - pitch_offset;

        //if (azimuth_scaled > PI)
         //   azimuth_scaled -= 2 * PI;


        //Scaled is now +-PI for 360
        //Only want 180 really

        if (azimuth_scaled<HALF_PI)
            azimuth_scaled = HALF_PI;

        if (azimuth_scaled>PI + HALF_PI)
            azimuth_scaled = PI + HALF_PI;



        if (pitch_scaled > PI)
            pitch_scaled -= 2 * PI;

        if (pitch_scaled<-HALF_PI)
            pitch_scaled = -HALF_PI;

        if (pitch_scaled>HALF_PI)
            pitch_scaled = HALF_PI;

       Log.d("", "sensor :" + azimuth_scaled + " " + (vals[0] + PI));
        if (client != null)
        {


            bytesToSend[4] = (byte)(255 - ((byte)((azimuth_scaled-HALF_PI)/PI * 255)));
            bytesToSend[5] = (byte)((pitch_scaled+HALF_PI)/PI * 255);
            Log.d("", "value = " + bytesToSend[4]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
