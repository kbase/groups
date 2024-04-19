package us.kbase.groups.core.exceptions;

/** Thrown when a group already exists.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class GroupExistsException extends GroupsException {

	//TODO TEST
	
	public GroupExistsException(final String message) {
		super(ErrorType.GROUP_ALREADY_EXISTS, message);
	}

	public GroupExistsException(
			final String message,
			final Throwable cause) {
		super(ErrorType.GROUP_ALREADY_EXISTS, message, cause);
	}
}
