package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class HapticCollector extends Collector {
	private static final String HAPTIC_CONTENT_URI = "content://settings/system/haptic_feedback_enabled";

	private static final int HAPTIC_DISABLED = 0;
	private static final int HAPTIC_ENABLED = 1;

	private static final String DB_NAME = "haptic";

	private EnableState prev = null;

	private ContentObserver observer = new ContentObserver(new Handler()) {
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			awakeHandle();
		}
	};

	@Override
	protected void handle() {
		EnableState current = getCurrentState();

		if (current != null && prev != current) {
			showToastMessage("Haptic " + current);

			collection.put(current.name());
			prev = current;
		}
	}

	private EnableState getCurrentState() {
		EnableState ret = null;
		int haptic_state = Settings.System.getInt(context.getContentResolver(),
				Settings.System.HAPTIC_FEEDBACK_ENABLED, -1);
		switch (haptic_state) {
		case HAPTIC_DISABLED:
			ret = EnableState.DISABLED;
			break;
		case HAPTIC_ENABLED:
			ret = EnableState.ENABLED;
			break;
		default:
		}
		return ret;
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		prev = getCurrentState();

		context.getContentResolver().registerContentObserver(
				Uri.parse(HAPTIC_CONTENT_URI), true, observer);
	}

	public void disable() {
		super.disable();

		context.getContentResolver().unregisterContentObserver(observer);
	}

    public String getDBName() {
        return DB_NAME;
    }
};
