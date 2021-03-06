package logging;

import java.util.regex.*;

public class RegexCollection {

	public static Pattern roundPattern = Pattern.compile("starting round (\\d+)");
	public static Pattern totalNodesKnownPattern = Pattern.compile("total nodes known (\\d+)");
	public static Pattern reachableNodesKnownPattern = Pattern.compile("Reachable nodes known (\\d+)");
	public static Pattern harvestIterationIndividualPattern = Pattern.compile("Took (\\d+) iterations.");
	public static Pattern harvestTotalWorkerThreadPattern = Pattern.compile("final size (\\d+)");
	public static Pattern harvestTotalParentThreadPattern = Pattern.compile("Harvested (\\d+)");

	public static Pattern totalConnFailurePattern = Pattern.compile("failed to conn (\\d+)");
	public static Pattern failureConnectionReason = Pattern.compile("failure (\\d+) : (.+)");
	
	public static Pattern connUASamplePattern = Pattern.compile("ua position (\\d+) (.+) with (\\d+)");

}
