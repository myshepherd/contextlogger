package ac.snu.cares.contextlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by wook on 13. 8. 9.
 */
public class OutsideIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("TAG-TAG", "Download Completed");

    }
}
