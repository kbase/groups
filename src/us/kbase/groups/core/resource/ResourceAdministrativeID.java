package us.kbase.groups.core.resource;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The adminstrative ID used to determine if a user can administrate a resource. In many cases
 * the {@link ResourceID} and the administrative ID may be identical. In some cases the
 * administrative ID may be different, however, and so determining what resources a user
 * administrates must use the administrative ID for the resource rather that the resource ID.
 * 
 * There must be a 1:1 or 1:M mapping from administrative ID to resource ID.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceAdministrativeID extends Name {
	
	/** Create the administrative resource ID.
	 * @param rid the ID.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 * @throws IllegalParameterException if the input contains control characters.
	 */
	public ResourceAdministrativeID(final String rid)
			throws MissingParameterException, IllegalParameterException {
		super(rid, "administrative resource ID", -1);
	}

}
