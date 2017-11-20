package com.example.qifu.bluetooth;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.bluetooth.BluetoothManager;
        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.Handler;
        import android.util.Log;
        import android.view.View;
        import android.widget.TextView;
        import android.widget.EditText;
        import android.widget.Button;

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
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });
    }

    void findBT()
    {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        String devicename = new String();;
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
               // myLabel.setText(device.getName());
                if(true/*device.getName().equals("SmartWatch 3 731C")*/)
                {
                    mmDevice = device;
                    devicename = device.getName();
                    break;
                }
            }
        }
        String found= "Bluetooth Device Found: " + devicename;
        myLabel.setText(found);
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes);
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                           myLabel.setText(data);

                                            final int total_row = data.length();
                                            Log.i("BlueToothAlbert", "total_row = " + total_row);
                                            final String fileprefix = "export";
                                            final String date = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
                                            final String filename = String.format("%s_%s.txt", fileprefix);

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
                                                    StringBuffer sb = new StringBuffer(data);
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
                                           // dataList.clear();
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
    }
