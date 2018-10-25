package us.kbase.groups.service.api;

public class Fields {
	
	/* ***********************
	 * groups fields
	 * ***********************
	 */
	
	/** The group id. */
	public static final String GROUP_ID = "id";
	/** The group name. */
	public static final String GROUP_NAME = "name";
	/** The group type. */
	public static final String GROUP_TYPE = "type";
	/** The username of the group owner. */
	public static final String GROUP_OWNER = "owner";
	/** The group administrators. */
	public static final String GROUP_ADMINS = "admins";
	/** A group member. */
	public static final String GROUP_MEMBER = "member";
	/** The group members. */
	public static final String GROUP_MEMBERS = "members";
	/** The group creation date. */
	public static final String GROUP_CREATION = "createdate";
	/** The group modification date. */
	public static final String GROUP_MODIFICATION = "moddate";
	/** The group description. */
	public static final String GROUP_DESCRIPTION = "description";

	/* ***********************
	 * request fields
	 * ***********************
	 */
	
	/** The request ID */
	public static final String REQUEST_ID = "id";
	/** The group ID for the request */
	public static final String REQUEST_GROUP_ID = "groupid";
	/** The user the request targets, if any. */
	public static final String REQUEST_TARGET = "targetuser";
	/** The user that made the request. */
	public static final String REQUEST_REQUESTER = "requester";
	/** The type of the request. */
	public static final String REQUEST_TYPE = "type";
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
}
