package ac.snu.cares.contextlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by wook on 13. 8. 10.
 */
public class RestartAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_RESTART_SERVICE ="ac.snu.cares.contextlogger.action.RESTART_SERVICE";

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(ACTION_RESTART_SERVICE)){
            context.startService(new Intent(context, MainService.class));
        }
    }
}
