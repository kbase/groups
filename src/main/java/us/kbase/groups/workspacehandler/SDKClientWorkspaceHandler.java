package us.kbase.groups.workspacehandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import us.kbase.groups.core.resource.ResourceAccess;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceHandler;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.workspace.GetObjectInfo3Params;
import us.kbase.workspace.GetObjectInfo3Results;
import us.kbase.workspace.GetPermissionsMassParams;
import us.kbase.workspace.ListWorkspaceIDsParams;
import us.kbase.workspace.ListWorkspaceIDsResults;
import us.kbase.workspace.ObjectSpecification;
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
	private static final String PUBLIC = "public";
	
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(
			"yyyy'-'MM'-'dd'T'HH':'mm':'ssX");
	
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
			//TODO WS add a method to check what admin creds you have to WS and use here
			client.administer(new UObject(ImmutableMap.of("command", "listModRequests")));
		} catch (IOException | JsonClientException e) {
			throw getGeneralWSException(e);
		}
		
		if (Version.valueOf(ver).lessThan(Version.forIntegers(0, 8, 2))) {
			throw new ResourceHandlerException("Workspace version 0.8.2 or greater is required");
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
		requireNonNull(resource, "resource");
		requireNonNull(user, "user");
		final Perms perms = getPermissions(Arrays.asList(getWSID(resource)), true);
		return new Perm(user, perms.perms.get(0)).perm.isAdmin();
	}
	
	@Override
	public boolean isPublic(final ResourceID resource)
			throws IllegalResourceIDException, ResourceHandlerException, NoSuchResourceException {
		requireNonNull(resource, "resource");
		return (boolean) getWSInfo(getWSID(resource), false, true).wi.get(PUBLIC);
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
			final ResourceAccess access)
			throws ResourceHandlerException, IllegalResourceIDException {
		checkNoNullsInCollection(resources, "resources");
		requireNonNull(access, "access");
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
				if (hasAccess(perm, access)) {
					final WSInfoOwner wi;
					try {
						wi = getWSInfo(wsid, true, false);
					} catch (NoSuchResourceException e) {
						throw new RuntimeException("Shouldn't be possible", e);
					}
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

	// hm. This seems nasty, but the ResourceAccess class makes sense to me...
	// For now I'll keep the nasty implementation and more readable, IMO, API.
	private boolean hasAccess(final Perm perm, final ResourceAccess access) {
		if (ResourceAccess.ALL.equals(access)) {
			return true;
		} else if (ResourceAccess.ADMINISTRATED_AND_PUBLIC.equals(access) &&
				(perm.perm.isAdmin() || perm.isPublic)) {
			return true;
		} else if (ResourceAccess.ADMINISTRATED.equals(access) && perm.perm.isAdmin()) {
			return true;
		}
		return false;
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
	private WSInfoOwner getWSInfo(
			final long wsid,
			boolean withDescriptionAndNarrativeInfo,
			boolean throwNoWorkspaceException)
			throws ResourceHandlerException, NoSuchResourceException {
		final Tuple9<Long, String, String, String, Long, String, String, String,
				Map<String, String>> wsinfo;
		final String desc;
		final Optional<NarrInfo> narrInfo;
		try {
			final WorkspaceIdentity wsi = new WorkspaceIdentity().withId((long) wsid);
			wsinfo = client.administer(new UObject(ImmutableMap.of(
					"command", "getWorkspaceInfo", "params", wsi)))
					.asClassInstance(WS_INFO_TYPEREF);
			if (withDescriptionAndNarrativeInfo) {
				final UObject d = client.administer(new UObject(ImmutableMap.of(
						"command", "getWorkspaceDescription", "params", wsi)));
				desc = d == null ? null : d.asScalar();
				narrInfo = Optional.of(getNarrativeInfo(wsinfo.getE1(), wsinfo.getE9()));
			} else {
				desc = null;
				narrInfo = Optional.empty();
			}
		} catch (ServerException e) {
			final Integer errorid = getWorkspaceID(e);
			if (errorid != null) { // deleted or missing
				if (throwNoWorkspaceException) {
					throw new NoSuchResourceException(errorid + "", e);
				} else {
					return null;
				}
			} else {
				throw getGeneralWSException(e);
			}
		} catch (IOException | JsonClientException | IllegalStateException e) {
			throw getGeneralWSException(e);
		}
		final Map<String, Object> ret = new HashMap<>();
		ret.put("name", wsinfo.getE2());
		ret.put("narrname", narrInfo.map(n -> n.name).orElse(null));
		ret.put("narrcreate", narrInfo.map(n -> n.created).orElse(null));
		ret.put(PUBLIC, PERM_READ.equals(wsinfo.getE7()));
		ret.put("moddate", timestampToEpochMS(wsinfo.getE4()));
		ret.put("description", desc);
		return new WSInfoOwner(ret, wsinfo.getE3());
	}

	private long timestampToEpochMS(final String timestamp) {
		return OffsetDateTime.parse(timestamp, FMT).toInstant().toEpochMilli();
	}

	private static class NarrInfo {
		private String name;
		private Long created; // the date of the 1st version of the narrative object
		
		private NarrInfo(final String name, final Long created) {
			this.name = name;
			this.created = created;
		}
	}
	
	private NarrInfo getNarrativeInfo(final long wsid, final Map<String, String> meta)
			throws IOException, JsonClientException {
		if ("false".equals(meta.get("is_temporary")) && meta.containsKey("narrative")) {
			final String name = meta.get("narrative_nice_name");
			final long narrativeID = Integer.parseInt(meta.get("narrative"));
			final GetObjectInfo3Results objinfo = client.administer(new UObject(ImmutableMap.of(
					"command", "getObjectInfo",
					"params", new GetObjectInfo3Params()
							.withObjects(Arrays.asList(new ObjectSpecification()
									.withWsid(wsid).withObjid(narrativeID).withVer((long) 1))))))
					.asClassInstance(GetObjectInfo3Results.class);
			final Long saved = timestampToEpochMS(objinfo.getInfos().get(0).getE4());
			return new NarrInfo(name, saved);
		} else {
			return new NarrInfo(null, null);
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
		final Map<String, String> perms = getPermissions(Arrays.asList(wsid), true).perms.get(0);
		if (!READ_PERMS.contains(perms.get(user.getName())) &&
				!PERM_READ.equals(perms.get(GLOBAL_READ_USER))) {
			// tiny chance for a race condition here
			// TODO WS add temporary perms to workspace service so these granted perms can expire rather than forcing the user to remove them if s/he doesn't want them. Optional - some perms should be permanent
			// TODO WS flag to grant greater of requested & current perm (see $max)
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
