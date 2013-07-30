package ac.snu.cares.contextlogger.collector;

import ac.snu.cares.contextlogger.database.StateCollection;
import ac.snu.cares.contextlogger.R;

import android.app.Service;
import android.content.Context;
import android.os.PowerManager;
import android.widget.Toast;

public abstract class Collector {
	public boolean enabled;
	protected Service context;
	protected StateCollection collection;
	private boolean isSilentMode;

	abstract public void enable(Service context);
    abstract public String getDBName();

	abstract protected void handle();

	protected void awakeHandle() {
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		wl.acquire();

		handle();

		wl.release();
		wl = null;
	}

	protected void showToastMessage(String msg) {
		if (isSilentMode) {
			return;
		}
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	protected void enableBase(Service context, String dbName) {
		this.enabled = true;
		this.context = context;
		this.isSilentMode = context.getString(R.string.silent_mode).equals(
				"true");
		if (this.collection == null) {
			this.collection = new StateCollection(context, dbName);
		}

		this.collection.open();
		this.collection.put(StateCollection.START_MSG);
	}

	public void disable() {
		this.enabled = false;
		if (this.collection != null) {
			this.collection.put(StateCollection.STOP_MSG);
			// this.collection.close(); // DB file should be always opened for other states' tables.
			this.collection = null;
		}
	}

	public long size() {
		if (collection != null) {
			return collection.size();
		} else {
			return -1;
		}
	}

	public String getLatest() {
		if (collection != null) {
			return collection.getLatest();
		} else {
			return null;
		}
	}
}
