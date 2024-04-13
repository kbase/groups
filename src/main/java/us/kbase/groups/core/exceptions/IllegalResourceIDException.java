package us.kbase.groups.core.exceptions;

/** Thrown when a resource ID has an illegal value.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class IllegalResourceIDException extends GroupsException {

	//TODO TEST
	
	public IllegalResourceIDException(final String message) {
		super(ErrorType.ILLEGAL_RESOURCE_ID, message);
	}

	public IllegalResourceIDException(
			final String message,
			final Throwable cause) {
		super(ErrorType.ILLEGAL_RESOURCE_ID, message, cause);
	}
}
