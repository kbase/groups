package us.kbase.groups.service.api;

/** Paths to service endpoints.
 * @author gaprice@lbl.gov
 *
 */
public class ServicePaths {
	
	private static final String UPDATE = "update";
	private static final String GETPERM = "getperm";
	private static final String GROUP_STR = "group";

	/* general strings */

	/** The URL path separator. */
	public static final String SEP = "/";
	
	/* Root endpoint */

	/** The root endpoint location. */
	public static final String ROOT = SEP;
	
	/* Groups endpoints */
	
	/** The group endpoint location. */
	public static final String GROUP = SEP + GROUP_STR;
	/** The group ID */
	public static final String GROUP_ID = "{" + Fields.GROUP_ID + "}";
	/** The location to check if a group exists. */
	public static final String GROUP_EXISTS = GROUP_ID + SEP + "exists";
	/** The location to update the current user's last visited date. */
	public static final String GROUP_VISIT = GROUP_ID + SEP + "visit";
	/** The location to update a group. */
	public static final String GROUP_UPDATE = GROUP_ID + SEP + UPDATE;
	/** The location to request membership in a group. */
	public static final String GROUP_REQUEST_MEMBERSHIP = GROUP_ID + SEP + "requestmembership";
	/** the location to get requests targeted at a group. */
	public static final String GROUP_REQUESTS = GROUP_ID + SEP + "requests";
	/** The location to remove a user from a group. */
	public static final String GROUP_USER_ID = GROUP_ID + SEP + "user" + SEP + "{" +
			Fields.GROUP_MEMBER + "}";
	/** The location to update a user. */
	public static final String GROUP_USER_ID_UPDATE = GROUP_USER_ID + SEP + UPDATE;
	/** The location to promote or demote an administrator. */
	public static final String GROUP_USER_ID_ADMIN = GROUP_USER_ID + SEP + "admin";
	
	// resources
	private static final String GROUP_RESOURCE = SEP + "resource" + SEP ;
	/** The location to add or remove a resource from a group. */
	public static final String GROUP_RESOURCE_ID = GROUP_ID + GROUP_RESOURCE + "{" +
			Fields.GROUP_RESOURCE_TYPE + "}" + SEP + "{" + Fields.GROUP_RESOURCE_ID + "}";
	/** The location to get read permission for a group resource. */
	public static final String GROUP_RESOURCE_ID_PERMS = GROUP_RESOURCE_ID + SEP + GETPERM;

	
	/* Request endpoints */
	
	/** The request endpoint location. */
	public static final String REQUEST = SEP + "request";
	/** The location to access a request by ID. */
	public static final String REQUEST_ID = SEP + "id" + SEP + "{" + Fields.REQUEST_ID + "}";
	/** The location to get information about a group associated with a request. */
	public static final String REQUEST_ID_GROUP = REQUEST_ID + SEP + GROUP_STR;
	/** The location to request permissions to view an outside resource associated with a
	 * request. */
	public static final String REQUEST_ID_PERMS = REQUEST_ID + SEP + GETPERM;
	/** The location to cancel a request. */
	public static final String REQUEST_CANCEL = REQUEST_ID + SEP + "cancel";
	/** The location to accept a request. */
	public static final String REQUEST_ACCEPT = REQUEST_ID + SEP + "accept";
	/** The location to deny a request. */
	public static final String REQUEST_DENY = REQUEST_ID + SEP + "deny";
	/** The location to list requests created by the user. */
	public static final String REQUEST_CREATED = SEP + "created";
	/** The location to list requests targeted at the user. */
	public static final String REQUEST_TARGETED = SEP + "targeted";
	/** The location to determine whether groups have open requests. */
	public static final String REQUEST_NEW = SEP + "groups" + SEP + "{" + Fields.IDS + "}" +
			SEP + "new";
	
	/* Member endpoints */
	
	/** The member endpoint location. */
	public static final String MEMBER = SEP + "member";
	
	/* Names endpoints */
	
	/** The names endpoint location. */
	public static final String NAMES = SEP + "names";
	/** The bulk names endpoint location. */
	public static final String NAMES_BULK = SEP + "{" + Fields.IDS + "}";
}
