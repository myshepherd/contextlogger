package ac.snu.cares.contextlogger.collector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wook on 13. 8. 10.
 */
public class MobileDataUsageCollector extends Collector {
    private static final String DB_NAME = "mobiledatausage";
    private static final String TAG = "MobileDataUsageCollector";

    private static final String ACTION_NETWORK_USAGE_LOG_ALARM ="ac.snu.cares.contextlogger.action.NETWORK_USAGE_LOG_ALARM";


    private enum NetworkState { MN, WIFI, DISCONNECTED };
    private enum DoLogState { INIT, NORMAL, TO_WIFI, TO_MN, TO_UNKNOWN, TO_DISCONNECTED };

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;

    private static final long POLLING_INTERVAL = 15 * ONE_MINUTE;
    //private static final long POLLING_INTERVAL = 60 * ONE_SECOND;

    private ConnectivityManager mConnectivityManager;
    private Service mContext;
    private NetworkState mCurNetState;
    private PackageManager mPackageManager;
    private PendingIntent pIntent;
    private DoLogState mDoLogState;

    //private HashMap<Integer, NetStatPerApp> mCurNetStatPerAppHashMap;

    private BroadcastReceiver alarmIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mDoLogState = DoLogState.NORMAL;
            awakeHandle(TAG);
        }
    };

    private void setAlarm(Service context){
        long setAlarmTime = SystemClock.elapsedRealtime();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_NETWORK_USAGE_LOG_ALARM), PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,setAlarmTime+POLLING_INTERVAL,POLLING_INTERVAL,pIntent);
    }

    private void registerAlarmIntentReceiver(Service context){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NETWORK_USAGE_LOG_ALARM);

        context.registerReceiver(alarmIntentReceiver, filter);
    }

    private BroadcastReceiver connectivityIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            NetworkInfo activeNetInfo = mConnectivityManager.getActiveNetworkInfo();

            if(activeNetInfo == null){
                mDoLogState = DoLogState.TO_DISCONNECTED;
                awakeHandle(TAG);
                return;
            }

            switch (activeNetInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    mDoLogState = DoLogState.TO_WIFI;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    mDoLogState = DoLogState.TO_MN;
                    break;
                default:
                    mDoLogState = DoLogState.TO_UNKNOWN;
                    break;
            }

            awakeHandle(TAG);
        }
    };


    private NetworkState getCurrentNetworkState(){
        NetworkInfo mn   = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if((!mn.isConnected()) && (!wifi.isConnected())){
            return NetworkState.DISCONNECTED;
        } else if(mn.isConnected()){
            return  NetworkState.MN;
        } else{
            return  NetworkState.WIFI;
        }
    }
    /*
    private class NetStatPerApp {
        private int uid;
        private String packageName;

        private long receivedTotal;
        private long sentTotal;

        public void setReceivedTotal(long bytes){
            this.receivedTotal = bytes;
        }

        public void setSentTotal(long bytes){
            this.sentTotal = bytes;
        }

        public int getUid() { return  this.uid; }
        public String getPackageName() { return this.packageName; }
        public long getReceivedTotal() { return this.receivedTotal; }
        public long getSentTotal() { return this.sentTotal; }

        public NetStatPerApp(int uid, String packageName){
            this.uid = uid;
            this.packageName = packageName;

            setReceivedTotal(0);
            setSentTotal(0);
        }
    }*/

    private boolean snapshotNetStat(){

        /*
        if(mDoLogState == DoLogState.INIT){
            collection.put(mDoLogState.name());
            Log.d(TAG, mDoLogState.name());
        }
        */


        collection.put(mCurNetState.name());
        //Log.d(TAG, mCurNetState.name());


        List<ApplicationInfo> packageList = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        Iterator<ApplicationInfo> iterator = packageList.iterator();

        while(iterator.hasNext()){
            ApplicationInfo info = iterator.next();
            int uid = info.uid;
            String packageName = info.packageName;
            long rxBytes = TrafficStats.getUidRxBytes(info.uid);
            long txBytes = TrafficStats.getUidTxBytes(info.uid);

            if((rxBytes == -1) && (txBytes == -1)){
                ; // do nothing
            } else {
                /*
                NetStatPerApp anApp = new NetStatPerApp(uid, packageName);
                anApp.setReceivedTotal(rxBytes);
                anApp.setSentTotal(txBytes);
                */
                String data = uid + "*" + packageName + "*" +  rxBytes + "*" + txBytes;
                collection.put(data);
                //Log.d(TAG, "**************** " + data);
            }
        }

        /*
        if(mDoLogState == DoLogState.NORMAL){
            return true;
        } else if (mDoLogState != DoLogState.INIT){
            collection.put(mDoLogState.name());
            Log.d(TAG, mDoLogState.name());
            mDoLogState = DoLogState.NORMAL;
        }
        */

        collection.put(mDoLogState.name());
        //Log.d(TAG, mDoLogState.name());
        mCurNetState = getCurrentNetworkState();

        return true;
    }

    @Override
    protected void handle() {

        snapshotNetStat();

    }

    public void enable(Service context) throws SQLiteException {
        super.enableBase(context, DB_NAME);

        mContext = context;
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mCurNetState = getCurrentNetworkState();
        //mCurNetStatPerAppHashMap = new HashMap<Integer, NetStatPerApp>();

        mDoLogState = DoLogState.INIT;
        snapshotNetStat();
        setAlarm(context);
        registerAlarmIntentReceiver(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        context.registerReceiver(connectivityIntentReceiver, filter);

        /*

        String lastLog = collection.getLatest();
        if (lastLog != null)
            prev = lastLog;
        else
            prev = null;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        context.registerReceiver(receiver, filter);
        */
    }

    public void disable() {
        super.disable();
        context.unregisterReceiver(alarmIntentReceiver);
        context.unregisterReceiver(connectivityIntentReceiver);
    }

    public String getDBName() {
        return DB_NAME;
    }
}
