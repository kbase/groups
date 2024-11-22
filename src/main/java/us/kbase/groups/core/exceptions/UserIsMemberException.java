package us.kbase.groups.core.exceptions;

/** Thrown when user is already a member of a group.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class UserIsMemberException extends GroupsException {

	//TODO TEST
	
	public UserIsMemberException(final String message) {
		super(ErrorType.USER_IS_MEMBER, message);
	}

	public UserIsMemberException(
			final String message,
			final Throwable cause) {
		super(ErrorType.USER_IS_MEMBER, message, cause);
	}
}
