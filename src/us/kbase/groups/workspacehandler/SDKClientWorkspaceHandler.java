package us.kbase.groups.workspacehandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple9;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;
import us.kbase.groups.core.workspace.WorkspaceHandler;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;
import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.workspace.GetPermissionsMassParams;
import us.kbase.workspace.WorkspaceClient;
import us.kbase.workspace.WorkspaceIdentity;

/** A handler implementation that uses a provided SDK workspace client to communicate with the 
 * workspace.
 * @author gaprice@lbl.gov
 *
 */
public class SDKClientWorkspaceHandler implements WorkspaceHandler {
	
	// TODO TEST
	// TODO CACHE may help to cache all or some of the results. YAGNI for now.

	private final WorkspaceClient client;
	
	/** Create the handler.
	 * @param client the workspace client to use to communicate with the workspace. The
	 * client must be initialized with a token with at least administrative read privileges.
	 * @throws WorkspaceHandlerException
	 */
	public SDKClientWorkspaceHandler(final WorkspaceClient client)
			throws WorkspaceHandlerException {
		checkNotNull(client, "client");
		this.client = client;
		final String ver;
		try {
			ver = client.ver();
			// ensure the client has admin creds
			client.administer(new UObject(ImmutableMap.of("command", "listAdmins")));
		} catch (IOException | JsonClientException e) {
			throw getGeneralWSException(e);
		}
		
		if (Version.valueOf(ver).lessThan(Version.forIntegers(0, 8))) {
			throw new WorkspaceHandlerException("Workspace version 0.8.0 or greater is required");
		}
	}

	private WorkspaceHandlerException getGeneralWSException(final Exception e) {
		return new WorkspaceHandlerException(String.format(
				"Error contacting workspace at %s", client.getURL()), e);
	}

