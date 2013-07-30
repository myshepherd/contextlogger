package ac.snu.cares.contextlogger.database;

// This class is written as a singleton manner.

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Random;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;

import ac.snu.cares.contextlogger.MainService;


public class DBFileSender {
    private static final String ACTION_TIME_REACHED_FOR_FILE_SENDING =
            "ac.snu.cares.elgger.action.TIME_REACHED_FOR_FILE";
    private static final long TOO_LONG_TIME = AlarmManager.INTERVAL_DAY * 3;

    private static final String HOST_NAME = "147.46.78.119";
    private static final int PORT = 25800;

    private static final int BUFFER_SIZE = 8192;

    // Singleton instance
    private static DBFileSender mInstance;
    private DBFileSender() {}
    public static DBFileSender getInstance() {
        if (mInstance == null)
            mInstance = new DBFileSender();

        return mInstance;
    }

    // registration for sending its db file
    public void registerAutomaticSending(Service context) {
        nLastSentTime = System.currentTimeMillis(); //init last time to current

        // Trigger 1. Periodically sending (every day)
        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_TIME_REACHED_FOR_FILE_SENDING);
            context.registerReceiver(triggerByPeriodicAlarm, filter);

            AlarmManager am = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    ACTION_TIME_REACHED_FOR_FILE_SENDING), PendingIntent.FLAG_UPDATE_CURRENT);

            // To avoid server congestion, make the time varying during night
            Random oRandom = new Random();
            Calendar todayMidnight = Calendar.getInstance();
            todayMidnight.add(Calendar.DATE, 1);
            todayMidnight.set(Calendar.HOUR_OF_DAY, oRandom.nextInt(3)+1); // 1~3
            todayMidnight.set(Calendar.MINUTE, oRandom.nextInt(60)); // 0~59
            todayMidnight.set(Calendar.SECOND, 0);

            am.setRepeating(AlarmManager.RTC_WAKEUP,
                    todayMidnight.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntent);
            //System.currentTimeMillis() + 5*1000, AlarmManager.INTERVAL_DAY, pIntent);
        }

        // Trigger 2. if Wifi is enabled (and there is a pended sending)
        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

            context.registerReceiver(triggerByWifiState, filter);
        }

        Log.i("CARES Context Logger", "Set triggers for sending its db file");
    }


    // Triggers,
    private boolean bExistPendedSending = false;
    private long nLastSentTime;

    private BroadcastReceiver triggerByPeriodicAlarm = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Send file only if wifi is available
            if (!IsWifiAvailable(context)) {
                // if Wifi is unavailable
                // but the last sending time is elapsed TOO LONG TIME, process sending
                if (System.currentTimeMillis() - nLastSentTime < TOO_LONG_TIME) {
                    getInstance().bExistPendedSending = true;
                    Log.i("CARES Context Logger",
                            "Sending is pended because a wifi connection is not available.");
                    return;
                }
            }

            getInstance().sendFile(context, null);
        }
    };

    private BroadcastReceiver triggerByWifiState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If there is a pended sending
            if (IsWifiAvailable(context) && getInstance().bExistPendedSending == true) {
                getInstance().sendFile(context, null);
            }
        }
    };

    // Body for sending file
    public void sendFile(Context context, Activity toastActivity) {
        new SendFileTask(context, toastActivity).execute();
    }

    private class SendFileTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private Activity toastActivity;

        SendFileTask(Context context, Activity toastActivity) {
            super();
            this.context = context;
            this.toastActivity = toastActivity;
        }

        @Override
        protected Void doInBackground(Void... params) {
            long nPrevLastSentTime = nLastSentTime;

            nLastSentTime = System.currentTimeMillis();
            bExistPendedSending = false;

            Log.i("CARES Context Logger", "Start to send the collected file");

            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(HOST_NAME, PORT));
                BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
                FileInputStream in = new FileInputStream(StateDBHelper.getDBfileName(context));

                byte[] buffer = new byte[BUFFER_SIZE];

                // Send the first chunk (Texted Client Info. Maximum length is BUFFER_SIZE(8192).)
                byte[] tmp = getClientInfo(context).getBytes();
                System.arraycopy(tmp, 0,
                        buffer, 0, tmp.length < BUFFER_SIZE ? tmp.length : BUFFER_SIZE - 1);
                out.write(buffer, 0, BUFFER_SIZE);

                // Send the db file
                int nTotalBytes = 0;
                int bytesRead = 0;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                    nTotalBytes += bytesRead;
                }
                out.flush();
                out.close();
                in.close();

                MainService.clearDBData();

                Log.i("CARES Context Logger", "Success to send a file (" + Integer.toString(nTotalBytes) + ")");

                if (toastActivity != null) {
                    toastActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(toastActivity, "Success to send the collected file", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                nLastSentTime = nPrevLastSentTime;
                bExistPendedSending = true;

                Log.i("CARES Context Logger", "Fail to send a file (1)");
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    nLastSentTime = nPrevLastSentTime;
                    bExistPendedSending = true;
                    Log.i("CARES Context Logger", "Fail to send a file (2)");
                }
            }

            return null;
        }
    }

    public static String getClientInfo(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        // if the phone number is unavailable, just use its device id as the client name.
        String strLineNumber = telephonyManager.getLine1Number();
        String strClientName = (strLineNumber != null && !strLineNumber.equals("")) ?
                strLineNumber : telephonyManager.getDeviceId();

        return
                "ClientName\t" + strClientName + "\n" +
                "Line1Number\t" + telephonyManager.getLine1Number() + "\n" +
                "DeviceId\t" + telephonyManager.getDeviceId() + "\n" +
                "Operator\t" + telephonyManager.getNetworkOperatorName() + "\n" +
                "Model\t" + Build.MODEL + "\n" +
                "Product\t" + Build.PRODUCT + "\n" +
                "Device\t" + Build.DEVICE + "\n" +
                "SDK\t" + Build.VERSION.RELEASE + "\n" +
                "Software\t" + telephonyManager.getDeviceSoftwareVersion() + "\n" +
                "User\t" + Build.USER + "\n" +
                "Board:\t" + Build.BOARD + "\n" +
                "Brand:\t" + Build.BRAND + "\n" +
                "CPU_ABI:\t" + Build.CPU_ABI + "\n" +
                "Display:\t" + Build.DISPLAY + "\n" +
                "Fingerprint:\t" + Build.FINGERPRINT + "\n" +
                "manufacturer:\t" + Build.MANUFACTURER + "\n"
            ;
    }

    private static boolean IsWifiAvailable(Context context)
    {
        ConnectivityManager m_NetConnectMgr =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean bConnect = false;
        try {
            if( m_NetConnectMgr == null ) return false;

            NetworkInfo info = m_NetConnectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            bConnect = (info.isAvailable() && info.isConnected());
        } catch(Exception e) {
            return false;
        }

        return bConnect;
    }
}
