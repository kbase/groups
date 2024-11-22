package us.kbase.groups.core.exceptions;

/** Thrown when a request already exists.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class RequestExistsException extends GroupsException {

	//TODO TEST
	
	public RequestExistsException(final String message) {
		super(ErrorType.REQUEST_ALREADY_EXISTS, message);
	}

	public RequestExistsException(
			final String message,
			final Throwable cause) {
		super(ErrorType.REQUEST_ALREADY_EXISTS, message, cause);
	}
}
