package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.net.wifi.WifiManager;

public class WiFiCollector extends Collector {
	private static final String DB_NAME = "wifi";

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

		int wifiState = intentHandling.getIntExtra(
				WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
		intentHandling = null;

		EnableState current = null;
		switch (wifiState) {
		case WifiManager.WIFI_STATE_ENABLED:
			current = EnableState.ENABLED;
			break;
		case WifiManager.WIFI_STATE_DISABLED:
			current = EnableState.DISABLED;
			break;
		case WifiManager.WIFI_STATE_ENABLING:
		case WifiManager.WIFI_STATE_DISABLING:
		case WifiManager.WIFI_STATE_UNKNOWN:
		default:
		}

		if (current != null && prev != current) {
			showToastMessage("Wifi " + current.name());

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

		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

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