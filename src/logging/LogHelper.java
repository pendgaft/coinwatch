package logging;

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
	
}
