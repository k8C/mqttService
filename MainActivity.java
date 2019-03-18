package project.thesis.vgu.mqtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "mqtt";
    static final boolean atLeastOreo = Build.VERSION.SDK_INT >= 26;
    boolean notifyInBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);Log.e(TAG, "activity onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
//        fm.beginTransaction().add(R.id.container, new MqttFragment()).commit();
        if (atLeastOreo) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel("service", "Background Service", NotificationManager.IMPORTANCE_LOW));
            notificationManager.createNotificationChannel(new NotificationChannel("mqttTopic", "MQTT Topics", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        notifyInBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notifyInBackground", false);
        menu.getItem(0).setChecked(notifyInBackground);
        if (notifyInBackground) {
            //stopService(new Intent(this, MqttService.class));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.service_setting) {
            notifyInBackground = !item.isChecked();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("notifyInBackground", notifyInBackground).apply();
            item.setChecked(notifyInBackground);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override // Activity Recreated after configuration changed
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "activity onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.e(TAG, "123456789");
        //super.onCreateContextMenu(menu, v, menuInfo);
    }
}
