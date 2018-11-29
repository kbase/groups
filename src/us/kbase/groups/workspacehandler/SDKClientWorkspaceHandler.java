package us.kbase.groups.workspacehandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;

import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple9;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.workspace.GetPermissionsMassParams;
import us.kbase.workspace.ListWorkspaceIDsParams;
import us.kbase.workspace.ListWorkspaceIDsResults;
import us.kbase.workspace.SetPermissionsParams;
import us.kbase.workspace.WorkspaceClient;
import us.kbase.workspace.WorkspaceIdentity;

/** A handler implementation that uses a provided SDK workspace client to communicate with the 
 * workspace.
 * @author gaprice@lbl.gov
 *
 */
public class SDKClientWorkspaceHandler implements ResourceHandler {
	
	// TODO CACHE may help to cache all or some of the results. YAGNI for now.

	private static final String PERM_ADMIN = "a";
	private static final String PERM_WRITE = "w";
	private static final String PERM_READ = "r";
	private static final String GLOBAL_READ_USER = "*";
	
	private final WorkspaceClient client;
	
	/** Create the handler.
	 * @param client the workspace client to use to communicate with the workspace. The
	 * client must be initialized with a token with administrative write privileges.
	 * @throws ResourceHandlerException if an error occurs contacting the workspace or
	 * the workspace version is less than 0.8.0.
	 */
	public SDKClientWorkspaceHandler(final WorkspaceClient client)
			throws ResourceHandlerException {
		checkNotNull(client, "client");
		this.client = client;
		final String ver;
		try {
			ver = client.ver();
			// ensure the client has admin creds
			// don't think there's a way to safely ensure write creds
			client.administer(new UObject(ImmutableMap.of("command", "listAdmins")));
		} catch (IOException | JsonClientException e) {
			throw getGeneralWSException(e);
		}
		
		if (Version.valueOf(ver).lessThan(Version.forIntegers(0, 8))) {
			throw new ResourceHandlerException("Workspace version 0.8.0 or greater is required");
		}
	}

	private ResourceHandlerException getGeneralWSException(final Exception e) {
		return new ResourceHandlerException(String.format(
				"Error contacting workspace at %s", client.getURL()), e);
	}

	private long getWSID(final ResourceID id) throws IllegalResourceIDException {
		try {
			return Long.parseLong(id.getName());
		} catch (NumberFormatException e) {
			throw new IllegalResourceIDException(id.getName());
		}
	}
	
	@Override
	public boolean isAdministrator(final ResourceID resource, final UserName user)
			throws NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException {
		checkNotNull(resource, "resource");
		checkNotNull(user, "user");
		final Perms perms = getPermissions(Arrays.asList(getWSID(resource)), true);
		return new Perm(user, perms.perms.get(0)).perm.isAdmin();
	}
	
	private final TypeReference<Map<String, List<Map<String, String>>>> TR_GET_PERMS =
			new TypeReference<Map<String, List<Map<String, String>>>>() {};

	private static class Perm {
		
		private final WorkspacePermission perm;
		private final boolean isPublic;
		
		private Perm(final UserName user, final Map<String, String> perms) {
			final String perm = user == null ? null : perms.get(user.getName());
			this.perm = perm == null ? WorkspacePermission.NONE :
				WorkspacePermission.fromWorkspaceRepresentation(perm);
			this.isPublic = PERM_READ.equals(perms.get(GLOBAL_READ_USER));
		}
	}

	private static class Perms {
		@SuppressWarnings("unused")
		private final Integer errorWSID;
		private final List<Map<String, String>> perms;
		
		private Perms(final int errorWSID) {
			this.errorWSID = errorWSID;
			this.perms = null;
		}
		
		private Perms(final List<Map<String, String>> perms) {
			this.errorWSID = null;
			this.perms = perms;
		}
	}
			
