package us.kbase.groups.service;

/** Field names used in incoming and outgoing data structures.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {

	/* root */
	
	/** The server name. */
	public static final String SERVER_NAME = "servname";
	/** The version of the service. */
	public static final String VERSION = "version";
	/** The time, in milliseconds since the epoch, at the service. */
	public static final String SERVER_TIME = "servertime";
	/** The Git commit from which the service was built. */
	public static final String GIT_HASH = "gitcommithash";
	
	/* errors */
	
	/** An error. */
	public static final String ERROR = "error";
	
	
}
