package com.emtronics.quadbotdroid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements H264CameraServer.ReceiveData {

    static String LOG = "MainActivity";

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    StringBuilder logBuilder = new StringBuilder();
    TextView logTextView;

    private UsbSerialPort port;


    MainActivity this_;

    H264CameraServer cameraServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this_ = this;


        final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);


        logTextView = (TextView)findViewById(R.id.log_textView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());



        Button usbConnect = (Button)findViewById(R.id.usb_connect_button);
        usbConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                if (availableDrivers.isEmpty()) {
                    log("No USB devices");
                    return;
                }
                //Get the first one..probably what we want..
                UsbSerialDriver driver = availableDrivers.get(0);
                manager.requestPermission(driver.getDevice(), mPermissionIntent);
            }
        });


        Button startServer = (Button)findViewById(R.id.start_server_button);
        startServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (cameraServer != null)
                {
                    cameraServer.shutDown();
                }

                cameraServer = new H264CameraServer(this_,this_,false,GD.width,GD.height);
                cameraServer.start();


                log("Started camera server");
                WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                int ip = wifiInfo.getIpAddress();
                String ipAddress = Formatter.formatIpAddress(ip);
                log("IP: " + ipAddress);
            }
        });

        Button showViewer = (Button)findViewById(R.id.viewer_button);
        showViewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(this_, ViewerActivity.class);
                this_.startActivity(myIntent);
            }
        });
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            log("USB Device NOT NULL");

                            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                            if (availableDrivers.isEmpty()) {
                                log("No USB devices");
                                return;
                            }

// Open a connection to the first available driver.
                            UsbSerialDriver driver = availableDrivers.get(0);

                            // manager.requestPermission(driver.getDevice(), mPermissionIntent);
                            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                            if (connection == null) {
                                log("USB connection null");
                                // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
                                return;
                            }
                            Log.d("TEST", "VID = " + driver.getDevice().getVendorId());
                            Log.d("TEST","PID = "+ driver.getDevice().getProductId());
                            Log.d("TEST","PID = "+ driver.getDevice().getDeviceName());

// Read some data! Most have just one port (port 0).
                            log("Number of ports = " + driver.getPorts().size() );

                            port = driver.getPorts().get(0);

                            try {
                                port.open(connection);
                                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        log("Permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void log(String message)
    {
        logBuilder.append(message + "\n");
        logTextView.setText(logBuilder);

        final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            logTextView.scrollTo(0, scrollAmount);
        else
            logTextView.scrollTo(0, 0);
    }


    @Override
    public void serverReceiveRata(byte[] data) {
        if (port !=null) {
            try {
                if (data.length == 8 + 4 + 4) {
                    byte buffer[] = new byte[13 * 3];
                    //First 8 are the servos
                    buffer[0] = 0x1E;
                    buffer[1] = 0x1; //command
                    buffer[5] = data[0];
                    buffer[6] = data[1];
                    buffer[7] = data[2];
                    buffer[8] = data[3];
                    buffer[9] = data[4];
                    buffer[10] = data[5];
                    buffer[11] = data[6];
                    buffer[12] = data[7];


                    buffer[13 + 0] = 0x1E;
                    buffer[13 + 1] = 2;//Motor direction command
                    buffer[13 + 5] = data[8];
                    buffer[13 + 6] = data[9];
                    buffer[13 + 7] = data[10];
                    buffer[13 + 8] = data[11];


                    buffer[26 + 0] = 0x1E;
                    buffer[26 + 1] = 0;//Motor speed
                    buffer[26 + 5] = data[12];
                    buffer[26 + 6] = data[13];
                    buffer[26 + 7] = data[14];
                    buffer[26 + 8] = data[15];

                    port.write(buffer, 1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
       // System.exit(0);
    }


}
