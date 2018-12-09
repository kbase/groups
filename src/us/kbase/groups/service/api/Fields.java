package us.kbase.groups.service.api;

public class Fields {
	
	/* ***********************
	 * groups fields
	 * ***********************
	 */
	
	/** The group ID. */
	public static final String GROUP_ID = "id";
	/** The group name. */
	public static final String GROUP_NAME = "name";
	/** The username of the group owner. */
	public static final String GROUP_OWNER = "owner";
	/** The group administrators. */
	public static final String GROUP_ADMINS = "admins";
	/** A group member. */
	public static final String GROUP_MEMBER = "member";
	/** The group members. */
	public static final String GROUP_MEMBERS = "members";
	/** The group resources. */
	public static final String GROUP_RESOURCES = "resources";
	/** The group creation date. */
	public static final String GROUP_CREATION = "createdate";
	/** The group modification date. */
	public static final String GROUP_MODIFICATION = "moddate";
	/** Custom fields associated with the group. */
	public static final String GROUP_CUSTOM_FIELDS = "custom";
	
	// members fields
	/** A group member name. */
	public static final String GROUP_MEMBER_NAME = "name";
	/** Custom fields for a group member. */
	public static final String GROUP_MEMBER_CUSTOM_FIELDS = "custom";
	/** A member's join date. */
	public static final String GROUP_MEMBER_JOIN_DATE = "joined";
	
	// resource fields
	/** The resource type. */
	public static final String GROUP_RESOURCE_TYPE = "resourcetype";
	/** The resource ID. */
	public static final String GROUP_RESOURCE_ID = "rid";
	
	/** Whether an action is complete or not, and therefore whether a request object is
	 * included in the response.
	 */
	public static final String GROUP_COMPLETE = "complete";

	/* ***********************
	 * request fields
	 * ***********************
	 */
	
	/** The request ID */
	public static final String REQUEST_ID = "id";
	/** The group ID for the request */
	public static final String REQUEST_GROUP_ID = "groupid";
	/** The user that made the request. */
	public static final String REQUEST_REQUESTER = "requester";
	/** The type of the request. */
	public static final String REQUEST_TYPE = "type";
	/** The type of the resource targeted by the request. */
	public static final String REQUEST_RESOURCE_TYPE = "resourcetype";
	/** The resource targeted by the request. */
	public static final String REQUEST_RESOURCE = "resource";
	/** The the status of the request. */
	public static final String REQUEST_STATUS = "status";
	/** The creation date of the request. */
	public static final String REQUEST_CREATION = "createdate";
	/** The modification date of the request. */
	public static final String REQUEST_MODIFICATION = "moddate";
	/** The expiration date of the request. */
	public static final String REQUEST_EXPIRATION = "expiredate";
	/** The permitted actions for the user. */
	public static final String REQUEST_USER_ACTIONS = "actions";
	/** The reason a request was denied. */
	public static final String REQUEST_DENIED_REASON = "reason";
	
	/* ***********************
	 * groups listing fields
	 * ***********************
	 */
	
	/** Exclude any groups where the sort key is before or after this key, exclusive,
	 * depending on the sort order.
	 */
	public static final String GET_GROUPS_EXCLUDE_UP_TO = "excludeupto";
	/** Set the sort order. */
	public static final String GET_GROUPS_SORT_ORDER = "order";
	
	/* ***********************
	 * request listing fields
	 * ***********************
	 */
	
	/** Exclude any requests modified before or after this date, exclusive, depending on the
	 * sort order.
	 */
	public static final String GET_REQUESTS_EXCLUDE_UP_TO = "excludeupto";
	/** Include closed requests. */
	public static final String GET_REQUESTS_INCLUDE_CLOSED = "closed";
	/** Set the sort order. */
	public static final String GET_REQUESTS_SORT_ORDER = "order";
	
	/* ***********************
	 * other fields
	 * ***********************
	 */
	
	/** Whether something exists or not. */
	public static final String EXISTS = "exists";
}
