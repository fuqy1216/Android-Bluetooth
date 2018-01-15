package com.example.qifu.bluetooth;

/**
 * Created by qifu on 1/15/2018.
 */
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;

public class MyService extends Service {
    // Binder given to clients
int i;
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        i = 2;
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");

    }

    @Override
    public void onCreate() {
        i=3;
        Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG).show();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        i=4;
        // For time consuming an long tasks you can launch a new thread here...
        Toast.makeText(this, " Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        i=5;
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();

    }
}
