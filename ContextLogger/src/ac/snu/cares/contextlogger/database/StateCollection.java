package ac.snu.cares.contextlogger.database;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class StateCollection {

	private static final int DB_VERSION_NUMBER = 3;
	public static final int NO_LIMIT = -1;

	public static final String START_MSG = "START LOGGING";
	public static final String STOP_MSG = "STOP LOGGING";

    private static StateDBHelper dbHelper; // Make it static cause we will use only one db file.

	private SQLiteDatabase database;
    private String TABLE_NAME;

	private long newestId;


    public static void clearTables(ArrayList<String> lstTableName) {
        if (dbHelper == null)
            return;

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        if (database == null)
            return;

        for (String tbName : lstTableName) {
            try {
                database.delete(tbName, null, null);
            } catch (SQLiteException e ) {} // if the table doesn't exists,
        }
    }

    public StateCollection(Context context, String name) {
        if (dbHelper == null)
		    dbHelper = new StateDBHelper(context, DB_VERSION_NUMBER);

        TABLE_NAME = name;
        dbHelper.checkCreateTable(TABLE_NAME);
	}

    public void open() throws SQLiteException {
		database = dbHelper.getWritableDatabase();

		newestId = 0;
		String query = "SELECT COUNT(" + StateDBHelper.EVENT_ID + ") FROM "
				+ TABLE_NAME;
		Cursor cursor = database.rawQuery(query, null);
		if (cursor != null) {
			cursor.moveToFirst();
			newestId = cursor.getInt(0);
		}
		cursor.close();
	}

    // There is no case for close the db file. (i.e., db file is always opened.)
    /*
	public void close() {
		dbHelper.close();
	}
	*/

	public void put(String desc) {
		ContentValues values = new ContentValues();
		values.put(StateDBHelper.EVENT_DESCRIPTION, desc);

		insert(values);
	}

	public void put(long desc) {
		ContentValues values = new ContentValues();
		values.put(StateDBHelper.EVENT_DESCRIPTION, String.valueOf(desc));

		insert(values);
	}

	public String getLatest() {
		String ret = null;
		int goup = 0;
		while (ret == null && goup < newestId) {
			ret = get(newestId - goup);
			if (ret == null || ret.equals(START_MSG) || ret.equals(STOP_MSG)) {
				ret = null;
				goup++;
			}
		}

		return ret;
	}

	public long getLatestLong() {
		long ret = -1;
		int goup = 0;
		while (ret < 0 && goup < newestId) {
			try {
				ret = Long.valueOf(get(newestId - goup));
			} catch (NumberFormatException e) {
				ret = -1;
				goup++;
			}
		}

		return ret;
	}

	public long size() {
		return newestId;
	}

	private String get(long i) {
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE "
				+ StateDBHelper.EVENT_ID + " = " + Long.toString(i);
		Cursor cursor = database.rawQuery(query, null);
		String desc = null;
		while (cursor.moveToNext()) {
			desc = cursor.getString(2);
		}
		cursor.close();

		return desc;
	}

	private void insert(ContentValues values) {
		values.put(StateDBHelper.OCCUR_TIME, System.currentTimeMillis());

		long insertedId = database.insert(TABLE_NAME, null, values);
		if (insertedId == -1) {
			Log.e("StateCollection", "can't record the event on"
					+ TABLE_NAME);
		} else {
			newestId = insertedId;
		}
	}
}