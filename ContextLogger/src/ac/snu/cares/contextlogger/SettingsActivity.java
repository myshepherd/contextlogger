package ac.snu.cares.contextlogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import ac.snu.cares.contextlogger.collector.DataUsageCollector;
import ac.snu.cares.contextlogger.database.StateCollection;
import ac.snu.cares.contextlogger.R;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;

public class SettingsActivity extends Activity implements
		OnCheckedChangeListener {
	public static final String PREF_SETTINGS = "settings_activity";
	public static final String KEY_ENABLED_LIST = "enabled_collectors";
	public static final String KEY_DATA_THRESHOLD = "data_threshold";
	public static final String KEY_ALARM_TIME = "alarm_time";

	private MainService mService;

	private ServiceConnection conn = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((MainService.LocalBinder) service).getService();
		}
	};

	private static String[] labels;
	static {
		MainService.CollectorName[] cnames = MainService.CollectorName.values();
		labels = new String[cnames.length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = cnames[i].label;
		}
	}

    private boolean[] checked;
	private String dataThreshold;
	private Calendar alarmTime;

	private View dialogHandling;
	private CompoundButton checkboxHandling;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

        if (checked == null || checked.length != labels.length) {
            checked = new boolean[labels.length];
        }

		CollectorAdapter adapter = new CollectorAdapter(this,
				R.layout.clist_view_item, labels);
		ListView listView = (ListView) findViewById(R.id.clistView);
		listView.setAdapter(adapter);
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS,
				MODE_PRIVATE);
		applyStringToCheckedStatus(prefs.getString(KEY_ENABLED_LIST, ""), checked);

		dataThreshold = prefs.getString(KEY_DATA_THRESHOLD,
				getString(R.string.default_data_threshold));
		String alarmTimeStr = prefs.getString(KEY_ALARM_TIME, "");
		SimpleDateFormat format = new SimpleDateFormat(
				getString(R.string.date_format));
		try {
			alarmTime = Calendar.getInstance();
			alarmTime.setTime(format.parse(alarmTimeStr));
		} catch (ParseException e) {
			alarmTime = null;
		}

		bindService(new Intent(this, MainService.class), conn,
				Context.BIND_WAIVE_PRIORITY);
	}

	@Override
	public void onStop() {
		super.onStop();

		unbindService(conn);

		SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putString(KEY_ENABLED_LIST, getStringFromCheckedStatus(checked));
		editor.putString(KEY_DATA_THRESHOLD, dataThreshold);
		if (alarmTime != null) {
			editor.putString(KEY_ALARM_TIME, (String) DateFormat.format(
					getString(R.string.date_format), alarmTime));
		}

		editor.commit();
	}

    public static String getStringFromCheckedStatus(boolean [] arrayChecked) {
        MainService.CollectorName[] cnames = MainService.CollectorName.values();

        String ret = "";
        for (int i = 0; i < arrayChecked.length; ++ i) {
            String key = cnames[i].className;
            ret += key + "\t" + ((arrayChecked[i]) ? "1" : "0") + "\n";
        }

        return ret;
    }

    public static void applyStringToCheckedStatus(String str, boolean [] arrayChecked) {
        MainService.CollectorName[] cnames = MainService.CollectorName.values();
        if (arrayChecked.length != MainService.CollectorName.length)
            return;

        for (int i = 0; i < arrayChecked.length; ++i) {
            arrayChecked[i] = cnames[i].bDefault;
        }

        String[] strItemList = str.split("\n");
        for (String strItemPair : strItemList) {
            String[] strItems = strItemPair.split("\t");

            MainService.CollectorName col = MainService.CollectorName.valueOfClassName(strItems[0]);
            if (col == null)
                continue;

            arrayChecked[col.ordinal()] = strItems[1].equals("1");
        }
    }

	private void showTimePickDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set Date and Time");
		builder.setPositiveButton("Set", mTimeSetListener);
		builder.setOnCancelListener(mTimeCancleListener);

		View dlayout = getLayoutInflater().inflate(R.layout.time_select_layout,
				(ViewGroup) findViewById(R.id.dialog_root_layout), false);
		builder.setView(dlayout);

		NumberPicker np = (NumberPicker) dlayout.findViewById(R.id.hourPicker);
		np.setMaxValue(23);
		np.setMinValue(0);

		if (alarmTime != null) {
			np.setValue(alarmTime.get(Calendar.HOUR_OF_DAY));

			int year = alarmTime.get(Calendar.YEAR);
			int month = alarmTime.get(Calendar.MONDAY);
			int date = alarmTime.get(Calendar.DAY_OF_MONTH);
			((DatePicker) dlayout.findViewById(R.id.datePicker)).init(year,
					month, date, null);
		}

		dialogHandling = dlayout;

		builder.show();
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		String label = (String) buttonView.getText();
		int idx = MainService.CollectorName.valueOfLabel(label).ordinal();

		if (checked[idx] == isChecked) {
			return;
		}

		checked[idx] = isChecked;

		if (!isChecked && mService != null) {
			mService.disableCollector(label);
			return;
		}

		if (idx == MainService.CollectorName.TIME.ordinal()) {
			if (alarmTime == null) {
				checkboxHandling = buttonView;
				showTimePickDialog();
			} else if (mService != null) {
				mService.enableTimeCollector(alarmTime);
			}
		} else {
			if (mService != null) {
				mService.enableCollector(label);
			}
		}
	}

	DialogInterface.OnClickListener mTimeSetListener = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {

			DatePicker dp = (DatePicker) dialogHandling
					.findViewById(R.id.datePicker);
			NumberPicker np = (NumberPicker) dialogHandling
					.findViewById(R.id.hourPicker);

			if (alarmTime == null) {
				alarmTime = Calendar.getInstance();
			}
			alarmTime.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(),
					np.getValue(), 0);

			if (mService != null && checked[MainService.CollectorName.TIME.ordinal()]) {
				mService.enableTimeCollector(alarmTime);
			}

			checkboxHandling = null;
			dialogHandling = null;
		}
	};

	OnCancelListener mTimeCancleListener = new OnCancelListener() {

		public void onCancel(DialogInterface dialog) {
			if (checkboxHandling != null && alarmTime == null) {
				checkboxHandling.setChecked(false);
				checkboxHandling = null;
			}
			dialogHandling = null;
		}
	};

	private class CollectorAdapter extends ArrayAdapter<String> {

		Context context;
		int layoutResourceId;
		String[] label;

		public CollectorAdapter(Context context, int resourceId,
				String[] objects) {
			super(context, resourceId, objects);
			this.context = context;
			this.layoutResourceId = resourceId;
			this.label = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CheckBox cb = null;

			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				cb = (CheckBox) row.findViewById(R.id.checkbox);

				cb.setOnCheckedChangeListener((OnCheckedChangeListener) context);

				row.setTag(cb);
			} else {
				cb = (CheckBox) row.getTag();
			}

			cb.setText(label[position]);

			if (checked[position] != cb.isChecked()) {
				cb.setChecked(checked[position]);
			}

			Button btn = (Button) row.findViewById(R.id.button);
			LinearLayout dataThd = (LinearLayout) row
					.findViewById(R.id.dataThd);
			if (position == MainService.CollectorName.DATA_USE.ordinal()) {
				btn.setText(getString(R.string.button_data_usage));
				btn.setVisibility(View.VISIBLE);
				btn.setEnabled(true);

				btn.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						if (mService != null) {
							mService.resetDataUsage();
						} else {
							StateCollection col = new StateCollection(context,
									DataUsageCollector.DB_NAME);
							col.open();
							col.put(0);
							// col.close(); // // DB file should be always opened for other states' tables.
						}
					}
				});

				dataThd.setVisibility(View.VISIBLE);
				dataThd.setEnabled(true);

				EditText etext = (EditText) row.findViewById(R.id.editText);
				etext.setText(dataThreshold);
				etext.setOnEditorActionListener(new EditText.OnEditorActionListener() {

					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH
								|| actionId == EditorInfo.IME_ACTION_DONE
								|| event.getAction() == KeyEvent.ACTION_DOWN
								&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

							String s = v.getText().toString();
							if (s.length() == 0) {
								Toast.makeText(context, "Insert a value",
										Toast.LENGTH_SHORT).show();
								return true;
							}

							if (s.equals(dataThreshold)) {
								return false;
							}

							if (mService != null) {
								int changedThreshold = Integer.valueOf(s);
								mService.setDataUsageThreshold(changedThreshold);
							}

							dataThreshold = s;
						}
						return false;
					}
				});
			} else if (position == MainService.CollectorName.TIME.ordinal()) {
				btn.setText(getString(R.string.button_time));
				btn.setVisibility(View.VISIBLE);
				btn.setEnabled(true);

				btn.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						showTimePickDialog();
					}
				});

				dataThd.setVisibility(View.INVISIBLE);
				dataThd.setEnabled(false);
			} else {
				btn.setVisibility(View.INVISIBLE);
				btn.setEnabled(false);

				dataThd.setVisibility(View.INVISIBLE);
				dataThd.setEnabled(false);
			}
			return row;
		}
	}
}