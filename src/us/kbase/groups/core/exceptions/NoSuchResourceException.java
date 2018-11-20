package us.kbase.groups.core.exceptions;

/** Thrown when the specified resource does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchResourceException extends NoDataException {

	//TODO TEST
	
	public NoSuchResourceException(final String message) {
		super(ErrorType.NO_SUCH_RESOURCE, message);
	}

	public NoSuchResourceException(final String message, final Throwable cause) {
		super(ErrorType.NO_SUCH_RESOURCE, message, cause);
	}
}
