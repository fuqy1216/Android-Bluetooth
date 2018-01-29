package com.example.qifu.bluetooth;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.bluetooth.BluetoothManager;
        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.Handler;
        import java.lang.reflect.Method;
        import android.widget.Toast;
        import android.util.Log;
        import android.view.View;
        import android.content.Intent;
        import android.view.Menu;
        import android.app.NotificationManager;
        import android.widget.TextView;
        import android.widget.EditText;
        import android.widget.Button;
        import android.widget.CompoundButton;
        import android.widget.Switch;
        import android.app.Notification;
        import android.support.v4.app.NotificationCompat;
        import java.io.BufferedWriter;
        import java.io.File;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Locale;
        import java.util.Set;
        import java.util.UUID;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocketWrapper bluetoothSocket;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    boolean counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openButton = (Button) findViewById(R.id.open);
        Button sendButton = (Button) findViewById(R.id.send);
        Button closeButton = (Button) findViewById(R.id.close);
        Switch recordswitch = (Switch) findViewById(R.id.record);
        myLabel = (TextView) findViewById(R.id.label);
        myTextbox = (EditText) findViewById(R.id.entry);
        findBT();

        recordswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    counter = true;
                } else {
                    counter = false;
                    // The toggle is disabled
                }
            }
        });
        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //startService(new Intent(MainActivity.this,MyService.class));
                //Intent startMain = new Intent(Intent.ACTION_MAIN);
/*                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);*/
            boolean success = false;
            int iteration = 0;
            while (success == false)
            try {
                    findBT();
                    openBT();
                    success = true;
                } catch (IOException ex) {
                success = false;
                iteration = iteration + 1;
                if (iteration == 5)
                { myLabel.setText("BlueTooth Device Found, Connection Failed");
                    break;}
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                }
                }

            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startService(new Intent(MainActivity.this,MyService.class));
               /* try {
                    sendData();
                } catch (IOException ex) {
                }*/
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this,MyService.class));
                try {
                   closeBT();
                } catch (IOException ex) {

                }
            }
        });
    }

    void findBT() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("No bluetooth adapter available");
        }
        if (mBluetoothAdapter == null ||!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        String devicename = new String();
        ;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // myLabel.setText(device.getName());
                if (true/*device.getName().equals("SmartWatch 3 731C")*/) {
                    mmDevice = device;
                    devicename = device.getName();
                    break;
                }
            }
        }
        String found = "Bluetooth Device Found on Record: " + devicename;
        myLabel.setText(found);
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        // ParcelUuid[] uuid1 = mmDevice.getUuids();
        //UUID uuid1 = UUID.fromString(uuid);
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
        } catch (IOException e) {
            throw e;
            //try the fallback
            /*try {
                bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
                Thread.sleep(500);
                bluetoothSocket.connect();
                //success = true;
                mmOutputStream = bluetoothSocket.getOutputStream();
                mmInputStream = bluetoothSocket.getInputStream();

                beginListenForData();

                myLabel.setText("Bluetooth Opened");
                return;
            } catch (FallbackException e1) {
                Log.w("BT", "Could not initialize FallbackBluetoothSocket classes.", e);
            } catch (InterruptedException e1) {
                Log.w("BT", e1.getMessage(), e1);
            } catch (IOException e1) {
                Log.w("BT", "Fallback failed. Cancelling.", e1);
            }*/
        }


        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {

                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes);
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            myLabel.setText(data);
                                    if(counter) {
                                        //final int total_row = data.length();
                                        final int total_row = 2;
                                        Log.i("BlueToothAlbert", "total_row = " + total_row);
                                        final String fileprefix = "export";
                                        final String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                                        final String exacttimer = new SimpleDateFormat("yyyyMMdd-HHmmssSSS", Locale.getDefault()).format(new Date());
                                        final String filename = String.format("%s_%s.txt", fileprefix, date);

                                        // final String directory = getContext().getApplicationContext().getFilesDir().getAbsolutePath();// + "/Albert";
                                        final String directory = Environment.getExternalStorageDirectory().getAbsolutePath();// + "/sdcard";
                                        final String direc = "/storage/emulated/0";
                                        final File logfile = new File(directory, filename);
                                        final File logPath = logfile.getParentFile();

                                        if (!logPath.isDirectory() && !logPath.mkdirs()) {
                                            Log.e("BlueToothAlbert", "Could not create directory for log files");
                                        }
/*
                int permissionCheck = ContextCompat.checkSelfPermission(getContext().getApplicationContext().getCurrentActivity(),
                        android.Manifest.permission.WRITE_CALENDAR);*/
                                        try {
                                            FileWriter filewriter = new FileWriter(logfile, true);
                                            BufferedWriter bw = new BufferedWriter(filewriter);


                                            // Write the string to the file
                                            for (int i = 1; i < total_row; i++) {
                                                StringBuffer sb = new StringBuffer(exacttimer);
                                                sb.append("\t");
                                                sb.append(String.valueOf(data));
                                                sb.append("\n");
                                                bw.append(sb.toString());
                                            }
                                            bw.flush();
                                            bw.close();
                        /*Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("**");

                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                "SensorloggerAlbert data export");
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logfile));
                        getContext().startActivity(Intent.createChooser(emailIntent, "Send mail..."));

                        Log.i("SensorloggerAlbert", "export finished!");*/
                                        } catch (IOException ioe) {
                                            Log.e("BlueToothAlbert", "IOException while writing Logfile");
                                        }
                                    }
                                            // dataList.clear();
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException {
        stopWorker = true;
        if((mmOutputStream != null)&&(mmInputStream != null)&&(mmSocket != null))
        {mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();}
        myLabel.setText("Bluetooth Closed");
    }

    public static interface BluetoothSocketWrapper {

        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        String getRemoteDeviceName();

        void connect() throws IOException;

        String getRemoteDeviceAddress();

        void close() throws IOException;

        BluetoothSocket getUnderlyingSocket();

    }

    public static class NativeBluetoothSocket implements BluetoothSocketWrapper {

        private BluetoothSocket socket;

        public NativeBluetoothSocket(BluetoothSocket tmp) {
            this.socket = tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public String getRemoteDeviceName() {
            return socket.getRemoteDevice().getName();
        }

        @Override
        public void connect() throws IOException {
            socket.connect();
        }

        @Override
        public String getRemoteDeviceAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        @Override
        public BluetoothSocket getUnderlyingSocket() {
            return socket;
        }

    }

    public class FallbackBluetoothSocket extends NativeBluetoothSocket {

        private BluetoothSocket fallbackSocket;

        public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
            super(tmp);
            try {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            } catch (Exception e) {
                throw new FallbackException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fallbackSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return fallbackSocket.getOutputStream();
        }


        @Override
        public void connect() throws IOException {
            fallbackSocket.connect();
        }


        @Override
        public void close() throws IOException {
            fallbackSocket.close();
        }

    }
    public static class FallbackException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public FallbackException(Exception e) {
            super(e);
        }

    }
}