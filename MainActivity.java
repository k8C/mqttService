package project.thesis.vgu.mqtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "mqtt";
    boolean atLeastOreo = Build.VERSION.SDK_INT >= 26;
    boolean notifyInBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);Log.e(TAG, "activity onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
//        fm.beginTransaction().add(R.id.container, new MqttFragment()).commit();
        if (atLeastOreo) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel("service", "Background Service", NotificationManager.IMPORTANCE_LOW));
            notificationManager.createNotificationChannel(new NotificationChannel("mqttTopic", "MQTT Topics", NotificationManager.IMPORTANCE_HIGH));
        }
        notifyInBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifyInBackground", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (notifyInBackground) {
            stopService(new Intent(this, MqttService.class));
        }
    }

    @Override
    protected void onStop() {
        List <Topic> topics = ((MqttFragment)getSupportFragmentManager().getFragments().get(0)).topics;
        Log.e(TAG, "Activity onStop, topics size: " + topics.size());
        if (notifyInBackground) {
            for (Topic topic : topics) {
                if(topic.notify) {
                    if (atLeastOreo) {
                        startForegroundService(new Intent(this, MqttService.class));
                    } else {
                        startService(new Intent(this, MqttService.class));
                    }
                    break;
                }
            }
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).setChecked(notifyInBackground);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.service_setting) {
            notifyInBackground = !item.isChecked();
            item.setChecked(notifyInBackground);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("notifyInBackground", notifyInBackground).apply();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override // Activity Recreated after configuration changed
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "activity onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }
}
