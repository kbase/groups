package us.kbase.groups.core.exceptions;

/** An exception thrown when a workspace handler encounters an unexpected error.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class WorkspaceHandlerException extends Exception {
	
	public WorkspaceHandlerException(final String message) {
		super(message);
	}
	
	public WorkspaceHandlerException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
