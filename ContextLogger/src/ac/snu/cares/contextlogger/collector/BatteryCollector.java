package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.ConnState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryCollector extends Collector {
	private enum ChargerType {AC, USB, Unkown};
	
	private static final String DB_NAME = "battery";
    private static final String TAG = "BatteryCollector";
	
	private ConnState prev = null;
	private ChargerType type = null;
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
		
		String action = intentHandling.getAction();
		
		if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
			int plug = intentHandling.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
			switch (plug) {
			case BatteryManager.BATTERY_PLUGGED_AC:
				type = ChargerType.AC;
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				type = ChargerType.USB;
				break;
			default:
				type = ChargerType.Unkown;
			}
			intentHandling = null;
			return;
		}

		ConnState current = null;
		String msg = "";
		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			if (type == null) {
				type = ChargerType.Unkown;
			}
			msg += type.name() + " ";
			
			current = ConnState.CONNECTED;
		} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {			
			current = ConnState.DISCONNECTED;
		}
		intentHandling = null;
		
		msg += "POWER " + current.name();
		showToastMessage(msg);

        if (current != null && prev != current) {
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
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(Intent.ACTION_POWER_CONNECTED);
		filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

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