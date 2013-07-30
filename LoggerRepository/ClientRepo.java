/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Repository directory and log & db file management class for one client
 */

import java.io.*;
import java.util.*;
import java.sql.*;

class ClientRepo {
	private String strClientName;
	ClientRepo(String strClientName) {
		this.strClientName = strClientName;
		makeClientRepoDir();
	}

	public FileOutputStream getInfoFileToWrite(String strFileNameBody) throws IOException {
		makeClientRepoDir();
		return new FileOutputStream(getInfoFilePath(strFileNameBody));
	}

	public FileOutputStream getDBFileToWrite(String strFileNameBody) throws IOException {
		makeClientRepoDir();
		return new FileOutputStream(getDBFilePath(strFileNameBody));
	}

	public boolean resolveMasterDB(String strTargetFileNameBody) throws IOException {
		makeClientRepoDir();

		ArrayList<String> targets = new ArrayList<String>();

		// Check existence of a master file
		File dest = new File(getMasterDBFilePath());
		if (dest.exists()) {
			// Add the given target for resolving to the master db
			targets.add(getDBFilePath(strTargetFileNameBody));
		} else {
			// Add all existed db files for resolving to the master db
			File dir = new File(getDirPath());
			File[] files = dir.listFiles();
			List<File> dirFileList = Arrays.asList(files);

			for (File file : files) {
				String strFileName = file.getName();
				if (strFileName.substring(strFileName.length() - 3).equals(".db"))
					targets.add(file.getAbsolutePath());
			}

			// Make a new master file
			try {
				Connection conn = DriverManager.getConnection("jdbc:sqlite:" + getMasterDBFilePath());
				conn.close();
			} catch (SQLException e) {
				System.out.println("+ Error occurs during creation of a new master db file.");
				return false;
			}
		}

		// Do resolving
		for (String target : targets) {
			if (!resolveOneDBFile(target)) {
				System.out.println("+ Error occurs during resolving db file. : " + target);
				break;
			}
		}

		return true;
	}

	private boolean resolveOneDBFile(String strDBFileName) {
		try {
			Connection connMaster = DriverManager.getConnection("jdbc:sqlite:" + getMasterDBFilePath());
			Connection connTarget = DriverManager.getConnection("jdbc:sqlite:" + strDBFileName);

			Statement master = connMaster.createStatement();
			Statement target = connTarget.createStatement();

			connMaster.prepareStatement("ATTACH DATABASE '" + strDBFileName + "' AS toMerge").execute();

			// Get all table names
			ArrayList<String> masterTables = getTableNameFromDB(master);
			ArrayList<String> targetTables = getTableNameFromDB(target);

			for (String strTable : targetTables) {
				String strCreateQuery = getCreateQuery(target, strTable);

				if (!strCreateQuery.contains("autoincrement")) // Only indexed table using autoincreament type will be merged.
					continue;

				if (!masterTables.contains(strTable)) {
					// If the table of the target does not exist in the master,
					// extract an original create sql query and apply it to the mater.
					strCreateQuery.replace("autoincrement", ""); // erase such type to avoid some collisions
					master.executeUpdate(strCreateQuery); // apply !
				}

				// Copy records
				connMaster.prepareStatement("REPLACE INTO " + strTable + " SELECT * FROM toMerge." + strTable).execute();
			}

			connMaster.close();
			connTarget.close();
		} catch (SQLException e) {
			System.out.println(e.toString());
			return false;
		}

		System.out.println("+ Resolving a DB file is completed. : " + strDBFileName);
		return true;
	}

	private ArrayList<String> getTableNameFromDB(Statement state) throws SQLException {
		ArrayList<String> ret = new ArrayList<String>();

		ResultSet rs = state.executeQuery("SELECT name FROM sqlite_master " +
				"WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%'" +
				"UNION ALL SELECT name FROM sqlite_temp_master WHERE type IN ('table', 'view')" + 
				"ORDER BY 1;");

		while (rs.next())
			ret.add(rs.getString("name"));

		return ret;
	}

	private String getCreateQuery(Statement state, String strTable) throws SQLException {
		ResultSet rs = state.executeQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '" + strTable + "'");
		rs.next();
		return rs.getString("sql"); // get the original one
	}

	// ClientRepo's dir/file path
	private String getDirPath() {
		return System.getProperty("user.dir") + File.separator + strClientName; 
	}

	private String getInfoFilePath(String strFileNameBody) {
		return getDirPath() + File.separator + strFileNameBody + ".txt";
	}

	private String getDBFilePath(String strFileNameBody) {
		return getDirPath() + File.separator + strFileNameBody + ".db";
	}

	private String getMasterDBFilePath() {
		return getDirPath() + File.separator + "master.db";
	}

	// Check(&make) a new folder for this client
	private void makeClientRepoDir() {
		File destDir = new File(getDirPath());
		if (!destDir.exists())
			destDir.mkdirs(); 
	}
}
