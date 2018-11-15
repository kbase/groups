package us.kbase.groups.core.exceptions;

/** Thrown when a catalog method already exists in a group.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class CatalogMethodExistsException extends GroupsException {

	//TODO TEST
	
	public CatalogMethodExistsException(final String message) {
		super(ErrorType.CATALOG_METHOD_IN_GROUP, message);
	}

	public CatalogMethodExistsException(
			final String message,
			final Throwable cause) {
		super(ErrorType.CATALOG_METHOD_IN_GROUP, message, cause);
	}
}
