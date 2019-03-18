package project.thesis.vgu.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

public class NotificationReceiver extends BroadcastReceiver {
    public NotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, MqttService.class));
    }
}