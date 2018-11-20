package us.kbase.groups.core.exceptions;

/** An exception thrown when a resource handler encounters an unexpected error.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class ResourceHandlerException extends Exception {
	
	public ResourceHandlerException(final String message) {
		super(message);
	}
	
	public ResourceHandlerException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
