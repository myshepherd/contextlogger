package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.SoundState;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

public class RingerCollector extends Collector {

	private static final String DB_NAME = "ringer";

	private SoundState prev = null;
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

		int ringMode = intentHandling.getIntExtra(
				AudioManager.EXTRA_RINGER_MODE, -1); // set default value as -1
		intentHandling = null;

		SoundState current = null;
		switch (ringMode) {
		case AudioManager.RINGER_MODE_NORMAL:
			current = SoundState.NORMAL;
			break;
		case AudioManager.RINGER_MODE_SILENT:
			current = SoundState.SILENT;
			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			current = SoundState.VIBRATE;
			break;
		default:
		}

		if (current != null && current != prev) {
			showToastMessage("Ringer " + current.name() + " Mode");

			collection.put(current.name());
			prev = current;
		}
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		String lastLog = collection.getLatest();
		if (lastLog != null)
			prev = SoundState.valueOf(lastLog);
		else
			prev = null;

		IntentFilter filter = new IntentFilter();
		filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);

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