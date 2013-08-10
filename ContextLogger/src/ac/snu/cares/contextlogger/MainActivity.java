package ac.snu.cares.contextlogger;

import ac.snu.cares.contextlogger.R;
import ac.snu.cares.contextlogger.database.DBFileSender;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
	public static final String PREF_MAIN = "main_activity";
	public static final String KEY_SERVICE_ENABLED = "service_enabled";

    private static final String TAG ="MainActivity";

    private boolean mReadLogsPermissionFlag;
    private Toast monlyToast;

    private boolean checkReadLogsPermission(){
        if(MainActivity.this.getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS,MainActivity.this.getPackageName()) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED){
            return false;

        }else{
            return true;
        }
    }

    private void showToast(String msg){
        int offsetX = 0;
        int offsetY = 0;

        monlyToast.setText(msg);
        monlyToast.setDuration(Toast.LENGTH_LONG);

        LinearLayout toastLayout = (LinearLayout) monlyToast.getView();
        TextView toastTV = (TextView) toastLayout.getChildAt(0);
        toastTV.setTextSize(30);
        toastTV.setGravity(Gravity.CENTER);

        monlyToast.setGravity(Gravity.CENTER,offsetX,offsetY);

        monlyToast.show();
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        monlyToast= Toast.makeText(this, null, Toast.LENGTH_LONG);

        mReadLogsPermissionFlag = checkReadLogsPermission();

        if(!mReadLogsPermissionFlag){
            showToast(getResources().getString(R.string.permissionHasNotBeenGranted));
        }else{
            showToast(getResources().getString(R.string.permissionHasBeenGranted));
        }
	}

	@Override
	public void onStart() {
		super.onStart();

		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);
        Button uploadButton = (Button) findViewById(R.id.uploadButton);

		SharedPreferences prefs = getSharedPreferences(PREF_MAIN, MODE_PRIVATE);

        tb.setEnabled(mReadLogsPermissionFlag);
        tb.setClickable(mReadLogsPermissionFlag);
        uploadButton.setEnabled(mReadLogsPermissionFlag);
        tb.setClickable(mReadLogsPermissionFlag);

        if(!mReadLogsPermissionFlag){
            tb.setText(getResources().getString(R.string.disabled));
            showToast(getResources().getString(R.string.permissionHasNotBeenGranted));
        } else {
		    boolean se = prefs.getBoolean(KEY_SERVICE_ENABLED, false);
		    tb.setChecked(se);

    		if (tb.isChecked()) {
	    		startService(new Intent(this, MainService.class));
		    }
        }

	}

    @Override
    protected void onResume() {
        super.onResume();
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