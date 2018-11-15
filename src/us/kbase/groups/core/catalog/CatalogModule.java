package us.kbase.groups.core.catalog;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The name of a module in the KBase catalog service.
 * @author gaprice@lbl.gov
 *
 */
public class CatalogModule extends Name {
	
	/** Create the module name.
	 * @param module the module name.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 * @throws IllegalParameterException if the input contains control characters.
	 */
	public CatalogModule(final String module)
			throws MissingParameterException, IllegalParameterException {
		super(module, "catalog module", -1);
	}

}
