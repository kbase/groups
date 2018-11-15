package us.kbase.groups.core.exceptions;

/** An exception thrown when a catalog handler encounters an unexpected error.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class CatalogHandlerException extends Exception {
	
	public CatalogHandlerException(final String message) {
		super(message);
	}
	
	public CatalogHandlerException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
