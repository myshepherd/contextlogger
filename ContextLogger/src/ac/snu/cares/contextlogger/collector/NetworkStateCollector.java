package ac.snu.cares.contextlogger.collector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStateCollector extends Collector {
    private static final String DB_NAME = "networkstate";
    private static final String TAG = "NetworkStateCollector";

    private String prev;
    private Intent intentHandling = null;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intentHandling = intent;
            awakeHandle(TAG);
        }
    };

    @Override
    protected void handle() {
        if (intentHandling == null) {
            return;
        }

        intentHandling = null;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo == null)
            return;

        String current = null;
        switch (activeNetInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                current = "Wifi";
                break;
            default:
                current = activeNetInfo.getSubtypeName();
                break;
        }

        if (current != null && (prev == null || !prev.equals(current))) {
            showToastMessage("Network State " + current);

            collection.put(current);

            prev = current;
        }
    }

    public void enable(Service context) throws SQLiteException {
        super.enableBase(context, DB_NAME);

        String lastLog = collection.getLatest();
        if (lastLog != null)
            prev = lastLog;
        else
            prev = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        context.registerReceiver(receiver, filter);
    }

    public void disable() {
        super.disable();
        context.unregisterReceiver(receiver);
    }

    public String getDBName() {
        return DB_NAME;
    }
}
