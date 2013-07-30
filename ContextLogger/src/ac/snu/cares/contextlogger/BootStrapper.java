package ac.snu.cares.contextlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootStrapper extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("BootStrapper", "Intent received");
		SharedPreferences prefs = context.getSharedPreferences(
				MainActivity.PREF_MAIN, Context.MODE_PRIVATE);
		boolean se = prefs.getBoolean(MainActivity.KEY_SERVICE_ENABLED, false);

		if (se) {
			context.startService(new Intent(context, MainService.class));
		}
	}

}
