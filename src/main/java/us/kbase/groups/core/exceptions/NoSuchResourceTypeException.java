package us.kbase.groups.core.exceptions;

/** Thrown when a resource type does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchResourceTypeException extends GroupsException {

	//TODO TEST
	
	public NoSuchResourceTypeException(final String message) {
		super(ErrorType.NO_SUCH_RESOURCE_TYPE, message);
	}

	public NoSuchResourceTypeException(final String message, final Throwable cause) {
		super(ErrorType.NO_SUCH_RESOURCE_TYPE, message, cause);
	}
}
