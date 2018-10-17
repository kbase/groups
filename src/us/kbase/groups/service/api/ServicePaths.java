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
	
}
