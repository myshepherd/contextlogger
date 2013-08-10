package ac.snu.cares.contextlogger.collector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by wook on 13. 8. 9.
 */
public class PackageManagementLogCollector extends Collector {
    private static final String TAG = "PackageManagementLogCollector";
    private static final String DB_NAME = "packagemanagement";

    private enum State { INIT, ADDED, REPLACED, REMOVED, ERR };

    private Intent mIntentToHandle;

    private BroadcastReceiver packageManagementReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIntentToHandle = intent;
            awakeHandle(TAG);
        }
    };

    @Override
    protected void handle() {
        String intentAction = mIntentToHandle.getAction();
        String packageName = mIntentToHandle.getData().getSchemeSpecificPart();
        String msgToSave;

        if(intentAction.equals(Intent.ACTION_PACKAGE_ADDED)){
            msgToSave = State.ADDED.name() + "*" + packageName;
        }else if(intentAction.equals(Intent.ACTION_PACKAGE_REPLACED)){
            msgToSave = State.REPLACED.name() + "*" + packageName;
        }else if(intentAction.equals(Intent.ACTION_PACKAGE_REMOVED)){
            msgToSave = State.REMOVED.name() + "*" + packageName;
        }else {
            msgToSave = State.ERR.name();
        }

        collection.put(msgToSave);
    }

    @Override
    public void enable(Service context) {
        super.enableBase(context, DB_NAME);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        context.registerReceiver(packageManagementReceiver, filter);

        collection.put(State.INIT.name());
    }

    @Override
    public void disable() {
        super.disable();

        context.unregisterReceiver(packageManagementReceiver);
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }
}
