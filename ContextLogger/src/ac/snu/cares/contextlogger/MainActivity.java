package ac.snu.cares.contextlogger;

import ac.snu.cares.contextlogger.R;
import ac.snu.cares.contextlogger.database.DBFileSender;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	public static final String PREF_MAIN = "main_activity";
	public static final String KEY_SERVICE_ENABLED = "service_enabled";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public void onStart() {
		super.onStart();
		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);

		SharedPreferences prefs = getSharedPreferences(PREF_MAIN, MODE_PRIVATE);

		boolean se = prefs.getBoolean(KEY_SERVICE_ENABLED, false);
		tb.setChecked(se);

		if (tb.isChecked()) {
			startService(new Intent(this, MainService.class));
		}
	}

	@Override
	public void onStop() {
		SharedPreferences.Editor editor = getSharedPreferences(PREF_MAIN,
				MODE_PRIVATE).edit();

		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
		editor.putBoolean(KEY_SERVICE_ENABLED, tb.isChecked());

		editor.commit();

		super.onStop();
	}

	public void onToggled(View view) {
		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);

		if (tb.isChecked()) {
			startService(new Intent(this, MainService.class));
		} else {
			stopService(new Intent(this, MainService.class));
		}
	}

	public void onClickSettingButton(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
	}

    public void onClickSendButton(View view) {
        DBFileSender.getInstance().sendFile(this, this);
    }
}