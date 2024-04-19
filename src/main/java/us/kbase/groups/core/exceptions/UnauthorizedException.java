package us.kbase.groups.core.exceptions;

/** Base class of all exceptions caused by an authorization failure.
 * @author gaprice@lbl.gov 
 */
@SuppressWarnings("serial")
public class UnauthorizedException extends GroupsException {
	
	
	public UnauthorizedException() {
		super(ErrorType.UNAUTHORIZED, null);
	}
	
	public UnauthorizedException(final String message) {
		super(ErrorType.UNAUTHORIZED, message);
	}
	
	public UnauthorizedException(final ErrorType err) {
		super(err, null);
	}
	
	public UnauthorizedException(final ErrorType err, final String message) {
		super(err, message);
	}
}