	@Override
	public boolean isAdministrator(final WorkspaceID wsid, final UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException {
		checkNotNull(wsid, "wsid");
		checkNotNull(user, "user");
		final Perms perms = getPermissions(Arrays.asList(wsid.getID()), true);
		return new Perm(user, perms.perms.get(0)).isAdmin;
	}
	
	private final TypeReference<Map<String, List<Map<String, String>>>> TR_GET_PERMS =
			new TypeReference<Map<String, List<Map<String, String>>>>() {};

	private static class Perm {
		
		private final boolean isAdmin;
		private final boolean isPublic;
		
		private Perm(final UserName user, final Map<String, String> perms) {
			this.isAdmin = "a".equals(perms.get(user == null ? null : user.getName()));
			this.isPublic = "r".equals(perms.get("*"));
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
			
	private Perms getPermissions(final List<Integer> ids, final boolean throwNoWorkspaceException)
			throws NoSuchWorkspaceException, WorkspaceHandlerException {
		final List<Map<String, String>> perms;
		try {
			perms = client.administer(new UObject(ImmutableMap.of(
					"command", "getPermissionsMass",
					"params", new GetPermissionsMassParams().withWorkspaces(
							ids.stream().map(id -> new WorkspaceIdentity().withId((long) id))
									.collect(Collectors.toList())))))
					.asClassInstance(TR_GET_PERMS).get("perms");
		} catch (ServerException e) {
			final Integer errorid = getWorkspaceID(e);
			if (errorid != null) {
				if (throwNoWorkspaceException) {
					throw new NoSuchWorkspaceException(errorid + "", e);
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
	public WorkspaceInfoSet getWorkspaceInformation(
			final UserName user,
			final WorkspaceIDSet ids,
			boolean administratedWorkspacesOnly)
			throws WorkspaceHandlerException {
		checkNotNull(ids, "ids");
		if (user == null) {
			administratedWorkspacesOnly = true; // only return public workspaces
		}
		//TODO WS make a bulk ws method for getwsinfo that returns error code (DELETED, MISSING, INACCESSIBLE, etc.) for inaccessible workspaces
		//TODO WS for get perms mass make ignore error option that returns error state (DELETED, MISSING, INACCESSIBLE etc.) and use here instead of going one at a time
		final WorkspaceInfoSet.Builder b = WorkspaceInfoSet.getBuilder(user);
		for (final Integer wsid: ids.getIDs()) {
			final Perms perms;
			try {
				perms = getPermissions(Arrays.asList(wsid), false);
			} catch (NoSuchWorkspaceException e) {
				throw new RuntimeException("This should be impossible", e);
			}
			if (perms.perms == null) {
				b.withNonexistentWorkspace(wsid);
			} else {
				final Perm perm = new Perm(user, perms.perms.get(0));
				if (!administratedWorkspacesOnly || perm.isAdmin || perm.isPublic) {
					final WorkspaceInformation wi = getWSInfo(wsid);
					if (wi == null) {
						// should almost never happen since we checked for inaccessible ws above
						b.withNonexistentWorkspace(wsid);
					} else {
						b.withWorkspaceInformation(wi, perm.isAdmin);
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

	// returns null if missing or deleted
	private WorkspaceInformation getWSInfo(final int wsid) throws WorkspaceHandlerException {
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
		return WorkspaceInformation.getBuilder(Math.toIntExact(wsinfo.getE1()), wsinfo.getE2())
				.withNullableNarrativeName(getNarrativeName(wsinfo.getE9()))
				.withIsPublic("r".equals(wsinfo.getE7()))
				.build();
	}

	private String getNarrativeName(final Map<String, String> meta) {
		if ("true".equals(meta.get("is_temporary"))) {
			return null;
		} else if (meta.get("narrative_nice_name") != null) {
			return meta.get("narrative_nice_name");
		} else {
			return null;
		}
	}

	public static void main(final String[] args) throws Exception {
		final WorkspaceClient ws = new WorkspaceClient(
				new URL("https://ci.kbase.us/services/ws/"),
				new AuthToken(args[0], "<fake>"));
		
		final SDKClientWorkspaceHandler sws = new SDKClientWorkspaceHandler(ws);
		System.out.println(sws.isAdministrator(new WorkspaceID(36967), new UserName("gaprice")));
		System.out.println(sws.isAdministrator(new WorkspaceID(36967), new UserName("msneddon")));
		
		System.out.println(sws.isAdministrator(new WorkspaceID(20554), new UserName("gaprice")));
		System.out.println(sws.isAdministrator(new WorkspaceID(20554), new UserName("msneddon")));
		
		System.out.println(sws.isAdministrator(new WorkspaceID(37268), new UserName("gaprice")));
		System.out.println(sws.isAdministrator(new WorkspaceID(37268), new UserName("msneddon")));
		
		final WorkspaceInfoSet wi1 = sws.getWorkspaceInformation(
				new UserName("gaprice"),
				WorkspaceIDSet.fromInts(new HashSet<>(
						Arrays.asList(36967, 20554, 37268, 37266, 100000, 37267, 35854))),
				false);
		System.out.println(wi1);
		System.out.println(wi1.getWorkspaceInformation().size());
		final WorkspaceInfoSet wi2 = sws.getWorkspaceInformation(
				new UserName("gaprice"),
				WorkspaceIDSet.fromInts(new HashSet<>(
						Arrays.asList(36967, 20554, 37268, 37266, 100000, 37267, 35854))),
				true);
		System.out.println(wi2);
		System.out.println(wi2.getWorkspaceInformation().size());
		final WorkspaceInfoSet wi3 = sws.getWorkspaceInformation(
				null,
				WorkspaceIDSet.fromInts(new HashSet<>(
						Arrays.asList(36967, 20554, 37268, 37266, 100000, 37267, 35854))),
				false);
		System.out.println(wi3);
		System.out.println(wi3.getWorkspaceInformation().size());
		final WorkspaceInfoSet wi4 = sws.getWorkspaceInformation(
				null,
				WorkspaceIDSet.fromInts(new HashSet<>(
						Arrays.asList(36967, 20554, 37268, 37266, 100000, 37267, 35854))),
				true);
		System.out.println(wi4);
		System.out.println(wi4.getWorkspaceInformation().size());
		
		try {
			sws.isAdministrator(new WorkspaceID(37266), new UserName("doesntmatterdeleted"));
		} catch (NoSuchWorkspaceException e) {
			e.printStackTrace();
		}
		sws.isAdministrator(new WorkspaceID(10000000), new UserName("doesntmatterdeleted"));
		
	}

}
