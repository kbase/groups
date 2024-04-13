package us.kbase.groups.core.exceptions;

/** Thrown when a resource already exists in a group.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class ResourceExistsException extends GroupsException {

	//TODO TEST
	
	public ResourceExistsException(final String message) {
		super(ErrorType.RESOURCE_IN_GROUP, message);
	}

	public ResourceExistsException(
			final String message,
			final Throwable cause) {
		super(ErrorType.RESOURCE_IN_GROUP, message, cause);
	}
}
