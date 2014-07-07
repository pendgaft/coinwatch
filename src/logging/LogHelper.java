package logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.Constants;

public class LogHelper {

	public static void initLogger() {
		/*
		 * Build handlers
		 */
		FileHandler logHandler = null;
		FileHandler summaryHandler = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
		String tsString = df.format(new Date());
		try {

			logHandler = new FileHandler(Constants.LOG_DIR + "harvest-" + tsString + ".out");
			summaryHandler = new FileHandler(Constants.LOG_DIR + "summary-" + tsString + ".out");
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logHandler.setLevel(Level.CONFIG);
		logHandler.setFormatter(new SimpleFormatter());
		summaryHandler.setLevel(Level.INFO);
		summaryHandler.setFormatter(new SimpleFormatter());
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setLevel(Level.WARNING);

		/*
		 * Get logger, add handlers
		 */
		Logger expLogger = Logger.getLogger(Constants.HARVEST_LOG);
		expLogger.setUseParentHandlers(false);
		expLogger.setLevel(Level.FINE);
		expLogger.addHandler(logHandler);
		expLogger.addHandler(conHandler);
		expLogger.addHandler(summaryHandler);
	}

	public static String formatMili(long timeDelta) {
		if (timeDelta < 1000) {
			return "" + timeDelta + " miliseconds";
		} else if (timeDelta < 60000) {
			return "" + (double) timeDelta / 1000.0 + " seconds";
		} else if (timeDelta < 3600000) {
			return "" + (double) timeDelta / 60000.0 + " minutes";
		} else {
			return "" + (double) timeDelta / 3600000.0 + " hours";
		}
	}

	public static List<String> buildDecendingList(HashMap<String, Double> values) {
		List<String> retList = new ArrayList<String>(values.size());
		Set<String> notPlaced = new HashSet<String>();
		notPlaced.addAll(values.keySet());

		while (notPlaced.size() > 0) {
			double largest = Double.NEGATIVE_INFINITY;
			String best = null;

			for (String tKey : notPlaced) {
				if (values.get(tKey) > largest) {
					largest = values.get(tKey);
					best = tKey;
				}
			}

			notPlaced.remove(best);
			retList.add(best);
		}

		return retList;
	}

}
