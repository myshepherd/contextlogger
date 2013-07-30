package ac.snu.cares.contextlogger.collector;

import java.util.Calendar;

import ac.snu.cares.contextlogger.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.widget.Toast;

public class TimeCollector extends Collector {
	private static final String ACTION_TIME_REACHED = "ac.snu.cares.elgger.action.TIME_REACHED";

	private static final String DB_NAME = "time";

	private PendingIntent pIntent;
	private long trigger;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			awakeHandle();
		}
	};

	@Override
	protected void handle() {
		String reachedTime = (String) DateFormat.format(
				context.getString(R.string.date_format), trigger);

		collection.put(reachedTime);
		Toast.makeText(context, "eLGger: " + reachedTime, Toast.LENGTH_SHORT)
				.show();
	}

	public void setTime(long millis) {
		trigger = millis;
	}

	public void setTime(Calendar calendar) {
		trigger = calendar.getTimeInMillis();
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		pIntent = PendingIntent.getBroadcast(context, 0, new Intent(
				ACTION_TIME_REACHED), PendingIntent.FLAG_UPDATE_CURRENT);
		am.set(AlarmManager.RTC_WAKEUP, trigger, pIntent);

		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_TIME_REACHED);

		context.registerReceiver(receiver, filter);

		String alarmTime = (String) DateFormat.format(
				context.getString(R.string.date_format), trigger);
		Toast.makeText(context, "Set alarm " + alarmTime, Toast.LENGTH_SHORT)
				.show();
	}

	public void disable() {
		super.disable();

		if (pIntent != null) {
			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			am.cancel(pIntent);
			pIntent = null;
		}

		trigger = 0;
		context.unregisterReceiver(receiver);
	}

    public String getDBName() {
        return DB_NAME;
    }
}