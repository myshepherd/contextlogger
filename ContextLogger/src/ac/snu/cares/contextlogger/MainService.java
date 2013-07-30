package ac.snu.cares.contextlogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import ac.snu.cares.contextlogger.database.DBFileSender;
import ac.snu.cares.contextlogger.database.StateCollection;
import ac.snu.cares.contextlogger.R;
import ac.snu.cares.contextlogger.collector.Collector;
import ac.snu.cares.contextlogger.collector.DataUsageCollector;
import ac.snu.cares.contextlogger.collector.TimeCollector;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service {

	public static enum CollectorName {
        NETWORKSTATE	("NetworkStateCollector",	"Network State", true),
        SCREENSTATE     ("ScreenStateCollector",	"Screen State", true),
		WIFI_HOTSPOT	("WiFiHotspotCollector",	"Wi-Fi Hotspot", false),
		WIFI			("WiFiCollector",			"Wi-Fi", false),
		HOST_USB		("UsbCollector",			"Usb Connection to Host", false),
		RINGER			("RingerCollector",		"Sound Mode", false),
		HEADSET		("HeadsetCollector",		"Headset Coonection", true),
		DOCK			("DockCollector",			"Dock Connection", true),
		BLUETOOTH		("BluetoothCollector",	"Bluetooth", true),
		BATTERY		("BatteryCollector",		"Charger Connection", true),
		DATA_USE		("DataUsageCollector",	"Mobile Data Usage", false),
		SYNC			("SyncCollector",			"Auto Sync", false),
		AUTO_ROTATE	("AutoRotateCollector",	"Auto Rotate Screen", false),
		HAPTIC			("HapticCollector",		"Vibrate on Touch", false),
		TIME			("TimeCollector",			"Stamp Certain Time", false);

		String className;
		String label;
        boolean bDefault;

		static int length = values().length;

		CollectorName(String _classname, String _label, boolean _bDefault) {
			this.className = _classname;
			this.label = _label;
            this.bDefault = _bDefault;
		}

		static CollectorName valueOfLabel(String label) {
			for (CollectorName c : values()) {
				if (label.equals(c.label)) {
					return c;
				}
			}
			return null;
		}

        static CollectorName valueOfClassName(String _className) {
            for (CollectorName c : values()) {
                if (_className.equals(c.className)) {
                    return c;
                }
            }
            return null;
        }
	}

	private static Collector[] collectors;

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		MainService getService() {
			return MainService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		collectors = new Collector[CollectorName.length];

		SharedPreferences prefs = getSharedPreferences(
				SettingsActivity.PREF_SETTINGS, MODE_PRIVATE);
		String strChecked = prefs.getString(SettingsActivity.KEY_ENABLED_LIST,"");
        boolean[] checked = new boolean[CollectorName.length];
        SettingsActivity.applyStringToCheckedStatus(strChecked, checked);

		for (CollectorName cname : CollectorName.values()) {
			int idx = cname.ordinal();

			collectors[idx] = createCollector(cname);

			if (cname == CollectorName.TIME) {
				String alarmTimeStr = prefs.getString(
						SettingsActivity.KEY_ALARM_TIME, "");
				SimpleDateFormat format = new SimpleDateFormat(
						getString(R.string.date_format));
				Calendar alarmTime = Calendar.getInstance();
				try {
					alarmTime.setTime(format.parse(alarmTimeStr));
				} catch (ParseException e) {
					alarmTime = null;
				}

				if (alarmTime != null) {
					((TimeCollector) collectors[idx]).setTime(alarmTime);
				}
			}

			if (checked[idx]) {
				collectors[idx].enable(this);
			}
		}

        DBFileSender.getInstance().registerAutomaticSending(this);
	}

	@Override
	public void onDestroy() {
		for (Collector col : collectors) {
			if (col != null && col.enabled) {
				col.disable();
			}
		}
		collectors = null;
	}

	public void disableCollector(String label) {
		Collector c = collectors[CollectorName.valueOfLabel(label).ordinal()];
		if (c != null && c.enabled) {
			c.disable();
		}
	}

	public void enableCollector(String label) {
		CollectorName cname = CollectorName.valueOfLabel(label);
		Collector c = collectors[cname.ordinal()];
		if (c != null && !c.enabled) {
			c.enable(this);
		} else {
			collectors[cname.ordinal()] = createCollector(cname);
			collectors[cname.ordinal()].enable(this);
		}
	}

	public void resetDataUsage() {
		DataUsageCollector dc = (DataUsageCollector) collectors[CollectorName.DATA_USE
				.ordinal()];
		if (dc.enabled) {
			dc.reset();
		} else {
			StateCollection collection = new StateCollection(this,
					DataUsageCollector.DB_NAME);
			collection.open();
			collection.put(0);
			// collection.close(); // DB file should be always opened for other states' tables.
		}
	}

    public static void clearDBData() {
        ArrayList<String> lstTargets = new ArrayList<String>();
        for (Collector col : collectors) {
            if (col != null) {
                lstTargets.add(col.getDBName());
            }
        }

        StateCollection.clearTables(lstTargets);
    }

	public void setDataUsageThreshold(int threshold) {
		DataUsageCollector dc = (DataUsageCollector) collectors[CollectorName.DATA_USE
				.ordinal()];
		dc.setThreshold(threshold);
	}

	public void enableTimeCollector(Calendar c) {
		TimeCollector tc = (TimeCollector) collectors[CollectorName.TIME
				.ordinal()];
		if (tc == null) {
			return;
		}

		if (tc.enabled) {
			tc.disable();
		}

		tc.setTime(c);
		tc.enable(this);
	}

	public String getLogInfo(int idx) {
		String ret = "";

		Collector c = collectors[idx];
		String log = c.getLatest();
		if (log != null) {
			ret += log;
		}

		ret += "(" + c.size() + "logs)";

		return ret;
	}

	private Collector createCollector(CollectorName cname) {
		String collectorPackageName = Collector.class.getPackage().getName();
		try {
			@SuppressWarnings("unchecked")
			Class<Collector> c = (Class<Collector>) Class
					.forName(collectorPackageName + "." + cname.className);
			Collector col = c.newInstance();
			return col;
		} catch (ClassNotFoundException e) {
			Log.e("MainService", "can't find class " + cname.className);
		} catch (InstantiationException e) {
			Log.e("MainService", "can't instantiate the class "
					+ cname.className);
		} catch (IllegalAccessException e) {
			Log.e("MainService", "can't access to the class " + cname.className);
		} catch (SQLiteException e) {
			Log.e("MainService", e.getMessage());
		}

		return null;
	}

}