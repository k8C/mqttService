package project.thesis.vgu.mqtt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import org.eclipse.paho.android.service.MqttAndroidClient;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MqttFragment extends Fragment {
    String x = "";
    //int number = 0;
    private static final String TAG = "mqtt";
    Intent service;
    PowerManager.WakeLock wakeLock;
    ConnectivityManager.NetworkCallback connectionCallback;
    List<Topic> topics;
    ListView listView;
    TopicAdapter topicAdapter;
    AlertDialog publishDialog;
    AlertDialog settingDialog;

    public MqttFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //retain variables across configchanges, onCreate and onDestroy not called
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

    TextView tv;
    //private MqttAndroidClient client;
    public MqttClient client;
    public MqttConnectOptions option;
    public IMqttToken connectToken, subscribeToken;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        if (savedInstanceState != null) Log.e(TAG, "onActivityCreated savedInstanceState != null");
        tv = getActivity().findViewById(R.id.tv);

        /*SharedPreferences appData = PreferenceManager.getDefaultSharedPreferences(getContext());
        String topicsJson = appData.getString("topics", null);
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
        }*/
        topics = new ArrayList<Topic>();
        boolean temp = false;
        for (int i = 0; i < 15; i++) {
            Topic tp = new Topic();
            tp.name = "" + i;
            temp = !temp;
            tp.notify = temp;
            tp.max = (float) i;
            topics.add(tp);
        }
        listView = getActivity().findViewById(R.id.list);
        topicAdapter = new TopicAdapter();
        listView.setAdapter(topicAdapter);

        registerForContextMenu(listView);

        service = new Intent(getContext(), MqttService.class);
//        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mqtt::k8c");
//        wakeLock.acquire();


//        if (MainActivity.atLeastOreo) {
//            getContext().startForegroundService(service);
//            Log.e(TAG, ">= Oreo");
//        } else {
//            getContext().startService(service);
//            Log.e(TAG, "< Oreo");
//        }

        /*publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    client.publish(publishTopic.getText().toString(), new MqttMessage("0".getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        subscribe.setOnClickListener(new View.OnClickListener() {
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
    public void onStart() {
        super.onStart();
        /*SharedPreferences appData = PreferenceManager.getDefaultSharedPreferences(getContext());
        String tp = appData.getString("topics", null);
        Topic[] topics = new Gson().fromJson(tp, Topic[].class); //new TypeToken<List<Topic>>() {}.getType()
        Topic[] topics = {cond, ph, temp};
        //List<Topic> t = Arrays.asList(topics);
        SharedPreferences.Editor storageManager = appData.edit();
        storageManager.putString("topics", new Gson().toJson(topics)).commit();*/
        ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), connectionCallback);
    }

    @Override
    public void onStop() {
        Log.e(TAG, "fragment onStop");
        super.onStop();
        // disconnectForcibly
        ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(connectionCallback);
//        PreferenceManager.getDefaultSharedPreferences(getContext())
//                .edit().putString("topics", new Gson().toJson(topics)).apply();
        //getActivity().findViewById(R.menu.)
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "fragment onDestroy");
//        getContext().stopService(service);
//        wakeLock.release();
        super.onDestroy();
    }

    class TopicAdapter extends BaseAdapter {
        int number;
        EditText max, min;

        @Override
        public int getCount() {
            return topics.size();
        }

        @Override
        public Object getItem(int position) {
            return topics.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.e(TAG, "position: " + position);
            ViewHolder viewHolder;
            if (convertView != null) {
                Log.e(TAG, "convertView != null");
                viewHolder = (ViewHolder) convertView.getTag();
            } else {
                Log.e(TAG, "convertView is null");
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
                                            Toast.makeText(getContext(), "Publish position: " + number, Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .show();
                        }
                    }
                });
                viewHolder.toggleBell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View toggleButton) {
                        Toast.makeText(getContext(), "isChecked: " + ((ToggleButton) toggleButton).isChecked(), Toast.LENGTH_SHORT).show();
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
        Log.e(TAG, "abcdefgfjfkfkgkjg");
        getActivity().getMenuInflater().inflate(R.menu.listview_contextmenu, menu);
        menu.setHeaderTitle(topics.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position).name);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }
}
/*connectionReceiver = new BroadcastReceiver() {@Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo ni = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {}}};
          registerReceiver(connectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));*/