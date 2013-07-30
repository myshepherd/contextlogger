/*
 * Copyright CARES 2013
 * by shepherd
 * 
 * Client Info class including parser for texted client information  
 */

import java.util.*;

public class ClientInfo {
	public static final String CLIENTNAME = "ClientName";

	public ClientInfo(String textedInfo) {
		strTextedClientInfo = textedInfo;

		String [] infos = textedInfo.split("\n");
		for (String s : infos) {
			String [] item = s.split("\t");
			if (item.length != 2)
				continue;

			infomap.put(item[0], item[1]);
		}
	}

	public String getInfo(String strTag) {
		return infomap.get(strTag);
	}

	public String getClientName() {
		return getInfo(ClientInfo.CLIENTNAME);
	}

	public String toString() {
		return strTextedClientInfo;
	}

	private String strTextedClientInfo;
	private Map<String,String> infomap = new HashMap<String,String>(); 
}

