package us.kbase.groups.workspaceHandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.WorkspaceHandler;
import us.kbase.groups.core.WorkspaceID;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;
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
		
		final Version v = Version.valueOf(ver);
		if (v.lessThan(Version.forIntegers(0, 8))) {
			throw new WorkspaceHandlerException("Workspace version 0.8.0 or greater is required");
		}
	}

	private WorkspaceHandlerException getGeneralWSException(final Exception e) {
		return new WorkspaceHandlerException(String.format(
				"Error contacting workspace at %s", client.getURL()), e);
	}
	
	private final TypeReference<Map<String, List<Map<String, String>>>> TR_GET_PERMS =
			new TypeReference<Map<String, List<Map<String, String>>>>() {};
	
	@Override
	public boolean isAdmin(final WorkspaceID wsid, final UserName user)
			throws WorkspaceHandlerException {
		checkNotNull(wsid, "wsid");
		checkNotNull(user, "user");
		final Map<String, String> perms;
		try {
			perms = client.administer(new UObject(ImmutableMap.of(
					"command", "getPermissionsMass",
					"params", new GetPermissionsMassParams().withWorkspaces(
							Arrays.asList(new WorkspaceIdentity().withId((long) wsid.getId()))))))
					.asClassInstance(TR_GET_PERMS).get("perms").get(0);
		} catch (IOException | JsonClientException | IllegalStateException e) {
			throw getGeneralWSException(e);
		}
		return "a".equals(perms.get(user.getName()));
	}
	
	public static void main(final String[] args) throws Exception {
		final WorkspaceClient ws = new WorkspaceClient(
				new URL("https://ci.kbase.us/services/ws/"),
				new AuthToken(args[0], "<fake>"));
		
		final SDKClientWorkspaceHandler sws = new SDKClientWorkspaceHandler(ws);
		System.out.println(sws.isAdmin(new WorkspaceID(36967), new UserName("gaprice")));
		System.out.println(sws.isAdmin(new WorkspaceID(36967), new UserName("msneddon")));
		
		System.out.println(sws.isAdmin(new WorkspaceID(20554), new UserName("gaprice")));
		System.out.println(sws.isAdmin(new WorkspaceID(20554), new UserName("msneddon")));
	}

}
