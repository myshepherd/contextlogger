package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothCollector extends Collector {
	private static final String DB_NAME = "bluetooth";
    private static final String TAG = "BluetoothCollector";

	private EnableState prev = null;
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

		int blueState = intentHandling.getIntExtra(
				BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		intentHandling = null;

		EnableState current = null;
		switch (blueState) {
		case BluetoothAdapter.STATE_ON:
			current = EnableState.ENABLED;
			break;
		case BluetoothAdapter.STATE_OFF:
			current = EnableState.DISABLED;
			break;
		case BluetoothAdapter.STATE_TURNING_ON:
		case BluetoothAdapter.STATE_TURNING_OFF:
		default:

		}

		if (current != null && current != prev) {
			showToastMessage("Bluetooth " + current.name());

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
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

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
