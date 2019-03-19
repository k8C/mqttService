package project.thesis.vgu.mqtt;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MqttService extends Service {
    private static final String TAG = "mqtt";
    Topic[] topics;
    PowerManager.WakeLock wakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    MqttAsyncClient client;

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        String topicsJson = PreferenceManager.getDefaultSharedPreferences(this).getString("topics", null);
        if (topicsJson == null) {
            stopSelf();
            Log.e(TAG, "no data to process, service terminated");
            return START_NOT_STICKY;
        }
        startForeground(1, new NotificationCompat.Builder(this, "service")
                .addAction(0, "STOP", PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class), 0))
                .setPriority(NotificationCompat.PRIORITY_LOW).setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("MQTT service is running").build());
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqtt::k8c");
        wakeLock.acquire();
        List<Topic> topicList = new Gson().fromJson(topicsJson, new TypeToken<List<Topic>>() {
        }.getType());
        for (int i = 0; i < topicList.size(); i++) {
            if (!topicList.get(i).notify) {
                topicList.remove(i);
                i--;
            }
        }
        topics = topicList.toArray(new Topic[topicList.size()]);

        try {
            client = new MqttAsyncClient("tcp://io.adafruit.com:1883", "k8c53795cakn", null);
        } catch (MqttException e) {
            Log.e(TAG, "constructor exception: " + e.getMessage());
            e.printStackTrace();
        }
        MqttConnectOptions option = new MqttConnectOptions();
        option.setAutomaticReconnect(true);
        option.setCleanSession(false);
        option.setUserName("monokia");
        option.setPassword("b19057d0daee4a4db05b4c0c1ed9166d".toCharArray());
        client.setCallback(new MqttCallbackExtended() {
            boolean notify = false;
            float value;
            String notificationText;
            int i;

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.e(TAG, "connectComplete");
                try {
                    for (Topic topic : topics) {
                        client.subscribe(topic.name, 1, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.e(TAG, "Subscribe Success To " + asyncActionToken.getTopics()[0]);
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.e(TAG, "Subscribe Fail, Retrying");
                                try {
                                    client.subscribe(asyncActionToken.getTopics()[0], 1, null, this);
                                } catch (MqttException e) {
                                    Log.e(TAG, "Exception Why Retrying To Subscribe");
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
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
            public void messageArrived(String topicName, MqttMessage message) throws Exception {
                Log.e(TAG, topicName + ": " + message);
                i = 2;
                for (Topic topic : topics) {
                    if (topic.name.equals(topicName)) {
                        if (topic.max != null) {
                            value = Float.parseFloat(message.toString());
                            if (topic.max < value) {
                                notify = true;
                                notificationText = "Value is " + value + " > " + topic.max;
                            }
                        } else if (topic.min != null) {
                            value = Float.parseFloat(message.toString());
                            if (topic.min > value) {
                                notify = true;
                                notificationText = "Value is " + value + " < " + topic.min;
                            }
                        }
                        if (notify) {
                            NotificationManagerCompat.from(MqttService.this).notify(i, new NotificationCompat.Builder(MqttService.this, "mqttTopic")
                                    .setContentIntent(PendingIntent.getActivity(MqttService.this, 0, new Intent(MqttService.this, MainActivity.class), 0))
                                    .setPriority(NotificationCompat.PRIORITY_MAX).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                    .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("WARNING Topic " + topicName)
                                    .setContentText(notificationText).setAutoCancel(true).build());
                            notify = false;
                        }
                        break;
                    }
                    i++;
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
                    Log.e(TAG, "connect fail, retrying");
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
            client.disconnect(0);
        } catch (MqttException e) {
            Log.e(TAG, "disconnectForcibly exception "+ e.getMessage());
            e.printStackTrace();
        }
        wakeLock.release();
    }
}
