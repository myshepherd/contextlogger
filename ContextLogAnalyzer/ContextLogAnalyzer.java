/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Context Logger Analyzer ::
 * Analyzer for collected logs from Logger app
 */

import org.apache.commons.cli.*; // For command line parsing

import java.io.*;
import java.util.*;
import java.sql.*;

public class ContextLogAnalyzer {

    public static void main(String[] args) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");

		if (parseOption(args) == false)
			return;

		System.out.println(
				"##################################################\n" +
				":: Context Log Analyzer ::\n" +
				"Analyzer for collected logs from Logger app\n\n" +
				"Copyright CARES 2013\n" + 
				"##################################################"
				);

		if (!startParsing()) {
			System.out.println("Error occurs during analysis.");
		}
    }

	private static boolean startParsing() {
		try
		{
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + strFileName);

			// Make dest dir
			if (env.strDestDir.equals(""))
				env.strDestDir = strFileName.substring(0, strFileName.lastIndexOf('.'));

			File destDir = new File(env.strDestDir);
			if (!destDir.exists())
				destDir.mkdirs(); 

			// Basic Info
			if (!parseBasicInformation(conn))
				return false;

			conn.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private static boolean parseBasicInformation(Connection conn) throws SQLException, IOException {
		BasicInfoParser parser = new BasicInfoParser(conn);

		return parser.parse();
	}

	private static String strFileName = "";
	private static boolean parseOption(String [] args) {
		// Set options
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("Output directory").hasArg().withDescription("Indicate [o]utput directory").create("o"));
		options.addOption(OptionBuilder.withDescription("Make output using human-readable [T]ime format").create("T"));
		options.addOption(OptionBuilder.withDescription("[i]nclude logger on/ogg message").create("i"));

		// Parse Command
		try {
			CommandLine cmd = parser.parse(options, args);

			try {
				// Options
				{
					env.bUseHumanReadableTime = cmd.hasOption("T");
					env.bIgnoreLoggerOnOffMsg = !cmd.hasOption("i");

					if (cmd.hasOption("o"))
						env.strDestDir = cmd.getOptionValue("o");
				}

				// Taking a file name
				{
					String [] opts = cmd.getArgs();

					if (opts.length != 1)
						throw new OptException("You should indicate a log file.");

					strFileName = opts[0];
				}

			} catch (OptException e) {
				System.err.println(e.getMessage());
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("ContextLogAnalyzer filename [Options]", options);

				return false;
			}

		} catch (ParseException e) {
			System.err.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ContextLogAnalyzer filename [Options]", options);

			return false;
		}

		return true;
	}

	static class OptException extends Exception {
		OptException(String s) { super(s); } 
	}


	public static class Environment
	{
		public String strDestDir = "";
		public boolean bUseHumanReadableTime = false;
		public boolean bIgnoreLoggerOnOffMsg = false;
	};

	public static Environment env = new Environment();
}
