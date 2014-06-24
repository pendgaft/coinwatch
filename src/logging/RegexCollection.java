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
	public static Pattern failedViaConnTimePattern = Pattern.compile("failed via conn timeout (\\d+)");
	public static Pattern failedViaHandTimePattern = Pattern.compile("failed via handshake timeout (\\d+)");
	public static Pattern failedViaIOErrorPattern = Pattern.compile("failed via other io (\\d+)");
	public static Pattern failedViaIncomingIOErrorPattern = Pattern.compile("failed via incoming io (\\d+)");

	public static Pattern connUASamplePattern = Pattern.compile("ua position (\\d+) (.+) with (\\d+)");

}
