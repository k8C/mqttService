package project.thesis.vgu.mqtt;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;

import java.io.UnsupportedEncodingException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    String x = "";
    private static final String TAG = "mqtt";
    Intent service;
    PowerManager.WakeLock wakeLock;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    TextView tv;
    //private MqttAndroidClient client;
    public MqttClient client;
    public MqttConnectOptions option;
    public IMqttToken connectToken, subscribeToken;

    private EditText publishTopic, subscribeTopic;
    private Button publish, subscribe;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tv = (TextView) getActivity().findViewById(R.id.tv);
        tv.setText("activity created");

        publishTopic = (EditText) getActivity().findViewById(R.id.publishTopic);
        publish = (Button) getActivity().findViewById(R.id.publish);

        subscribe = (Button) getActivity().findViewById(R.id.subscribe);
        subscribeTopic = (EditText) getActivity().findViewById(R.id.subscribeTopic);

        service = new Intent(getContext(), MqttService.class);
        getContext().startService(service);
        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqtt::k8c");
        wakeLock.acquire();


        /*publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    client.publish(publishTopic.getText().toString(), new MqttMessage("0".getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });*/

        /*subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    subscribeToken = client.subscribeWithResponse(subscribeTopic.getText().toString(), 1);
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
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });*/
    }

    @Override
    public void onDestroy() {
        getContext().stopService(service);
        wakeLock.release();
        super.onDestroy();
    }

    //PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//    WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//            "mqtt::k8c");
//    wakeLock.acquire();
    //    @Override
//    public void onStart() {
//        super.onStart();
//        Toast.makeText(getContext(), "shit", Toast.LENGTH_SHORT).show();
//    }


//                new MqttAndroidClient(getContext(), "tcp://io.adafruit.com:1883",
//        options.setUserName("monokia");
//        options.setPassword("b19057d0daee4a4db05b4c0c1ed9166d".toCharArray());
//                    Log.e(TAG, "connect success");
//                    x += "connect success";
//                    subscribe(client, "monokia/f/ph");
//                    subscribe(client, "monokia/f/temp");
//                            Log.e(TAG, "connectionLost");
//                            x += "connectionLost";
//                                tv.setText("ph: " + message.toString());
//                                tv.setText("temp :" + message.toString());
//                    Log.e(TAG, "connect fail");
//                    x += "connect fail";
//            Log.e(TAG, "exception");
//                    Log.e(TAG, "subscribe success");
//                    x+="subscribe success";
//                    Log.e(TAG, "subscribe fail");
//                    x+="subscribe fail";
//                    tv.setText(x);
}
