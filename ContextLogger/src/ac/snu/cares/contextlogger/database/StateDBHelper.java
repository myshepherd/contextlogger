package ac.snu.cares.contextlogger.database;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StateDBHelper extends SQLiteOpenHelper {
	public final static String EVENT_ID = "id";
	public final static String OCCUR_TIME = "time";
	public final static String EVENT_DESCRIPTION = "desc";

    public final static String DBFILE_NAME = "contextlog";

	public StateDBHelper(Context context, int version) {
		super(context, getDBfileName(context), null, version);
	}

    public static String getDBfileName(Context context) {
        return context.getExternalFilesDir(null) + File.separator +
                DBFILE_NAME + ".db";
    }

    public void checkCreateTable(String TABLE_NAME) {
        String DATABASE_CREATE = "create table IF NOT EXISTS " + TABLE_NAME + " (" + EVENT_ID
                + " integer primary key autoincrement, " + OCCUR_TIME
                + " bigint, " + EVENT_DESCRIPTION + " text);";

        getWritableDatabase().execSQL(DATABASE_CREATE);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
        // There is nothing to make a table as default.
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE");
		onCreate(db);
	}
}