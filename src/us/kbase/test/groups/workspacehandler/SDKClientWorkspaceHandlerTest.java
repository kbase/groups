package us.kbase.test.groups.workspacehandler;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.google.common.collect.ImmutableMap;

import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.workspacehandler.SDKClientWorkspaceHandler;
import us.kbase.test.groups.TestCommon;
import us.kbase.workspace.WorkspaceClient;

public class SDKClientWorkspaceHandlerTest {
	
	private static class UObjectArgumentMatcher implements ArgumentMatcher<UObject> {

		private final Map<String, Object> adminPackage;

		public UObjectArgumentMatcher(final Map<String, Object> adminPackage) {
			this.adminPackage = adminPackage;
		}
		
		@Override
		public boolean matches(final UObject uo) {
			final Object obj = uo.asInstance();
			final boolean match = adminPackage.equals(obj);
			if (!match) {
				System.out.println(String.format("UObject match failed. Expected:\n%s\nGot:\n%s",
						adminPackage, obj));
			}
			return match;
		}
	}
	
	@Test
	public void constructFailNull() throws Exception {
		failConstruct(null, new NullPointerException("client"));
	}
	
	@Test
	public void constructFailVersion() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.7.999999");
		
		failConstruct(c, new WorkspaceHandlerException(
				"Workspace version 0.8.0 or greater is required"));
	}
	
	@Test
	public void constructFailJsonClientException() throws Exception {
		constructFail(new JsonClientException("User foo is not an admin"));
	}
	
	@Test
	public void constructFailIOException() throws Exception {
		constructFail(new IOException("blearg"));
	}


	private void constructFail(final Exception exception) throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://bar.com"));
		
		when(c.administer(argThat(new UObjectArgumentMatcher(
				ImmutableMap.of("command", "listAdmins")))))
				.thenThrow(exception);
		
		failConstruct(c, new WorkspaceHandlerException(
				"Error contacting workspace at http://bar.com"));
	}
	
	private void failConstruct(final WorkspaceClient c, final Exception expected) {
		try {
			new SDKClientWorkspaceHandler(c);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isAdminAdmin() throws Exception {
		isAdmin("user1", true);
	}
	
	@Test
	public void isAdminWrite() throws Exception {
		isAdmin("user2", false);
	}
	
	@Test
	public void isAdminNoUser() throws Exception {
		isAdmin("user3", false);
	}

	private void isAdmin(final String user, final boolean expected) throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		final Map<String, Object> command = ImmutableMap.of(
				"command", "getPermissionsMass",
				"params", ImmutableMap.of("workspaces", Arrays.asList(ImmutableMap.of("id", 24))));
				
		when(c.administer(argThat(new UObjectArgumentMatcher(command))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
						ImmutableMap.of("user1", "a", "user2", "w")))));
		
		assertThat("incorrect admin", h.isAdministrator(
				new WorkspaceID(24), new UserName(user)), is(expected));
	}
	
	@Test
	public void isAdminFailNulls() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failIsAdmin(h, null, new UserName("u"), new NullPointerException("wsid"));
		failIsAdmin(h, new WorkspaceID(1), null, new NullPointerException("user"));
	}
	
	@Test
	public void isAdminFailDeletedWS() throws Exception {
		isAdminFail(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchWorkspaceException("24"));
	}
	
	@Test
	public void isAdminFailMissingWS() throws Exception {
		isAdminFail(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchWorkspaceException("24"));
	}
	
	@Test
	public void isAdminFailOtherServerException() throws Exception {
		isAdminFail(new ServerException("You pootied real bad I can smell it", -1, "n"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailJsonClientException() throws Exception {
		isAdminFail(new JsonClientException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIOException() throws Exception {
		isAdminFail(new IOException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIllegalStateException() throws Exception {
		isAdminFail(new IllegalStateException("You pootied real bad I can smell it"),
				new WorkspaceHandlerException("Error contacting workspace at http://foo.com"));
	}

	private void isAdminFail(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn("0.8.0");
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		final Map<String, Object> command = ImmutableMap.of(
				"command", "getPermissionsMass",
				"params", ImmutableMap.of("workspaces", Arrays.asList(ImmutableMap.of("id", 24))));
				
		when(c.administer(argThat(new UObjectArgumentMatcher(command))))
				.thenThrow(exception);
		
		failIsAdmin(h, new WorkspaceID(24), new UserName("user"), expected);
	}
	
	private void failIsAdmin(
			final SDKClientWorkspaceHandler h,
			final WorkspaceID id,
			final UserName user,
			final Exception expected) {
		try {
			h.isAdministrator(id, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
