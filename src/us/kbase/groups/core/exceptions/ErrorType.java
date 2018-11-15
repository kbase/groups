package us.kbase.groups.core.exceptions;

/** An enum representing the type of a particular error.
 * @author gaprice@lbl.gov
 *
 */
public enum ErrorType {
	
	/** The authentication service returned an error. */
	AUTHENTICATION_FAILED	(10000, "Authentication failed"),
	/** No token was provided when required */
	NO_TOKEN				(10010, "No authentication token"),
	/** The token provided is not valid. */
	INVALID_TOKEN			(10020, "Invalid token"),
	/** The user is not authorized to perform the requested action. */
	UNAUTHORIZED			(20000, "Unauthorized"),
	/** A required input parameter was not provided. */
	MISSING_PARAMETER		(30000, "Missing input parameter"),
	/** An input parameter had an illegal value. */
	ILLEGAL_PARAMETER		(30001, "Illegal input parameter"),
	/** The provided user name was not legal. */
	ILLEGAL_USER_NAME		(30010, "Illegal user name"),
	/** The provided group id was not legal. */
	ILLEGAL_GROUP_ID		(30020, "Illegal group ID"),
	/** The group could not be created because it already exists. */
	GROUP_ALREADY_EXISTS	(40000, "Group already exists"),
	/** The request could not be created because it already exists. */
	REQUEST_ALREADY_EXISTS	(40010, "Request already exists"),
	/** The user is already a member of the group. */
	USER_IS_MEMBER			(40020, "User already group member"),
	/** The workspace is already a member of the group. */
	WORKSPACE_IN_GROUP		(40030, "Workspace already in group"),
	/** The catalog method is already a member of the group. */
	CATALOG_METHOD_IN_GROUP	(40040, "Catalog method already in group"),
	/** The requested group does not exist. */
	NO_SUCH_GROUP			(50000, "No such group"),
	/** The requested request does not exist. */
	NO_SUCH_REQUEST			(50010, "No such request"),
	/** The requested user does not exist. */
	NO_SUCH_USER			(50020, "No such user"),
	/** The custom field does not exist. */
	NO_SUCH_CUSTOM_FIELD	(50030, "No such custom field"),
	/** The workspace does not exist. */
	NO_SUCH_WORKSPACE		(50040, "No such workspace"),
	/** The catalog module or method does not exist. */
	NO_SUCH_CATALOG_ENTRY	(50050, "No such catalog entry"),
	/** The request is closed. */
	REQUEST_CLOSED			(60000, "Request closed"),
	/** The requested operation is not supported. */
	UNSUPPORTED_OP			(70000, "Unsupported operation");
	
	private final int errcode;
	private final String error;
	
	private ErrorType(final int errcode, final String error) {
		this.errcode = errcode;
		this.error = error;
	}

	/** Get the error code for the error type.
	 * @return the error code.
	 */
	public int getErrorCode() {
		return errcode;
	}

	/** Get a text description of the error type.
	 * @return the error.
	 */
	public String getError() {
		return error;
	}

}
