package net;

import java.util.*;
import java.net.*;

public class DNSSeed {

	private String dnsName;
	/*
	 * TTLs
	 *   seed.bitcointstats.com - 60
	 *   bitseed.xf2.org - 1800 (have seen 1158, dynamic?)
	 *   seed.bitcoin.sipa.be - 60
	 *   dnsseed.bitcoin.dashjr.org - 60
	 */
	private static final String[] HOSTS = { "seed.bitcoinstats.com", "bitseed.xf2.org", 
			"seed.bitcoin.sipa.be", "dnsseed.bitcoin.dashjr.org" };

	public DNSSeed(String canonName) {
		this.dnsName = canonName;
	}

	// TODO handle exceptions cleanly?
	public InetAddress[] fetchNodes() throws UnknownHostException {
		return InetAddress.getAllByName(this.dnsName);
	}

	public String getCanonName() {
		return this.dnsName;
	}

	public static void main(String args[]) {
		int SAMPLESIZE = 10;
		
		DNSSeed selfs[] = new DNSSeed[DNSSeed.HOSTS.length];
		for (int counter = 0; counter < selfs.length; counter++) {
			selfs[counter] = new DNSSeed(DNSSeed.HOSTS[counter]);
		}

		HashMap<String, List<HashSet<InetAddress>>> responses = new HashMap<String, List<HashSet<InetAddress>>>();
		HashMap<String, List<Long>> timing = new HashMap<String, List<Long>>();
		for (String tCanonName : DNSSeed.HOSTS) {
			responses.put(tCanonName, new LinkedList<HashSet<InetAddress>>());
			timing.put(tCanonName, new LinkedList<Long>());
		}

		for (int counter = 0; counter < SAMPLESIZE; counter++) {
			for (DNSSeed tSelf : selfs) {
				try {
					long time = System.currentTimeMillis();
					InetAddress[] results = tSelf.fetchNodes();
					time = System.currentTimeMillis() - time;
					HashSet<InetAddress> tempSet = new HashSet<InetAddress>();
					for (InetAddress tAddy : results) {
						tempSet.add(tAddy);
					}
					responses.get(tSelf.getCanonName()).add(tempSet);
					timing.get(tSelf.getCanonName()).add(time);
				} catch (UnknownHostException e) {
					// do noting now, we'll note it later
				}
			}
			
			/*
			 * Sleep for 60 seconds, seems to be the ttl
			 */
			try {
				Thread.sleep(90000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		HashSet<InetAddress> totalSeen = new HashSet<InetAddress>();
		for(String canaonName: responses.keySet()){
			System.out.println("Stats for: " + canaonName);
			
			List<HashSet<InetAddress>> tempResponses = responses.get(canaonName);
			List<Long> tempTimings = timing.get(canaonName);
			
			System.out.println("Results sets seen: " + tempResponses.size());
			if(tempResponses.size() > 0){
				int sizesum = 0;
				long timeSum = 0;
				for(HashSet<InetAddress> tSet : tempResponses){
					sizesum += tSet.size();
				}
				for(Long tTime: tempTimings){
					timeSum += tTime;
				}
				System.out.println("Average responses: " + (double)sizesum/(double)tempResponses.size());
				System.out.println("Average response time: " + (double)timeSum/(double)tempTimings.size());
				
				HashSet<InetAddress> allResults = new HashSet<InetAddress>();
				for(HashSet<InetAddress> tSet: tempResponses){
					allResults.addAll(tSet);
					totalSeen.addAll(tSet);
				}
				System.out.println("Unique results: " + allResults.size());
			}
		}
		System.out.println("Total unique seen: " + totalSeen.size());
	}
}
