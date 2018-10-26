package us.kbase.groups.core.exceptions;

/** Thrown when the specified workspace does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchWorkspaceException extends NoDataException {

	//TODO TEST
	
	public NoSuchWorkspaceException(final String message) {
		super(ErrorType.NO_SUCH_WORKSPACE, message);
	}

	public NoSuchWorkspaceException(final String message, final Throwable cause) {
		super(ErrorType.NO_SUCH_WORKSPACE, message, cause);
	}
}
