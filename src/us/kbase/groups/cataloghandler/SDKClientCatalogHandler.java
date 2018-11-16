package us.kbase.groups.cataloghandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import us.kbase.catalog.BasicModuleInfo;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ListModuleParams;
import us.kbase.catalog.ModuleVersion;
import us.kbase.catalog.SelectModuleVersion;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientException;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.catalog.CatalogHandler;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.catalog.CatalogModule;
import us.kbase.groups.core.exceptions.CatalogHandlerException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCatalogEntryException;

/** A handler implementation that uses a provided SDK workspace client to communicate with the 
 * catalog.
 * @author gaprice@lbl.gov
 *
 */
public class SDKClientCatalogHandler implements CatalogHandler {
	
	// TODO CACHE may help to cache all or some of the results. YAGNI for now.
	// TODO TEST integration tests, general. Not sure how painful it's gonna be to run the catalog in tests. Docker?
	
	/* may want to change behavior wrt disabled modules (see below). Currently disabled modules
	 * are treated like any other module.
	 * fns to enable / disable are set_active/inactive.
	 * get_module_state may be useful.
	 */

	private final CatalogClient client;

	/** Create the handler.
	 * @param client the catalog client to use to communicate with the catalog. No token is
	 * necessary.
	 * @throws CatalogHandlerException if an error occurs contacting the catalog.
	 */
	public SDKClientCatalogHandler(final CatalogClient client) throws CatalogHandlerException {
		checkNotNull(client, "client");
		this.client = client;
		try {
			client.version();
		} catch (IOException | JsonClientException e) {
			throw wrapGeneralCatalogException(e);
		}
	}

	private CatalogHandlerException wrapGeneralCatalogException(Exception e) {
		return new CatalogHandlerException("Error contacting catalog service at " +
				client.getURL(), e);
	}
	
	@Override
	public boolean isOwner(final CatalogModule module, final UserName user)
			throws CatalogHandlerException, NoSuchCatalogEntryException {
		checkNotNull(module, "module");
		checkNotNull(user, "user");
		return getModuleOwners(module).contains(user.getName());
	}

	private List<String> getModuleOwners(final CatalogModule module)
			throws CatalogHandlerException, NoSuchCatalogEntryException {
		try {
			return client.getModuleInfo(new SelectOneModuleParams()
					.withModuleName(module.getName())).getOwners();
		} catch (IOException e) {
			throw wrapGeneralCatalogException(e);
		} catch (JsonClientException e) {
			if (e.getMessage().contains("module/repo is not registered")) {
				throw new NoSuchCatalogEntryException(module.getName(), e);
			} else {
				throw wrapGeneralCatalogException(e);
			}
		}
	}

	@Override
	public boolean isMethodExtant(final CatalogMethod method) throws CatalogHandlerException {
		checkNotNull(method, "method");
		final ModuleVersion modver;
		try {
			modver = client.getModuleVersion(new SelectModuleVersion()
					.withModuleName(method.getModule().getName())
					//TODO TEST need an integration test for this where beta has method but release doesn't
					.withVersion("release"));
		} catch (IOException e) {
			throw wrapGeneralCatalogException(e);
		} catch (JsonClientException e) {
			if (e.getMessage().contains("Module cannot be found")) {
				return false;  // <----- NOTE VERY NAUGHTY RETURN RIGHT HERE
			} else {
				throw wrapGeneralCatalogException(e);
			}
		}
		// wow this is some shit right here, the catalog spec is wrong
		// https://github.com/kbase/catalog/issues/100
		final Map<String, Object> addl = modver.getAdditionalProperties();
		@SuppressWarnings("unchecked")
		final List<String> localMethods = (List<String>) addl.get("local_functions");
		@SuppressWarnings("unchecked")
		final List<String> narrMethods = (List<String>) addl.get("narrative_methods");
		
		return localMethods.contains(method.getMethod()) ||
				narrMethods.contains(method.getMethod());
	}

	@Override
	public Set<CatalogModule> getOwnedModules(final UserName user) throws CatalogHandlerException {
		checkNotNull(user, "user");
		final List<BasicModuleInfo> mods;
		try {
			mods = client.listBasicModuleInfo(new ListModuleParams()
					.withOwners(Arrays.asList(user.getName()))
					// may want to disallow disabled later, but all the other methods ignore that
					// TODO TEST integration test for this, ensure disabled modules show up
					.withIncludeDisabled(1L));
		} catch (IOException | JsonClientException e) {
			throw wrapGeneralCatalogException(e);
		}
		final Set<CatalogModule> ret = new HashSet<>();
		for (final BasicModuleInfo m: mods) {
			try {
				ret.add(new CatalogModule(m.getModuleName()));
			} catch (MissingParameterException | IllegalParameterException e) {
				throw new CatalogHandlerException(
						"Illegal module name returned from catalog: " + m.getModuleName());
			}
		}
		return ret;
	}

	@Override
	public Set<UserName> getOwners(final CatalogModule module)
			throws NoSuchCatalogEntryException, CatalogHandlerException {
		checkNotNull(module, "module");
		final Set<UserName> users = new HashSet<>();
		for (final String u: getModuleOwners(module)) {
			try {
				users.add(new UserName(u));
			} catch (IllegalParameterException | MissingParameterException e) {
				throw new CatalogHandlerException("Illegal user name returned from catalog: " + u);
			}
		}
		return users;
	}

}
