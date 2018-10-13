package us.kbase.groups.service.api;

/** Paths to service endpoints.
 * @author gaprice@lbl.gov
 *
 */
public class ServicePaths {

	/* general strings */

	/** The URL path separator. */
	public static final String SEP = "/";
	
	/* Root endpoint */

	/** The root endpoint location. */
	public static final String ROOT = SEP;
	
	/* Groups endpoints */
	
	/** The group endpoint location. */
	public static final String GROUP = SEP + "group";
	/** The group ID */
	public static final String GROUP_ID = "{" + Fields.GROUP_ID + "}";
	public static final String GROUP_REQUEST_MEMBERSHIP = GROUP_ID + SEP + "requestmembership";
	
	/* Request endpoints */
	
	/** The request endpoint location. */
	public static final String REQUEST = SEP + "request";
	/** The location to access a request by ID */
	public static final String REQUEST_ID = "/id/{" + Fields.REQUEST_ID + "}";
	
	
}
