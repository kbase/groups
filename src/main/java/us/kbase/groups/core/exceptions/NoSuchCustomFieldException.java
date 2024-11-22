package us.kbase.groups.core.exceptions;

/** Thrown when the specified custom field does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchCustomFieldException extends GroupsException {

	//TODO TEST
	
	public NoSuchCustomFieldException(final String message) {
		super(ErrorType.NO_SUCH_CUSTOM_FIELD, message);
	}

	public NoSuchCustomFieldException(final String message, final Throwable cause) {
		super(ErrorType.NO_SUCH_CUSTOM_FIELD, message, cause);
	}
}
