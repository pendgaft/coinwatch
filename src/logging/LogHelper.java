package logging;

import java.util.*;

public class LogHelper {

	public static String formatMili(long timeDelta){
		if(timeDelta < 1000){
			return "" + timeDelta + " miliseconds";
		} else if (timeDelta < 60000){
			return "" + (double)timeDelta / 1000.0 + " seconds"; 
		} else if(timeDelta < 3600000){
			return "" + (double)timeDelta / 60000.0 + " minutes";
		} else{
			return "" + (double)timeDelta / 3600000.0 + " hours";
		}
	}
	
	public static List<String> buildDecendingList(HashMap<String, Double> values){
		List<String> retList = new ArrayList<String>(values.size());
		Set<String> notPlaced = new HashSet<String>();
		notPlaced.addAll(values.keySet());
		
		
		while(notPlaced.size() > 0){
			double largest = Double.NEGATIVE_INFINITY;
			String best = null;
			
			for(String tKey: notPlaced){
				if(values.get(tKey) > largest){
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
