package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A group name. The only restrictions on a group name are that it may not include control
 * characters and must be no more than 256 Unicode code points.
 * @author gaprice@lbl.gov
 *
 */
public class GroupName extends Name {
	
	//TODO TEST
	
	/** Create a new name. The name will be {@link String#trim()}ed.
	 * @param name the name.
	 * @throws MissingParameterException if the name is null or whitespace only.
	 * @throws IllegalParameterException if the name is too long or contains illegal characters.
	 */
	public GroupName(final String name)
			throws MissingParameterException, IllegalParameterException {
		super(name, "group name", 256);
	}
}
