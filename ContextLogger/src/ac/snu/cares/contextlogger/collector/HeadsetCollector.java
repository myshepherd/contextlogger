package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.ConnState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class HeadsetCollector extends Collector {

	private static final String DB_NAME = "headset";

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

		int headSetState = intentHandling.getIntExtra("state", 0);
		intentHandling = null;

		ConnState current = null;
		switch (headSetState) {
		case 0:
			current = ConnState.DISCONNECTED;
			break;
		case 1:
			current = ConnState.CONNECTED;
			break;
		}

		if (current != null && current != prev) {
			showToastMessage("HeadSet " + current.name());

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
		filter.addAction(Intent.ACTION_HEADSET_PLUG);

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
