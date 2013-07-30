/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Log Downloader class for one client (device)
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class LogDownloader implements Runnable {
	// Buffer size for tcp/ip communication (Should be same as the value of Device's variable)
	private static final int BUFFER_SIZE = 8192;

	private Socket socket;
	private String strTimeStamp;

	private ClientInfo clientInfo;
	private ClientRepo clientRepo;

	LogDownloader(Socket s) {
		socket = s;
		strTimeStamp = (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date());
	}

	public void run() {
		try {
			byte[] bufDBFileHeader = new byte[BUFFER_SIZE];
			int nDBFileHeaderSize;

			System.out.println("= A client connected from " + socket.getInetAddress() + ".");

			// 1. Extract Client Info
			nDBFileHeaderSize = recvClientInfo(bufDBFileHeader);
			if (nDBFileHeaderSize == -1) {
				System.out.println("+ Fail to extract client info from " + socket.getInetAddress() + ".");
				return;
			}

			// 2. Download Log DB file
			if (!recvDBFile(bufDBFileHeader, nDBFileHeaderSize)) {
				System.out.println("+ Fail to download db file from " + socket.getInetAddress() + ".");
				return;
			}

			// 3. Resolve a downloaded log DB file to a master DB
			if (!clientRepo.resolveMasterDB(strTimeStamp)) {
				System.out.println("+ Fail to resolve the downloaded db file to the master db from " + socket.getInetAddress() + ".");
				return;
			}


			System.out.println("= Update Completes from " + socket.getInetAddress() + ".");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// The first chunk is texted client information.
	// Download it and make a context information file.
	// Return: remainedPacketData's size if success, -1 if fail
	private int recvClientInfo(byte[] remainedPacketData) throws IOException {
		byte[] firstChunk = new byte[BUFFER_SIZE];
		int nFirstChunkOffset = 0;
		int nRemainedPacketSize = -1;

		// 1. Download the first chunk
		{
			InputStream in = socket.getInputStream();
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = 0;
			while ((bytesRead = in.read(buffer)) > 0) {
				// Fill the first chunk
				int nCopySize = (nFirstChunkOffset + bytesRead < BUFFER_SIZE)? bytesRead : (BUFFER_SIZE - nFirstChunkOffset);
				System.arraycopy(buffer, 0, firstChunk, nFirstChunkOffset, nCopySize);
				nFirstChunkOffset += nCopySize;

				// If downloading the first chunk is completed,
				if (nFirstChunkOffset == BUFFER_SIZE) {
					// Copy remained packet data and break.
					nRemainedPacketSize = bytesRead - nCopySize;
					System.arraycopy(buffer, nCopySize, remainedPacketData, 0, nRemainedPacketSize);
					break;
				}
			}
		}

		// 2. Make a context information file
		{
			// Extract client information
			clientInfo = new ClientInfo(new String(firstChunk));
			if (clientInfo.getClientName() == null) {
				System.out.println("+ Invalid client information !!");
				return -1;
			}
			System.out.println("+ Collected logs sent by " + clientInfo.getClientName() + " [" + strTimeStamp + "]");

			// Write down client information
			clientRepo = new ClientRepo(clientInfo.getClientName());
			FileOutputStream outInfo = clientRepo.getInfoFileToWrite(strTimeStamp);
			if (outInfo == null)
				return -1;

			byte[] bytes = clientInfo.toString().getBytes();
			outInfo.write(bytes, 0, bytes.length);
			outInfo.close();
		}

		return nRemainedPacketSize;
	}

	// From second chunk, the logged db file is sent.
	// A part of start packet for the db file should be passed
	// at earlier step (i.e., extracting client info.)
	private boolean recvDBFile(byte[] bufDBFileHeader, int nDBFileHeaderSize) throws IOException {
		InputStream in = socket.getInputStream();

		FileOutputStream out = clientRepo.getDBFileToWrite(strTimeStamp);
		if (out == null)
			return false;

		out.write(bufDBFileHeader, 0, nDBFileHeaderSize);

		// continue downloading db file
		int nTotalBytes = nDBFileHeaderSize;
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		while ((bytesRead = in.read(buffer)) > 0) {
			if (out != null && bytesRead > 0) {
				out.write(buffer, 0, bytesRead);
				nTotalBytes += bytesRead;
			}
		}

		out.flush();
		out.close();

		System.out.println("+ The transfer from " + socket.getInetAddress() +
				" is finished. (" + nTotalBytes + " Bytes)");

		return true;
	}
}
