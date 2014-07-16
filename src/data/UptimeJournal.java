package data;

import java.util.*;
import java.io.*;

public class UptimeJournal {

	private List<UptimeEntry> entries;

	public UptimeJournal() {
		this.entries = new LinkedList<UptimeEntry>();
	}

	public boolean nodeUp() {
		return this.entries.size() > 0 && !this.entries.get(this.entries.size() - 1).isEntryClosed();
	}

	public void addOfflineEvent(long timeStamp) {
		if (this.entries.size() == 0) {
			throw new RuntimeException("Offline event without an online event!");
		}

		UptimeEntry tail = this.entries.get(this.entries.size() - 1);
		if (tail.isEntryClosed()) {
			throw new RuntimeException("Offline event when already closed!");
		}

		tail.setOfflineTime(timeStamp);
	}

	public void addOnlineEvent(long timeStamp) {
		if (this.nodeUp()) {
			throw new RuntimeException("Online event while already online!");
		}

		this.entries.add(new UptimeEntry(timeStamp));
	}

	public void printJournalToFile(BufferedWriter outBuff) throws IOException {
		for (UptimeEntry tEntry : this.entries) {
			outBuff.write("\t" + tEntry.toString() + "\n");
		}
	}

}
