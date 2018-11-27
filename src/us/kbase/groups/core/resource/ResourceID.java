package us.kbase.groups.core.resource;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** An immutable ID of a resource.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceID extends Name {
	
	/** Create the resource ID.
	 * @param rid the ID.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 * @throws IllegalParameterException if the input contains control characters.
	 */
	public ResourceID(final String rid)
			throws MissingParameterException, IllegalParameterException {
		super(rid, "resource ID", -1);
	}

}
