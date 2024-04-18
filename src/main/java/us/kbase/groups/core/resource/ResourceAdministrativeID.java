package us.kbase.groups.core.resource;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The immutable administrative ID used to determine if a user can administrate a resource.
 * In many cases the {@link ResourceID} and the administrative ID may be identical.
 * In some cases the administrative ID may be different, however, and so determining what
 * resources a user administrates must use the administrative ID for the resource rather that the
 * resource ID.
 * 
 * There must be a 1:1 or 1:M mapping from administrative ID to resource ID.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceAdministrativeID extends Name {
	
	/** Create the administrative resource ID.
	 * @param rid the ID.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 * @throws IllegalParameterException if the input contains control characters or is more
	 * than 256 characters.
	 */
	public ResourceAdministrativeID(final String rid)
			throws MissingParameterException, IllegalParameterException {
		super(rid, "administrative resource ID", 256);
	}
	
	/** Create the administrative resource ID from a long.
	 * @param rid the ID.
	 */
	public static ResourceAdministrativeID from(final long rid) {
		try {
			return new ResourceAdministrativeID(rid + "");
		} catch (MissingParameterException | IllegalParameterException e) {
			// max long chars is 20, soooo...
			throw new RuntimeException("This is impossible", e);
		}
	}
	
	/** Create the administrative resource ID from an int.
	 * @param rid the ID.
	 */
	public static ResourceAdministrativeID from(final int rid) {
		return from((long) rid);
	}


}