	private Perms getPermissions(final List<Long> ids, final boolean throwNoWorkspaceException)
			throws NoSuchResourceException, ResourceHandlerException {
		final List<Map<String, String>> perms;
		try {
			perms = client.administer(new UObject(ImmutableMap.of(
					"command", "getPermissionsMass",
					"params", new GetPermissionsMassParams().withWorkspaces(
							ids.stream().map(id -> new WorkspaceIdentity().withId(id))
									.collect(Collectors.toList())))))
					.asClassInstance(TR_GET_PERMS).get("perms");
		} catch (ServerException e) {
			final Integer errorid = getWorkspaceID(e);
			if (errorid != null) {
				if (throwNoWorkspaceException) {
					throw new NoSuchResourceException(errorid + "", e);
				} else {
					return new Perms(errorid);
				}
			} else {
				throw getGeneralWSException(e);
			}
		} catch (IOException | JsonClientException | IllegalStateException e) {
			throw getGeneralWSException(e);
		}
		return new Perms(perms);
	}
	
	private static final Pattern WSERR = Pattern.compile(
			"(?:Workspace (\\d+) is deleted|No workspace with id (\\d+) exists)");
	
	// returns null if error is not a deleted or missing workspace
	private Integer getWorkspaceID(final ServerException e) {
		final Matcher m = WSERR.matcher(e.getMessage());
		if (m.find()) {
			if (m.group(1) != null) {
				return Integer.parseInt(m.group(1)); // deleted
			} else {
				return Integer.parseInt(m.group(2)); // exists
			}
		} else {
			return null;
		}
	}

	@Override
	public ResourceInformationSet getResourceInformation(
			final UserName user,
			final Set<ResourceID> resources,
			boolean administratedResourcesOnly)
			throws ResourceHandlerException, IllegalResourceIDException {
		checkNoNullsInCollection(resources, "resources");
		if (user == null) {
			administratedResourcesOnly = true; // only return public workspaces
		}
		//TODO WS make a bulk ws method for getwsinfo that returns error code (DELETED, MISSING, INACCESSIBLE, etc.) for inaccessible workspaces
		//TODO WS for get perms mass make ignore error option that returns error state (DELETED, MISSING, INACCESSIBLE etc.) and use here instead of going one at a time
		final ResourceInformationSet.Builder b = ResourceInformationSet.getBuilder(user);
		for (final ResourceID rid: resources) {
			final long wsid = getWSID(rid);
			final Perms perms;
			try {
				perms = getPermissions(Arrays.asList(wsid), false);
			} catch (NoSuchResourceException e) {
				throw new RuntimeException("This should be impossible", e);
			}
			if (perms.perms == null) {
				b.withNonexistentResource(rid);
			} else {
				final Perm perm = new Perm(user, perms.perms.get(0));
				if (!administratedResourcesOnly || perm.perm.isAdmin() || perm.isPublic) {
					final WSInfoOwner wi = getWSInfo(wsid);
					if (wi == null) {
						// should almost never happen since we checked for inaccessible ws above
						b.withNonexistentResource(rid);
					} else {
						if (user != null && wi.owner.equals(user.getName())) {
							wi.wi.put("perm", WorkspacePermission.OWN.getRepresentation());
						} else {
							wi.wi.put("perm", perm.perm.getRepresentation());
						}
						wi.wi.keySet().stream()
								.forEach(s -> b.withResourceField(rid, s, wi.wi.get(s)));
					}
				}
			}
		}
		return b.build();
	}

	private static final TypeReference<Tuple9<Long, String, String, String, Long, String,
			String, String, Map<String, String>>> WS_INFO_TYPEREF =
				new TypeReference<Tuple9<Long, String, String, String, Long, String, String,
						String,Map<String,String>>>() {};

	private static class WSInfoOwner {
		private final Map<String, Object> wi;
		private final String owner;
		
		private WSInfoOwner(Map<String, Object> wi, String owner) {
			this.wi = wi;
			this.owner = owner;
		}
	}
						
