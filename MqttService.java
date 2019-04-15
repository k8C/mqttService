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
    Topic[] topics;
    PowerManager.WakeLock wakeLock;
    MqttAsyncClient client;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String topicsJson = PreferenceManager.getDefaultSharedPreferences(this).getString("topics", null);
        startForeground(1, new NotificationCompat.Builder(this, "service")
                .addAction(0, "STOP", PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class), 0))
                .setPriority(NotificationCompat.PRIORITY_LOW).setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.app2).setContentTitle("MQTT service is running").build());
        if (topicsJson == null) {
            stopSelf();
            Log.e(MainActivity.TAG, "no data to process, service terminated");
            return START_NOT_STICKY;
        }
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqtt::k8c");
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
            Log.e(MainActivity.TAG, "constructor exception: " + e.getMessage());
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
                //Log.e(MainActivity.TAG, "connectComplete");
                try {
                    for (Topic topic : topics) {
                        client.subscribe(topic.name, 1, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.e(MainActivity.TAG, "Subscribe Success To " + asyncActionToken.getTopics()[0]);
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.e(MainActivity.TAG, "Subscribe Fail, Retrying");
                                try {
                                    client.subscribe(asyncActionToken.getTopics()[0], 1, null, this);
                                } catch (MqttException e) {
                                    Log.e(MainActivity.TAG, "Exception Why Retrying To Subscribe");
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (MqttException e) {
                    Log.e(MainActivity.TAG, "subscribe mqttException: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Log.e(MainActivity.TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String topicName, MqttMessage message) throws Exception {
                Log.e(MainActivity.TAG, topicName + ": " + message);
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
                                    .setSmallIcon(R.drawable.app2).setContentTitle("WARNING Topic ".concat(topicName))
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
            }
        });
        try {
            client.connect(option, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Log.e(MainActivity.TAG, "connect success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Log.e(MainActivity.TAG, "connect fail, retrying");
                    try {
                        client.reconnect();
                    } catch (MqttException e) {
                        Log.e(MainActivity.TAG, "reconnect exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (MqttException e) {
            Log.e(MainActivity.TAG, "connect exception: " + e.getMessage());
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(MainActivity.TAG, "service onDestroy");
        try {
            //client.disconnect(0, null, null);
            client.disconnectForcibly(0, 0, false);
        } catch (MqttException e) {
            Log.e(MainActivity.TAG, "disconnectForcibly exception " + e.getMessage());
            e.printStackTrace();
        }
        try {
            client.close(true);
        } catch (MqttException e) {
            Log.e(MainActivity.TAG, "close exception");
            e.printStackTrace();
        }
        wakeLock.release();
    }
}
