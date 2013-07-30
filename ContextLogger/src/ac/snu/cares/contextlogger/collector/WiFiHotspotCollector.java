package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class WiFiHotspotCollector extends Collector {
	private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	private static final String EXTRA_WIFI_AP_STATE = "wifi_state";

	private static final int WIFI_AP_STATE_DISABLING_B = 0;
	private static final int WIFI_AP_STATE_DISABLED_B = 1;
	private static final int WIFI_AP_STATE_ENABLING_B = 2;
	private static final int WIFI_AP_STATE_ENABLED_B = 3;
	private static final int WIFI_AP_STATE_FAILED_B = 4;

	private static final int WIFI_AP_STATE_DISABLING = 10;
	private static final int WIFI_AP_STATE_DISABLED = 11;
	private static final int WIFI_AP_STATE_ENABLING = 12;
	private static final int WIFI_AP_STATE_ENABLED = 13;
	private static final int WIFI_AP_STATE_FAILED = 14;

	private static final String DB_NAME = "wifi_hotspot";

	private EnableState prev = null;
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

		int curstate = intentHandling.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_DISABLED);
		intentHandling = null;

		EnableState current = null;
		switch (curstate) {
		case WIFI_AP_STATE_DISABLED:
		case WIFI_AP_STATE_DISABLED_B:
			current = EnableState.DISABLED;
			break;
		case WIFI_AP_STATE_ENABLED:
		case WIFI_AP_STATE_ENABLED_B:
			current = EnableState.ENABLED;
			break;
		case WIFI_AP_STATE_DISABLING:
		case WIFI_AP_STATE_DISABLING_B:
		case WIFI_AP_STATE_ENABLING:
		case WIFI_AP_STATE_ENABLING_B:
		case WIFI_AP_STATE_FAILED:
		case WIFI_AP_STATE_FAILED_B:
		default:
		}

		if (current != null && current != prev) {
			showToastMessage("WiFi Hotspot " + current.name());

			collection.put(current.name());
			prev = current;
		}
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		String lastLog = collection.getLatest();
		if (lastLog != null)
			prev = EnableState.valueOf(lastLog);
		else
			prev = null;

		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);

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