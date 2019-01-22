package us.kbase.groups.core.exceptions;

/** Thrown when the provided token is invalid.
 * @author gaprice@lbl.gov 
 */
@SuppressWarnings("serial")
public class InvalidTokenException extends AuthenticationException {
	
	public InvalidTokenException() {
		super(ErrorType.INVALID_TOKEN, null);
	}
	
	public InvalidTokenException(final String message) {
		super(ErrorType.INVALID_TOKEN, message);
	}
	
	public InvalidTokenException(final String message, final Throwable cause) {
		super(ErrorType.INVALID_TOKEN, message, cause);
	}
}
