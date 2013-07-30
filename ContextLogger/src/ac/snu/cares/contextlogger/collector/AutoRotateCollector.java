package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.EnableState;

import android.app.Service;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class AutoRotateCollector extends Collector {
	private static final String AUTO_ROTATE_CONTENT_URI = "content://settings/system/accelerometer_rotation";

	private static final int AUTO_ROTATE_DISABLED = 0;
	private static final int AUTO_ROTATE_ENABLED = 1;

	private static final String DB_NAME = "auto_rotate";

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
			showToastMessage("AutoRotate " + current.name());

			collection.put(current.name());
			prev = current;
		}
	}

	private EnableState getCurrentState() {
		EnableState ret = null;
		int rotate_state = Settings.System.getInt(context.getContentResolver(),
				Settings.System.ACCELEROMETER_ROTATION, -1);

		switch (rotate_state) {
		case AUTO_ROTATE_DISABLED:
			ret = EnableState.DISABLED;
			break;
		case AUTO_ROTATE_ENABLED:
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
				Uri.parse(AUTO_ROTATE_CONTENT_URI), true, observer);
	}

	public void disable() {
		super.disable();

		context.getContentResolver().unregisterContentObserver(observer);
	}

    public String getDBName() {
        return DB_NAME;
    }
}