package net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Constants {

	public static final int DEFAULT_PORT = 8333;

	public static final int CONNECT_TIMEOUT = 5000;
	public static final long TRANSACTION_TIMEOUT = 5000;
	public static final TimeUnit TRANSACTION_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

	public static final int MAX_ADDR_ADV = 1000;

	public static final String VERSION_CMD = "version";
	public static final String VERACK_CMD = "verack";
	public static final String GETADDR_CMD = "getaddr";
	public static final String ADDR_CMD = "addr";

	public static final String HARVEST_LOG = "experiment.harvestlog";

	public static InetAddress SrcIP = null;

	public static void initConstants(){
		if(Constants.SrcIP == null){
			try {
				Constants.SrcIP = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}
}
