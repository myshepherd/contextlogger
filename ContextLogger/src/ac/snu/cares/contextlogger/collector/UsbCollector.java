package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.ConnState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class UsbCollector extends Collector {

	private static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
	private static final String USB_CONNECTED = "connected";

	private static final String DB_NAME = "usb";
    private static final String TAG = "UsbCollector";

	private ConnState prev = null;
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

		boolean usbConnected = intentHandling.getExtras().getBoolean(USB_CONNECTED);
		intentHandling = null;

		ConnState cur;
		if (usbConnected) {
			cur = ConnState.CONNECTED;
		} else {
			cur = ConnState.DISCONNECTED;
		}

		if (cur != prev) {
			showToastMessage("USB Host " + cur);

			collection.put(cur.name());
			prev = cur;
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
		filter.addAction(ACTION_USB_STATE);

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