	// returns null if missing or deleted
	private WSInfoOwner getWSInfo(final long wsid) throws ResourceHandlerException {
		final Tuple9<Long, String, String, String, Long, String, String, String,
				Map<String, String>> wsinfo;
		try {
			wsinfo = client.administer(new UObject(ImmutableMap.of(
					"command", "getWorkspaceInfo",
					"params", new WorkspaceIdentity().withId((long) wsid))))
					.asClassInstance(WS_INFO_TYPEREF);
		} catch (ServerException e) {
			if (getWorkspaceID(e) != null) { // deleted or missing
				return null;
			} else {
				throw getGeneralWSException(e);
			}
		} catch (IOException | JsonClientException | IllegalStateException e) {
			throw getGeneralWSException(e);
		}
		final Map<String, Object> ret = new HashMap<>();
		ret.put("name", wsinfo.getE2());
		ret.put("narrname", getNarrativeName(wsinfo.getE9()));
		ret.put("public", PERM_READ.equals(wsinfo.getE7()));
		return new WSInfoOwner(ret, wsinfo.getE3());
	}

	private String getNarrativeName(final Map<String, String> meta) {
		if ("false".equals(meta.get("is_temporary"))) {
			// if nice name is null will return null, obviously
			return meta.get("narrative_nice_name");
		} else {
			return null;
		}
	}
	
	@Override
	public Set<ResourceAdministrativeID> getAdministratedResources(final UserName user)
			throws ResourceHandlerException {
		checkNotNull(user, "user");
		final List<Long> ids;
		try {
			ids = client.administer(new UObject(ImmutableMap.of(
					"command", "listWorkspaceIDs",
					"user", user.getName(),
					"params", new ListWorkspaceIDsParams().withPerm(PERM_ADMIN))))
					.asClassInstance(ListWorkspaceIDsResults.class)
					.getWorkspaces();
		} catch (IOException | JsonClientException e) {
			throw getGeneralWSException(e);
		}
		return ids.stream().map(i -> ResourceAdministrativeID.from(i)).collect(Collectors.toSet());
	}
	
	@Override
	public Set<UserName> getAdministrators(final ResourceID resource)
			throws NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException {
		checkNotNull(resource, "resource");
		final Map<String, String> perms = getPermissions(Arrays.asList(getWSID(resource)), true)
				.perms.get(0);
		final Set<UserName> ret = new HashSet<>();
		for (final String user: perms.keySet()) {
			if (PERM_ADMIN.equals(perms.get(user))) {
				try {
					ret.add(new UserName(user));
				} catch (MissingParameterException | IllegalParameterException e) {
					throw new RuntimeException(
							"Unexpected illegal user name returned from workspace: " +
							e.getMessage(), e);
				}
			}
		}
		return ret;
	}
	
	private static final Set<String> READ_PERMS = new HashSet<>(Arrays.asList(
			PERM_READ, PERM_WRITE, PERM_ADMIN));
	
	@Override
	public void setReadPermission(final ResourceID resource, final UserName user)
			throws IllegalResourceIDException, NoSuchResourceException, ResourceHandlerException {
		checkNotNull(resource, "resource");
		checkNotNull(user, "user");
		final long wsid = getWSID(resource);
		final Map<String, String> perms = getPermissions(Arrays.asList(wsid), true)
				.perms.get(0);
		if (!READ_PERMS.contains(perms.get(user.getName())) &&
				!PERM_READ.equals(perms.get(GLOBAL_READ_USER))) {
			// tiny chance for a race condition here
			// TODO WS add temporary perms to workspace service so these granted perms can expire rather than forcing the user to remove them if s/he doesn't want them
			try {
				client.administer(new UObject(ImmutableMap.of(
						"command", "setPermissions",
						"params", new SetPermissionsParams()
								.withId(wsid)
								.withNewPermission("r")
								.withUsers(Arrays.asList(user.getName())))));
			} catch (IOException | JsonClientException e) {
				throw getGeneralWSException(e);
			}
		}
	}

	@Override
	public ResourceDescriptor getDescriptor(final ResourceID resource) 
			throws IllegalResourceIDException {
		checkNotNull(resource, "resource");
		getWSID(resource); // check for bad id
		return new ResourceDescriptor(resource);
	}
}
