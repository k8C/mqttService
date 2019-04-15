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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "mqtt";
    boolean atLeastOreo = Build.VERSION.SDK_INT >= 26, notifyInBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "activity onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (atLeastOreo) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel("service", "Background Service", NotificationManager.IMPORTANCE_LOW));
            notificationManager.createNotificationChannel(new NotificationChannel("mqttTopic", "MQTT Topics", NotificationManager.IMPORTANCE_HIGH));
        }
        notifyInBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifyInBackground", false);

        ((ViewPager) findViewById(R.id.viewPager)).setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return position == 0 ? new MqttFragment() : new DriveFragment();
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return position == 0 ? "mqtt" : "drive";
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        stopService(new Intent(this, MqttService.class));
    }

    @Override
    protected void onStop() {
        /*List<Topic> topics = ((MqttFragment) getSupportFragmentManager().getFragments().get(0)).topics;
        Log.e(TAG, "Activity onStop, topics size: " + topics.size());
        if (notifyInBackground) {
            for (Topic topic : topics) {
                if (topic.notify) {
                    if (atLeastOreo) {
                        startForegroundService(new Intent(this, MqttService.class));
                    } else {
                        startService(new Intent(this, MqttService.class));
                    }
                    break;
                }
            }
        }*/
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
//https://accounts.google.com/o/oauth2/v2/auth?scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file&response_type=code&redirect_uri=urn:ietf:wg:oauth:2.0:oob&client_id=463875113005-1att4hu76j0ac7ta17mdjniinfgg2di2.apps.googleusercontent.com
//1/0gh8dGi_1bkC-g5BFGlvRRJHKFwUYjQMpW59nv56ZVA

//client_id=463875113005-icovngqrabn2hass5tug5ik5m436ks2k.apps.googleusercontent.com&client_secret=8PWn96NTst2-rbkaXToWoi6F&refresh_token=1/7C-dMwDk771wT5lads8os4_mziPZspcIU6ndw_ZJpi4&grant_type=refresh_token
//4/FwG9vGUJa37B48T-SRWo4_PxxBxJNg_vbWul86O7ICEQHqEgg36SjGYnl84Ls2VQBsw5-hYzbFlMDhtYjKaFlFs
//1/VRNPHjn46h-4vuR8Emw754daGgEx9VmCFWFWronfIO8