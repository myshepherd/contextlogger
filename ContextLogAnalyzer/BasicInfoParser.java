/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Basic Analyzer
 */

import java.io.*;
import java.util.*;
import java.sql.*;

public class BasicInfoParser {
	public static enum BasicCollector {
		NETWORKSTATE    ("networkstate",   "Network State", "networkstate"),
		SCREENSTATE     ("screenstate",    "Screen State", "screenstate"),
		WIFI_HOTSPOT    ("wifi_hotspot",    "Wi-Fi Hotspot", "wifihotshot"),
		WIFI            ("wifi",           "Wi-Fi", "wifistate"),
		HOST_USB        ("usb",            "Usb Connection to Host", "usbconnection"),
		RINGER          ("ringer",     "Sound Mode", "soundmode"),
		HEADSET     	("headset",        "Headset Coonection", "headsetconnection"),
		DOCK            ("dock",           "Dock Connection", "dockconnection"),
		BLUETOOTH       ("bluetooth",  "Bluetooth", "bluetoothconnection"),
		BATTERY     	("battery",        "Charger Connection", "chargerconnection"),
		DATA_USE        ("data_usage",  "Mobile Data Usage", "mobiledatausage"),
		SYNC            ("sync",           "Auto Sync", "autosync"),
		AUTO_ROTATE 	("auto_rotate", "Auto Rotate Screen", "autorotate"),
		HAPTIC          ("haptic",     "Vibrate on Touch", "vibontouch"),
		TIME            ("time",           "Stamp Certain Time", "stampedtime");

		String table;
		String annotation;
		String output;

		static int length = values().length;

		BasicCollector(String _table, String _annotation, String _output) {
			this.table = _table;
			this.annotation = _annotation;
			this.output = _output;
		}

		static BasicCollector valueOfTable(String tbl) {
			for (BasicCollector c : values()) {
				if (tbl.equals(c.table)) {
					return c;
				}
			}
			return null;
		}
	}

    private static final String START_MSG = "START LOGGING";
	private static final String STOP_MSG = "STOP LOGGING";

	private Connection conn;
	public BasicInfoParser(Connection conn) {
		this.conn = conn;
	}

	public boolean parse() throws SQLException, IOException {
		ArrayList<String> tables = SQLiteUtil.getTableNameFromDB(conn);

		for (BasicCollector collector : BasicCollector.values()) {
			if (!tables.contains(collector.table))
				continue;

			Statement state = conn.createStatement();
			ResultSet rs = state.executeQuery("SELECT time, desc FROM "
					+ collector.table + " ORDER BY id;");

			FileOutputStream out = new FileOutputStream(ContextLogAnalyzer.env.strDestDir + File.separator + collector.output + ".txt");

			while (rs.next()) {
				String strTime = rs.getString("time");
				String strDesc = rs.getString("desc");

				if (ContextLogAnalyzer.env.bUseHumanReadableTime)
					strTime = SQLiteUtil.getDateFromUnixTime(strTime);

				if (ContextLogAnalyzer.env.bIgnoreLoggerOnOffMsg &&
					(strDesc.equals(STOP_MSG) || strDesc.equals(START_MSG)))
					continue;

				String strLine = strTime + "\t" + strDesc + "\n";
				byte[] bytes = strLine.getBytes();
				out.write(bytes, 0, bytes.length);
			}

			out.close();
			System.out.println("Basic Information [" + collector.annotation + "] is completely parsed.");
		}

		return true;
	}
}
