package us.kbase.groups.core.catalog;

import java.util.Set;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.CatalogHandlerException;
import us.kbase.groups.core.exceptions.NoSuchCatalogEntryException;

/** Handles requests for information from the KBase Catalog service.
 * @author gaprice@lbl.gov
 *
 */
public interface CatalogHandler {

	/** Determine whether a user is an owner of a catalog module.
	 * @param method the catalog method.
	 * @param user the user.
	 * @return true if the user is a method owner, false otherwise.
	 * @throws CatalogHandlerException if an error occurs contacting the catalog.
	 * @throws NoSuchCatalogEntryException if there is no catalog module with the given name.
	 */
	boolean isAdministrator(CatalogModule module, UserName user)
			throws CatalogHandlerException, NoSuchCatalogEntryException;

	/** Determine whether a catalog method exists.
	 * @param method the method to check.
	 * @return true if the method exists, false otherwise.
	 * @throws CatalogHandlerException if an error occurs contacting the catalog.
	 */
	boolean isMethodExtant(CatalogMethod method) throws CatalogHandlerException;
	
	/** Get the catalog modules a user owns.
	 * @param user the user.
	 * @return the modules.
	 * @throws CatalogHandlerException if an error occurs contacting the catalog.
	 */
	Set<CatalogModule> getOwnedModules(UserName user)
		throws CatalogHandlerException;

	/** Get the set of users that own a catalog module.
	 * @param module the module to query.
	 * @return the set of owners.
	 * @throws CatalogHandlerException if an error occurs contacting the catalog.
	 * @throws NoSuchCatalogEntryException if there is no catalog module with the given name.
	 */
	Set<UserName> getOwners(CatalogModule module)
			throws NoSuchCatalogEntryException, CatalogHandlerException;
}
