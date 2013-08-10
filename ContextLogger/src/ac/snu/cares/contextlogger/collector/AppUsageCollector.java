package ac.snu.cares.contextlogger.collector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import ac.snu.cares.contextlogger.database.ConnState;

/**
 * Created by wook on 13. 8. 2.
 */
public class AppUsageCollector extends Collector {


    private static final String DB_NAME = "appusage";
    private static final String TAG = "AppUsageCollector";
    private static final String ACTION_APPUSAGE_LOG_ALARM ="ac.snu.cares.contextlogger.action.APPUSAGE_LOG_ALARM";

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    private static final long DEFAULT_POLLING_INTERVAL = 13 * ONE_MINUTE;
    //private static final long DEFAULT_POLLING_INTERVAL = 8 * ONE_SECOND;

    private enum TagType { AM, AM_CRITICAL };
    private enum State { DO_LOG_INIT, DO_LOG_FAILED, DO_LOG_ERROR, DO_LOG_SUCCEEDED };

    private long mLastLoggedTimeNano;

    private static class CollectableTag{

        private TagType mTagType;
        private String mDescriptionStr;

        public void setTagType(TagType tagType)         { mTagType = tagType;};
        public void setDescription(String description)  { mDescriptionStr = description; };

        public TagType  getTagType()        { return mTagType; };
        public String   getDescription()    { return mDescriptionStr; };


        public CollectableTag(TagType tagType, String description){
            setTagType(tagType);
            setDescription(description);
        }
    }

    private static final CollectableTag[] mCollectableTagArray = {
            new CollectableTag(TagType.AM_CRITICAL, "am_finish_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_task_to_front"),
            new CollectableTag(TagType.AM_CRITICAL, "am_new_intent"),
            new CollectableTag(TagType.AM_CRITICAL, "am_create_task"),
            new CollectableTag(TagType.AM_CRITICAL, "am_create_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_restart_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_resume_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_proc_bound"),
            new CollectableTag(TagType.AM_CRITICAL, "am_proc_died"),
            new CollectableTag(TagType.AM_CRITICAL, "am_pause_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_proc_start"),
            new CollectableTag(TagType.AM_CRITICAL, "am_low_memory"),
            new CollectableTag(TagType.AM_CRITICAL, "am_destroy_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_relaunch_resume_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_relaunch_activity"),
            new CollectableTag(TagType.AM_CRITICAL, "am_on_paused_called"),
            new CollectableTag(TagType.AM_CRITICAL, "am_on_resume_called"),
            new CollectableTag(TagType.AM_CRITICAL, "am_kill"),
            new CollectableTag(TagType.AM_CRITICAL, "am_create_service"),
            new CollectableTag(TagType.AM_CRITICAL, "am_destroy_service"),
            new CollectableTag(TagType.AM_CRITICAL, "am_drop_process"),
            new CollectableTag(TagType.AM, "am_schedule_service_restart"),
            new CollectableTag(TagType.AM, "am_crash")
    };

    private PendingIntent pIntent;
    private ArrayList<Integer> mCollectableTagIDArrayList;
    private int[] mCollectableTagIDArray;
    private ArrayList<EventLog.Event> logArrayList;

    private BroadcastReceiver alarmIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            awakeHandle(TAG);
        }
    };

    private void setAlarm(Service context){
        long setAlarmTime = SystemClock.elapsedRealtime();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_APPUSAGE_LOG_ALARM), PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,setAlarmTime+DEFAULT_POLLING_INTERVAL,DEFAULT_POLLING_INTERVAL,pIntent);
    }

    private void registerAlarmIntentReceiver(Service context){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APPUSAGE_LOG_ALARM);

        context.registerReceiver(alarmIntentReceiver, filter);
    }

    private boolean makeCollectableTagIDArrayList(){
        int tagID;
        String description;
        TagType tagType;

        mCollectableTagIDArrayList = new ArrayList<Integer>();

        for(int i = 0; i < mCollectableTagArray.length; i++){

            tagType = mCollectableTagArray[i].getTagType();
            description = mCollectableTagArray[i].getDescription();
            tagID = EventLog.getTagCode(description);

            try{
                if( (tagID == -1) && (tagType == TagType.AM_CRITICAL)){
                    throw new Exception();
                } else if (tagID != -1){
                    mCollectableTagIDArrayList.add(new Integer(tagID));
                }
                /*
                else{
                    Log.d(TAG, "Failed_Normal: TagID : " + tagID
                            + " Description: " + description);
                }*/

            }catch (Exception e){

                return false;
            }
        }
        return true;
    }
    private void makeCollectableTagIDArray(){

        mCollectableTagIDArray = new int[mCollectableTagIDArrayList.size()];

        for(int i = 0; i < mCollectableTagIDArrayList.size(); i++){
            mCollectableTagIDArray[i] = mCollectableTagIDArrayList.get(i).intValue();
        }

    }

    private void doLog(){
        int savedLogsCnt = 0;
        try{
            EventLog.readEvents(mCollectableTagIDArray, logArrayList);

            for (EventLog.Event curEventLog : logArrayList) {
                String dataStr;
                long eventLogTimestampNano = curEventLog.getTimeNanos();

                if(mLastLoggedTimeNano >= eventLogTimestampNano){
                    continue;
                }

                savedLogsCnt++;

                mLastLoggedTimeNano = eventLogTimestampNano;

                int eventLogTag = curEventLog.getTag();

                dataStr = ""+ eventLogTag;

                if(curEventLog.getData() instanceof Object[]) {

                    Object[] logData = (Object[]) curEventLog.getData();
                    for(int i = 0; i < logData.length; i++){
                        dataStr = dataStr + "*" + logData[i].toString();
                    }
                } else if (curEventLog.getData() instanceof  Long) {
                    dataStr = dataStr + "*" + curEventLog.getData().toString();
                } else if (curEventLog.getData() instanceof  String) {
                    dataStr = dataStr + "*" + curEventLog.getData();
                } else if (curEventLog.getData() instanceof  Integer) {
                    dataStr = dataStr + "*" + curEventLog.getData().toString();
                } else {
                    dataStr = dataStr + "*";
                }

                collection.put(eventLogTimestampNano, dataStr);

            }

        }catch(IOException e){
            State logState = State.DO_LOG_FAILED;
            collection.put(logState.name());
        }

        if(savedLogsCnt != 0){
            State logState = State.DO_LOG_SUCCEEDED;
            collection.put(logState.name());
        }
        logArrayList.clear();

    }

    @Override
    protected void handle() {
        /************* DEBUG **************/
        /*
        long currentTime = SystemClock.elapsedRealtime();
        String msg = "Current Time: " + currentTime;
        Log.d(TAG, msg);
        */
        /************* DEBUG **************/
        doLog();
    }

    @Override
    public void enable(Service context) {
        State logState;

        super.enableBase(context, DB_NAME);

        if(makeCollectableTagIDArrayList() == false){
            logState = State.DO_LOG_ERROR;
        }
        else {
            makeCollectableTagIDArray();
            mLastLoggedTimeNano = 0;
            logState = State.DO_LOG_INIT;

            setAlarm(context);
            registerAlarmIntentReceiver(context);
            logArrayList = new ArrayList<EventLog.Event>();
        }

        collection.put(logState.name());
    }

    public void disable() {
        super.disable();
        context.unregisterReceiver(alarmIntentReceiver);
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }
}