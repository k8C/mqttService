package project.thesis.vgu.mqtt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import org.eclipse.paho.android.service.MqttAndroidClient;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

public class MqttFragment extends Fragment {
    private static final String TAG = "mqtt";
    ConnectivityManager.NetworkCallback connectionCallback;
    List<Topic> topics;
    ListView listView;
    TopicAdapter topicAdapter;
    TextView tv;
    MqttAsyncClient client;

    public MqttFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true); //retain variables across configchanges, onCreate and onDestroy not called
        Log.e(TAG, "fragment onCreate");
        connectionCallback = new ConnectivityManager.NetworkCallback() {
            boolean noConnection = false;

            @Override
            public void onLost(Network network) {
                if (!noConnection) {
                    Snackbar.make(listView, "No Connection", Snackbar.LENGTH_INDEFINITE).show();
                    noConnection = true;
                }
            }

            @Override
            public void onAvailable(Network network) {
                if (noConnection) {
                    Snackbar.make(listView, "Connected", Snackbar.LENGTH_SHORT).show();
                    noConnection = false;
                } else {
                    tv.post(new Runnable() {
                        @Override
                        public void run() {
                            tv.setText("io.adafruit.com");
                        }
                    });
                }
                //client.connect;
            }

        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "fragment onCreateView");
        if (savedInstanceState != null) Log.e(TAG, "onCreateView savedInstanceState != null");
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        if (savedInstanceState != null) Log.e(TAG, "onActivityCreated savedInstanceState != null");
        tv = getActivity().findViewById(R.id.tv);

        String topicsJson = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("topics", null);
        if (topicsJson != null) {
            topics = new Gson().fromJson(topicsJson, new TypeToken<List<Topic>>() {
            }.getType());
        } else {
            Topic cond = new Topic();
            cond.name = "monokia/f/cond";
            Topic ph = new Topic();
            ph.name = "monokia/f/ph";
            Topic temp = new Topic();
            temp.name = "monokia/f/temp";
            topics = new ArrayList<Topic>();
            topics.add(cond);
            topics.add(ph);
            topics.add(temp);
        }

        listView = getActivity().findViewById(R.id.list);
        topicAdapter = new TopicAdapter();
        listView.setAdapter(topicAdapter);

        registerForContextMenu(listView);
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
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.e(TAG, "connectComplete");
                try {
                    for (Topic topic : topics) {
                        if (topic.isSubscribed) {
                            client.subscribe(topic.name, 1, null, new IMqttActionListener() {
                                @Override
                                public void onSuccess(IMqttToken asyncActionToken) {
                                    Log.e(TAG, "Subscribed Successfully To " + asyncActionToken.getTopics()[0]);
                                }

                                @Override
                                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                    Log.e(TAG, "Subscribe Failed To " + asyncActionToken.getTopics()[0]);
                                }
                            });
                        }
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
                for (Topic topic : topics) {
                    if (topic.name.equals(topicName)) {
                        topic.value = message.toString();
                        topicAdapter.notifyDataSetChanged();
                        if (topic.notify && ((topic.max != null && topic.max < Float.parseFloat(message.toString()))
                                || (topic.min != null && topic.min > Float.parseFloat(message.toString())))) {
                            NotificationManagerCompat.from(getContext()).notify(1, new NotificationCompat.Builder(getContext(), "mqttTopic")
                                    .setPriority(NotificationCompat.PRIORITY_MAX).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                    .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("WARNING Topic " + topicName).build());
                        }
                        break;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.e(TAG, "deliveryComplete");
                //Log.e(TAG, "Published successfully to " + token.getTopics()[0]);
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
                        e.printStackTrace();
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //List<Topic> t = Arrays.asList(topics);
        ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), connectionCallback);
    }

    @Override
    public void onStop() {
        Log.e(TAG, "fragment onStop");
        super.onStop();
        // disconnectForcibly
        ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(connectionCallback);
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("topics", new Gson().toJson(topics)).apply();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "fragment onDestroy");
        super.onDestroy();
    }

    class TopicAdapter extends BaseAdapter {
        int number;
        AlertDialog publishDialog, settingDialog;
        EditText max, min;

        @Override
        public int getCount() {
            return topics.size();
        }

        @Override
        public Object getItem(int position) {
            Log.e(TAG, "getItem");
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
            } else {
                convertView = getLayoutInflater().inflate(R.layout.topic_row, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.topicTextView = convertView.findViewById(R.id.topic);
                viewHolder.messageTextView = convertView.findViewById(R.id.value);
                viewHolder.publishButton = convertView.findViewById(R.id.arrow);
                viewHolder.toggleBell = convertView.findViewById(R.id.bell);
                viewHolder.settingButton = convertView.findViewById(R.id.gear);
                viewHolder.publishButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View publishButton) {
                        number = (int) publishButton.getTag();
                        if (publishDialog != null) {
                            publishDialog.show();
                        } else {
                            publishDialog = new AlertDialog.Builder(getContext())
                                    .setView(getActivity().getLayoutInflater().inflate(R.layout.publish_dialog, null))
                                    .setPositiveButton("Publish", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                client.publish(topics.get(number).name, ((EditText) publishDialog.findViewById(R.id.publishText)).getText().toString().getBytes(), 1, ((CheckBox) publishDialog.findViewById(R.id.retain)).isChecked(), null, new IMqttActionListener() {
                                                    @Override
                                                    public void onSuccess(IMqttToken asyncActionToken) {
                                                        Toast.makeText(getContext(), "Published successfully", Toast.LENGTH_SHORT).show();
                                                    }

                                                    @Override
                                                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                                        Toast.makeText(getContext(), "Publish failed", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } catch (MqttException e) {
                                                Log.e(TAG, "publish Exception");
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .show();
                        }
                    }
                });
                viewHolder.toggleBell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View toggleButton) {
                        topics.get((int) toggleButton.getTag()).notify = ((ToggleButton) toggleButton).isChecked();
                    }
                });
                viewHolder.settingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View settingButton) {
                        number = (int) settingButton.getTag();
                        if (settingDialog == null) {
                            settingDialog = new AlertDialog.Builder(getContext()).setTitle("Notify when value:")
                                    .setView(getActivity().getLayoutInflater().inflate(R.layout.setting_dialog, null))
                                    .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
                            settingDialog.setCanceledOnTouchOutside(false);
                            settingDialog.create();
                            settingDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Topic topic = topics.get(number);
                                    String maxText = max.getText().toString(), minText = min.getText().toString();
                                    if (!maxText.isEmpty() && minText.isEmpty()) {
                                        topic.max = Float.valueOf(maxText);
                                        topic.min = null;
                                    } else if (maxText.isEmpty() && !minText.isEmpty()) {
                                        topic.min = Float.valueOf(minText);
                                        topic.max = null;
                                    } else {
                                        Toast.makeText(getContext(), "Please check again", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    settingDialog.dismiss();
                                }
                            });
                            max = settingDialog.findViewById(R.id.max);
                            min = settingDialog.findViewById(R.id.min);
                        }
                        Topic topic = topics.get(number);
                        max.setText(topic.max == null ? null : topic.max.toString());
                        min.setText(topic.min == null ? null : topic.min.toString());
                        settingDialog.show();
                    }
                });
                convertView.setTag(viewHolder);
            }
            Topic topic = topics.get(position);
            viewHolder.topicTextView.setText(topic.name);
            viewHolder.messageTextView.setText(topic.value);
            viewHolder.publishButton.setTag(position);
            viewHolder.toggleBell.setChecked(topic.notify);
            viewHolder.toggleBell.setTag(position);
            viewHolder.settingButton.setTag(position);
            return convertView;
        }

        class ViewHolder {
            TextView topicTextView;
            TextView messageTextView;
            ImageButton publishButton;
            ToggleButton toggleBell;
            ImageButton settingButton;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.e(TAG, "onCreateContextMenu");
        getActivity().getMenuInflater().inflate(R.menu.listview_contextmenu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
        switch (item.getItemId()) {
            case R.id.subscribe:
                try {
                    client.subscribe(topics.get(position).name, 1, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Toast.makeText(getContext(), "Subscribed successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Toast.makeText(getContext(), "Subscribe failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.unsubscribe:
                try {
                    client.unsubscribe(topics.get(position).name, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Toast.makeText(getContext(), "Unsubscribed successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Toast.makeText(getContext(), "Unsubscribe failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                break;
        }
        return super.onContextItemSelected(item);
    }
}
/*connectionReceiver = new BroadcastReceiver() {@Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo ni = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {}}};
          registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));*/