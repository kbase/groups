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
	/** The user is already a member of the group. */
	USER_IS_MEMBER			(40010, "User already group member"),
	/** The request could not be created because it already exists. */
	REQUEST_ALREADY_EXISTS	(40020, "Request already exists"),
	/** The requested group does not exist. */
	NO_SUCH_GROUP			(50000, "No such group"),
	/** The requested request does not exist. */
	NO_SUCH_REQUEST			(50010, "No such request"),
	/** The requested user does not exist. */
	NO_SUCH_USER			(50020, "No such user"),
	/** The requested operation is not supported. */
	UNSUPPORTED_OP			(60000, "Unsupported operation");
	
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
