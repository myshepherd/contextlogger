package ac.snu.cares.contextlogger.collector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;

import ac.snu.cares.contextlogger.database.EnableState;

public class ScreenStateCollector extends Collector {
    private static final String DB_NAME = "screenstate";

    private EnableState prev;
    private Intent intentHandling = null;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intentHandling = intent;
            awakeHandle();
        }
    };

    @Override
    protected void handle() {
        if (intentHandling == null) {
            return;
        }

        EnableState current = null;

        if (intentHandling.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            current = EnableState.ENABLED;
        } else if (intentHandling.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            current = EnableState.DISABLED;
        }
        intentHandling = null;

        if (current != null && (prev == null || prev != current)) {
            showToastMessage("Screen State " + current);

            collection.put(current.name());

            prev = current;
        }
    }

    public void enable(Service context) throws SQLiteException {
        super.enableBase(context, DB_NAME);

        String lastLog = collection.getLatest();
        if (lastLog != null)
            prev = EnableState.valueOf(lastLog);
        else
            prev = null;

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

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
