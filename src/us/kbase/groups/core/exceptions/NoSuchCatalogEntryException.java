package us.kbase.groups.core.exceptions;

/** Thrown when the specified catalog module or method does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchCatalogEntryException extends NoDataException {

	//TODO TEST
	
	public NoSuchCatalogEntryException(final String message) {
		super(ErrorType.NO_SUCH_CATALOG_ENTRY, message);
	}

	public NoSuchCatalogEntryException(final String message, final Throwable cause) {
		super(ErrorType.NO_SUCH_CATALOG_ENTRY, message, cause);
	}
}
