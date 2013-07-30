package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.ConnState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class DockCollector extends Collector {
	private static final String DB_NAME = "dock";

	private ConnState prev = null;
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

		int dockState = intentHandling.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);
		intentHandling = null;

		ConnState current = null;
		switch (dockState) {
		case Intent.EXTRA_DOCK_STATE_UNDOCKED:
			current = ConnState.DISCONNECTED;
			break;
		case Intent.EXTRA_DOCK_STATE_DESK:
		case Intent.EXTRA_DOCK_STATE_CAR:
		case Intent.EXTRA_DOCK_STATE_LE_DESK:
		case Intent.EXTRA_DOCK_STATE_HE_DESK:
			current = ConnState.CONNECTED;
			break;
		default:
		}

		if (current != null && current != prev) {
			showToastMessage("Dock " + current.name());

			collection.put(current.name());
			prev = current;
		}
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		String lastLog = collection.getLatest();
		if (lastLog != null)
			prev = ConnState.valueOf(lastLog);
		else
			prev = null;

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_DOCK_EVENT);

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
