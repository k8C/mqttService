package project.thesis.vgu.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttService extends Service {
    private static final String TAG = "mqtt";

    public MqttService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    public MqttClient client;
    public MqttConnectOptions option;
    public IMqttToken connectToken, subscribeToken;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.e(TAG, "123");
            client = new MqttClient("tcp://io.adafruit.com:1883", "k8c53795cakn", null);
            Log.e(TAG, "456");
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        option = new MqttConnectOptions();
        option.setAutomaticReconnect(true);
        option.setCleanSession(false);
        option.setUserName("monokia");
        option.setPassword("b19057d0daee4a4db05b4c0c1ed9166d".toCharArray());
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.e(TAG, "connectComplete");
                //tv.setText("connectComplete");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "connectionLost");
                //tv.setText("connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.e(TAG, topic + ": " + message);
                //tv.setText(topic + ": " + message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.e(TAG, "deliveryComplete");
                //tv.setText("deliveryComplete");
            }
        });
        try {
            connectToken = client.connectWithResult(option);
            connectToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e(TAG, "connect success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "connect fail");
                    //tv.setText("connect fail");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        try {
            subscribeToken = client.subscribeWithResponse("monokia/f/cond", 1);
            subscribeToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e(TAG, "subscribe success");
                    //tv.setText("subscribe success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "subscribe fail");
                    //tv.setText("subscribe fail");
                }
            });
            client.subscribe("monokia/f/ph");
            client.subscribe("monokia/f/temp");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return START_REDELIVER_INTENT;
    }
}
