package ac.snu.cares.contextlogger.collector;

import android.app.Service;
import android.content.Context;
import android.net.TrafficStats;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class DataUsageCollector extends Collector {

	public static final String DB_NAME = "data_usage";
    private static final String TAG = "DataUsageCollector";

	private static long DATA_THRESHOLD; //bytes

	private long prev_usage = 0;
	private long total_usage_recorded;
	private int directionHandling;

	private PhoneStateListener listener = new PhoneStateListener() {
		@Override
		public void onDataActivity(int direction) {
			directionHandling = direction;
			awakeHandle(TAG);
		}
	};

	@Override
	protected void handle() {
		boolean transfer;
		switch (directionHandling) {
		case TelephonyManager.DATA_ACTIVITY_INOUT:
			transfer = true;
			break;
		case TelephonyManager.DATA_ACTIVITY_IN:
		case TelephonyManager.DATA_ACTIVITY_OUT:
		case TelephonyManager.DATA_ACTIVITY_DORMANT:
		case TelephonyManager.DATA_ACTIVITY_NONE:
		default:
			transfer = false;
		}

		if (transfer) {
			long current_usage = TrafficStats.getMobileRxBytes()
					+ TrafficStats.getMobileTxBytes();
			long diff = current_usage - prev_usage;

			collection.put(total_usage_recorded + diff);

			total_usage_recorded += diff;
			prev_usage = current_usage;

			if (total_usage_recorded >= DATA_THRESHOLD) {
				collection.put(DATA_THRESHOLD + "(TH REACHED)");
				Toast.makeText(
						context,
						"Data usage reached "
								+ (DATA_THRESHOLD / (1024L * 1024L) + " MB"),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void reset() {
		collection.put(0);
		total_usage_recorded = 0;
	}

	public void setThreshold(int megabytes) {
		DATA_THRESHOLD = megabytes * 1024L * 1024L;
	}

	public void enable(Service context) {
		super.enableBase(context, DB_NAME);

		if (collection.size() == 0) {
			collection.put(0);
		}

		prev_usage = TrafficStats.getMobileRxBytes()
				+ TrafficStats.getMobileTxBytes();
		total_usage_recorded = collection.getLatestLong();

		((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
				.listen(listener, PhoneStateListener.LISTEN_DATA_ACTIVITY);
	}

	public void disable() {
		super.disable();

		((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
				.listen(listener, PhoneStateListener.LISTEN_NONE);
	}

    public String getDBName() {
        return DB_NAME;
    }
}
