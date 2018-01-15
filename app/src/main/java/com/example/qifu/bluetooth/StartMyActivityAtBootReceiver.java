package com.example.qifu.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
/**
 * Created by qifu on 1/15/2018.
 */

public class StartMyActivityAtBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            Intent myStarterIntent = new Intent(context, MainActivity.class);
            myStarterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myStarterIntent);

        }
    }

}
