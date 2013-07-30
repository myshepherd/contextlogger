/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Logger Repository ::
 * Repository for collected logs from Logger app
 */

import org.apache.commons.cli.*; // For command line parsing

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class LoggerRepository implements Runnable {
    private static int PORT = 25800; // Default port

    public static void main(String[] args) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");

		if (parseOption(args) == false)
			return;

		System.out.println(
				"##################################################\n" +
				":: Logger Repository ::\n" +
				"Repository for collected logs from Logger app\n\n" +
				"Copyright CARES 2013\n" + 
				"##################################################"
				);

		System.out.println("Starting server ...");

        new Thread(new LoggerRepository()).start();
    }

    public void run() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

		System.out.println("<Server is started. PORT: " + Integer.toString(PORT) + ">\n");

        while (s != null) {
            try {
                Socket client = s.accept();
                new Thread(new LogDownloader(client)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	private static boolean parseOption(String [] args) {
		// Set options
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("port").hasArg().withDescription("[p]ort number").create("p"));

		// Parse Command
		try {
			CommandLine cmd = parser.parse(options, args);

			try {
				// Option p
				if (cmd.hasOption("p")) {
					String strPortNumber = cmd.getOptionValue("p");
					PORT = Integer.parseInt(strPortNumber);
				}

			} catch (Exception e) {
				System.err.println(e.getMessage());
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("LoggerRepository [Options]", options);

				return false;
			}

		} catch (ParseException e) {
			System.err.println("Unexpected exception: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("LoggerRepository [Options]", options);

			return false;
		}

		return true;
	}
}
