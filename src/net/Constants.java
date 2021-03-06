package net;

import java.net.InetAddress;
import java.util.Random;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Constants {

	public static final int DEFAULT_PORT = 8333;

	public static final int CONNECT_TIMEOUT = 10000;
	public static final TimeUnit CONNECT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
	public static final long TRANSACTION_TIMEOUT = 15000;
	public static final TimeUnit TRANSACTION_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
	public static final boolean STRICT_TIMEOUTS = true;

	public static final int MAX_ADDR_ADV = 1000;

	public static final String VERSION_CMD = "version";
	public static final String VERACK_CMD = "verack";
	public static final String GETADDR_CMD = "getaddr";
	public static final String ADDR_CMD = "addr";
	public static final String PING_CMD = "ping";
	public static final String PONG_CMD = "pong";
	public static final String REJECT_CMD = "reject";
	
	public static final String PING_TX = "ping";
	public static final String GETADDR_TX = "getaddr";
	public static final String CONNECT_TX = "connect";
	public static final String NONE_TX = "none";

	public static final String HARVEST_LOG = "experiment.harvestlog";
	public static final String PASSIVE_MON_LOG = "passiveMonitor.passiveclient";
	
	public static final String LOG_DIR = "logs/";

	public static final long MAX_UNSIGNED_INT = (long)(Math.pow(2, 32) - 1);
	
	public static InetAddress SrcIP = null;
	public static long LAST_BLOCK = 0;
	public static Random NON_SEC_RNG = new Random();

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
