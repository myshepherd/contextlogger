package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class SyncCollector extends Collector {
	private static final String SYNC_CONNECTION_SETTING_CHANGED = "com.android.sync.SYNC_CONN_STATUS_CHANGED";

	private static final String DB_NAME = "sync";
    private static final String TAG = "SyncCollector";

	private EnableState prev;

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			awakeHandle(TAG);
		}
	};

	@Override
	protected void handle() {
		boolean sync = ContentResolver.getMasterSyncAutomatically();
		EnableState current = (sync) ? EnableState.ENABLED : EnableState.DISABLED;
		if (current != prev) {
			showToastMessage("Automatic Sync " + current.name());

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
		filter.addAction(SYNC_CONNECTION_SETTING_CHANGED);

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