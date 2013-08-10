package ac.snu.cares.contextlogger.collector;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ac.snu.cares.contextlogger.database.EnableState;

/**
 * Created by wook on 13. 8. 9.
 */
public class RunningProcInfoCollector extends Collector {
    private static final String DB_NAME = "runprocinfo";
    private static final String TAG = "RunningProcInfoCollector";
    private static final String ACTION_RUN_PROC_INFO_LOG_ALARM ="ac.snu.cares.contextlogger.action.CUR_FG_SERVICES_LOG_ALARM";


    private static final int MAX_RETRIEVE_INFO_NUM = 100;

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    //private static final long DEFAULT_POLLING_INTERVAL = 2 * ONE_MINUTE;
    private static final long TIGHT_POLLING_INTERVAL = 31 * ONE_SECOND;
    private static final long LOOSE_POLLING_INTERVAL = 17 * ONE_MINUTE;

    private Service mContext;
    private ActivityManager mActivityManager;
    private PowerManager mPowerManager;
    private PendingIntent pIntent;
    private Intent screenIntentToHandle = null;
    private long mPolling_interval = TIGHT_POLLING_INTERVAL;
    private ArrayList<String> mDataToSave;

    private BroadcastReceiver alarmIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            awakeHandle(TAG);
        }
    };

    private BroadcastReceiver screenIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            screenIntentToHandle = intent;
            awakeHandle(TAG);
        }
    };

    private void setAlarm(Service context){

        long setAlarmTime = SystemClock.elapsedRealtime();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_RUN_PROC_INFO_LOG_ALARM), PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,setAlarmTime+mPolling_interval,mPolling_interval,pIntent);
    }

    private void registerAlarmIntentReceiver(Service context){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RUN_PROC_INFO_LOG_ALARM);

        context.registerReceiver(alarmIntentReceiver, filter);
    }

    private void doLog(){

        mDataToSave.add("RunningServiceInfo");
        List<ActivityManager.RunningServiceInfo> curRunningServicesInfoList = mActivityManager.getRunningServices(MAX_RETRIEVE_INFO_NUM);
        for(int i=0; i<curRunningServicesInfoList.size(); i++){
            ActivityManager.RunningServiceInfo curRunningServiceInfo = curRunningServicesInfoList.get(i);
            String packageName = curRunningServiceInfo.service.getPackageName();
            int flags = curRunningServiceInfo.flags;
            String processName = curRunningServiceInfo.process;
            boolean foreground = curRunningServiceInfo.foreground;
            String serviceName = curRunningServiceInfo.service.flattenToString();

            String data = packageName + "*" + flags + "*" + processName + "*" +
                    foreground + "*" + serviceName;

            mDataToSave.add(data);
        }

        mDataToSave.add("RunningAppProcessInfo");
        List<ActivityManager.RunningAppProcessInfo> appList = mActivityManager.getRunningAppProcesses();
        for(int i=0; i<appList.size(); i++){
            ActivityManager.RunningAppProcessInfo rapi = appList.get(i);
            String processName = rapi.processName;
            int importance = rapi.importance;
            int lru = rapi.lru;
            int uid = rapi.uid;

            String data = processName + "*" + importance + "*" + lru + "*" + uid;
            mDataToSave.add(data);
        }

        mDataToSave.add("RecentTaskInfo");
        List<ActivityManager.RecentTaskInfo> recentTaskList = mActivityManager.getRecentTasks(MAX_RETRIEVE_INFO_NUM, ActivityManager.RECENT_WITH_EXCLUDED);
        for(int i=0; i<recentTaskList.size(); i++){
            ActivityManager.RecentTaskInfo rti = recentTaskList.get(i);
            String packageName = rti.baseIntent.getComponent().getPackageName();
            mDataToSave.add(packageName);
        }
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memInfo);
        String memUsage = "MEMUSAGE*" + memInfo.totalMem + "*" + memInfo.availMem + "*" +  memInfo.lowMemory + "*" + memInfo.threshold;
        mDataToSave.add(memUsage);
        collection.putAll(mDataToSave);

        mDataToSave.clear();
    }


    @Override
    protected void handle() {

        if(screenIntentToHandle != null){
            if (screenIntentToHandle.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mPolling_interval = TIGHT_POLLING_INTERVAL;
            } else if (screenIntentToHandle.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mPolling_interval = LOOSE_POLLING_INTERVAL;
            }
            setAlarm(mContext);
            screenIntentToHandle = null;
            return;
        }
        doLog();

    }
    @Override
    public void enable(Service context) {

        super.enableBase(context, DB_NAME);
        mContext = context;
        mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mDataToSave = new ArrayList<String>();

        setAlarm(context);
        registerAlarmIntentReceiver(context);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenIntentReceiver, filter);

    }

    public void disable() {
        super.disable();
        context.unregisterReceiver(alarmIntentReceiver);
        context.unregisterReceiver(screenIntentReceiver);
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }

}
