package project.thesis.vgu.mqtt;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.reflect.Type;
import java.util.List;

public class MqttService extends Service {
    private static final String TAG = "mqtt";
    Topic[] topics;

    public MqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public MqttAsyncClient client;
    public MqttConnectOptions option;
    public IMqttToken connectToken, subscribeToken;

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        startForeground(1, new NotificationCompat.Builder(this, "service")
                .addAction(0, "STOP", PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class), 0))
                .setPriority(NotificationCompat.PRIORITY_LOW).setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("MQTT service is running").build());
        String topicsJson = PreferenceManager.getDefaultSharedPreferences(this).getString("topics", null);
        if (topicsJson == null) {
            stopSelf();
            Log.e(TAG, "no data to process, service terminated");
            return START_NOT_STICKY;
        }
        List<Topic> topicList = new Gson().fromJson(topicsJson, new TypeToken<List<Topic>>() {
        }.getType());
        for (int i = 0; i < topicList.size(); i++) {
            if (topicList.get(i).notify == false) {
                topicList.remove(i);
                i--;
            }
        }
        topics = (Topic[]) topicList.toArray();

        try {
            Log.e(TAG, "123");
            client = new MqttAsyncClient("tcp://io.adafruit.com:1883", "k8c53795cakn", null);
            Log.e(TAG, "456");
        } catch (MqttException e) {
            Log.e(TAG, "constructor exception: " + e.getMessage());
            e.printStackTrace();
        }
        option = new MqttConnectOptions();
        option.setAutomaticReconnect(true);
        option.setCleanSession(false);
        option.setUserName("monokia");
        option.setPassword("b19057d0daee4a4db05b4c0c1ed9166d".toCharArray());
        client.setCallback(new MqttCallbackExtended() {
            boolean notify = false;
            float value;
            String notificationText;
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.e(TAG, "connectComplete");
                try {
                    for (int i = 0; i < topics.length; i++) {
                        client.subscribe(topics[i].name, 1);
                    }
                    /*client.subscribe("monokia/f/cond", 1, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.e(TAG, "subscribe success");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.e(TAG, "subscribe fail");
                        }
                    });*/
                } catch (MqttException e) {
                    Log.e(TAG, "subscribe mqttException: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.e(TAG, topic + ": " + message);
                for (int i = 0; i < topics.length; i++) {
                    if (topics[i].name == topic) {
                        if (topics[i].max != null) {
                            value = Float.parseFloat(message.toString());
                            if (topics[i].max < value) {
                                notify = true;
                                notificationText = "Value is " + value + " > " + topics[i].max;
                            }
                        } else if (topics[i].min != null) {
                            value = Float.parseFloat(message.toString());
                            if (topics[i].min > value) {
                                notify = true;
                                notificationText = "Value is " + value + " < " + topics[i].min;
                            }
                        }
                        if (notify) {
                            NotificationManagerCompat.from(MqttService.this).notify(i+2, new NotificationCompat.Builder(MqttService.this, "mqttTopic")
                                    .setContentIntent(PendingIntent.getActivity(MqttService.this, 0, new Intent(MqttService.this, MainActivity.class), 0))
                                    .setPriority(NotificationCompat.PRIORITY_MAX).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                    .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("WARNING Topic " + topic)
                                    .setContentText(notificationText).setAutoCancel(true).build());
                            notify = false;
                        }
                        break;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.e(TAG, "deliveryComplete");
            }
        });
        try {
            client.connect(option, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e(TAG, "connect success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "connect fail");
                    try {
                        client.reconnect();
                    } catch (MqttException e) {
                        Log.e(TAG, "reconnect exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "connect exception: " + e.getMessage());
            e.printStackTrace();
        }
        Log.e(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "service onCreate");
//        List<Topic> topics = null;
//        SharedPreferences appData = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor storageManager = appData.edit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "service onDestroy");
        try {
            client.disconnectForcibly();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
