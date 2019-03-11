package us.kbase.groups.cataloghandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import us.kbase.catalog.BasicModuleInfo;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ListModuleParams;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientException;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.resource.ResourceAccess;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceInformationSet.Builder;

/** A handler implementation that uses a provided SDK workspace client to communicate with the 
 * catalog.
 * @author gaprice@lbl.gov
 *
 */
public class SDKClientCatalogHandler implements ResourceHandler {
	
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
	 * @throws ResourceHandlerException if an error occurs contacting the catalog.
	 */
	public SDKClientCatalogHandler(final CatalogClient client) throws ResourceHandlerException {
		checkNotNull(client, "client");
		this.client = client;
		try {
			client.version();
		} catch (IOException | JsonClientException e) {
			throw wrapGeneralCatalogException(e);
		}
	}

	private ResourceHandlerException wrapGeneralCatalogException(Exception e) {
		return new ResourceHandlerException("Error contacting catalog service at " +
				client.getURL(), e);
	}
	
	private static class ModMeth {
		private final String mod;
		private final String meth;
		
		private ModMeth(String mod, String meth) {
			this.mod = mod;
			this.meth = meth;
		}
	}
	
	private ModMeth getModMeth(final ResourceID resource) throws IllegalResourceIDException {
		final String[] split = resource.getName().split("\\.");
		if (split.length != 2) {
			throw new IllegalResourceIDException("Illegal catalog method name: " +
					resource.getName());
		}
		final String mod = split[0].trim();
		final String meth = split[1].trim();
		// meth cannot be empty at this point
		if (mod.isEmpty() || mod.length() + meth.length() + 1 != resource.getName().length()) {
			throw new IllegalResourceIDException("Illegal catalog method name: " +
					resource.getName());
		}
		return new ModMeth(mod, meth);
	}

	@Override
	public boolean isAdministrator(final ResourceID resource, final UserName user)
			throws ResourceHandlerException, NoSuchResourceException, IllegalResourceIDException {
		checkNotNull(resource, "resource");
		checkNotNull(user, "user");
		return getModuleOwners(resource).contains(user.getName());
	}

	private List<String> getModuleOwners(final ResourceID module)
			throws ResourceHandlerException, NoSuchResourceException, IllegalResourceIDException {
		final ModMeth modmeth = getModMeth(module);
		final ModuleInfo mod;
		try {
			mod = client.getModuleInfo(new SelectOneModuleParams()
					.withModuleName(modmeth.mod));
		} catch (IOException e) {
			throw wrapGeneralCatalogException(e);
		} catch (JsonClientException e) {
			if (e.getMessage().contains("module/repo is not registered")) {
				throw new NoSuchResourceException(module.getName(), e);
			} else {
				throw wrapGeneralCatalogException(e);
			}
		}
		if (mod.getRelease() == null) {
			throw new NoSuchResourceException(module.getName());
		}
		// wow this is some shit right here, the catalog spec is wrong
		// https://github.com/kbase/catalog/issues/100
		final Map<String, Object> addl = mod.getRelease().getAdditionalProperties();
		@SuppressWarnings("unchecked")
		final List<String> localMethods = (List<String>) addl.get("local_functions");
		@SuppressWarnings("unchecked")
		final List<String> narrMethods = (List<String>) addl.get("narrative_methods");
		
		if (!localMethods.contains(modmeth.meth) && !narrMethods.contains(modmeth.meth)) {
			throw new NoSuchResourceException(module.getName());
		}
		return mod.getOwners();
	}

	@Override
	public Set<ResourceAdministrativeID> getAdministratedResources(final UserName user)
			throws ResourceHandlerException {
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
		final Set<ResourceAdministrativeID> ret = new HashSet<>();
		for (final BasicModuleInfo m: mods) {
			try {
				ret.add(new ResourceAdministrativeID(m.getModuleName()));
			} catch (MissingParameterException | IllegalParameterException e) {
				throw new ResourceHandlerException(
						"Illegal module name returned from catalog: " + m.getModuleName());
			}
		}
		return ret;
	}

	@Override
	public Set<UserName> getAdministrators(final ResourceID resource)
			throws NoSuchResourceException, ResourceHandlerException, IllegalResourceIDException {
		checkNotNull(resource, "resource");
		final Set<UserName> users = new HashSet<>();
		for (final String u: getModuleOwners(resource)) {
			try {
				users.add(new UserName(u));
			} catch (IllegalParameterException | MissingParameterException e) {
				throw new ResourceHandlerException(
						"Illegal user name returned from catalog: " + u);
			}
		}
		return users;
	}

	@Override
	public ResourceDescriptor getDescriptor(final ResourceID resource)
			throws IllegalResourceIDException {
		checkNotNull(resource, "resource");
		final ModMeth m = getModMeth(resource);
		try {
			return new ResourceDescriptor(new ResourceAdministrativeID(m.mod), resource);
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("This should be impossible", e);
		}
	}

	@Override
	public ResourceInformationSet getResourceInformation(
			final UserName user,
			Set<ResourceID> resources,
			final ResourceAccess access)
			throws IllegalResourceIDException, ResourceHandlerException {
		checkNoNullsInCollection(resources, "resources");
		requireNonNull(access, "access");
		for (final ResourceID r: resources) {
			getModMeth(r); // check ids are valid before we do anything else
		}
		// check != so that if another level is added to ResourceAccess it doesn't
		// accidentally grant access
		if (!ResourceAccess.ALL.equals(access) &&
				!ResourceAccess.ADMINISTRATED_AND_PUBLIC.equals(access)) {
			if (user == null) {
				resources = Collections.emptySet();
			} else {
				resources = filterNonAdministrated(resources, user);
			}
		}
		final Builder b = ResourceInformationSet.getBuilder(user);
		resources.stream().forEach(r -> b.withResource(r));
		return b.build();
	}

	private Set<ResourceID> filterNonAdministrated(
			final Set<ResourceID> resources,
			final UserName user)
			throws ResourceHandlerException, IllegalResourceIDException {
		// there are no bulk methods for getting catalog methods, so just list all admin'd mods
		final Set<String> admined = getAdministratedResources(user).stream().map(r -> r.getName())
				.collect(Collectors.toSet());
		final Set<ResourceID> ret = new HashSet<>();
		for (final ResourceID r: resources) {
			if (admined.contains(getModMeth(r).mod)) {
				ret.add(r);
			}
		}
		return ret;
	}

	@Override
	public void setReadPermission(ResourceID resource, UserName user) {
		return; // nothing to do, catalog methods are all public
	}
}
