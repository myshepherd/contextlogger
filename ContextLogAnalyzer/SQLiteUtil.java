/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * SQLLite Util
 */

import java.util.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class SQLiteUtil {
	public static ArrayList<String> getTableNameFromDB(Connection conn) throws SQLException {
		ArrayList<String> ret = new ArrayList<String>();

		Statement state = conn.createStatement();
		ResultSet rs = state.executeQuery("SELECT name FROM sqlite_master " +
				"WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%'" +
				"UNION ALL SELECT name FROM sqlite_temp_master WHERE type IN ('table', 'view')" + 
				"ORDER BY 1;");

		while (rs.next())
			ret.add(rs.getString("name"));

		return ret;
	}

	public static String getDateFromUnixTime(String strTime) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return formatter.format(Long.parseLong(strTime));
	}
}
