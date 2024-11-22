package us.kbase.test.groups.workspacehandler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.google.common.collect.ImmutableMap;

import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.ServerException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple9;
import us.kbase.common.service.UObject;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.resource.ResourceAccess;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.workspacehandler.SDKClientWorkspaceHandler;
import us.kbase.test.groups.TestCommon;
import us.kbase.workspace.WorkspaceClient;

public class SDKClientWorkspaceHandlerTest {
	
	private static boolean DEBUG = false;
	
	private static String MIN_WS_VER = "0.8.2";
	
	private static final ResourceID RD3;
	private static final ResourceID RD5;
	private static final ResourceID RD7;
	private static final ResourceID RD8;
	private static final ResourceID RD9;
	private static final ResourceID RD10;
	private static final ResourceID RD11;
	private static final ResourceID RD20;
	private static final ResourceID RD21;
	private static final ResourceID RD30;
	private static final ResourceID RD31;
	private static final ResourceID RD40;
	private static final ResourceID RD41;
	static {
		try {
			RD3 = new ResourceID("3");
			RD5 = new ResourceID("5");
			RD7 = new ResourceID("7");
			RD8 = new ResourceID("8");
			RD9 = new ResourceID("9");
			RD10 = new ResourceID("10");
			RD11 = new ResourceID("11");
			RD20 = new ResourceID("20");
			RD21 = new ResourceID("21");
			RD30 = new ResourceID("30");
			RD31 = new ResourceID("31");
			RD40 = new ResourceID("40");
			RD41 = new ResourceID("41");
		} catch (Exception e) {
			throw new RuntimeException("Fix yer tests", e);
		}
	}
	
	private static class UObjectArgumentMatcher implements ArgumentMatcher<UObject> {

		private final Map<String, Object> adminPackage;

		public UObjectArgumentMatcher(final Map<String, Object> adminPackage) {
			this.adminPackage = adminPackage;
		}
		
		@Override
		public boolean matches(final UObject uo) {
			final Object obj = uo.asInstance();
			final boolean match = adminPackage.equals(obj);
			if (DEBUG && !match) {
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
		
		when(c.ver()).thenReturn("0.8.1");
		
		failConstruct(c, new ResourceHandlerException(
				"Workspace version 0.8.2 or greater is required"));
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
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://bar.com"));
		
		when(c.administer(argThat(new UObjectArgumentMatcher(
				ImmutableMap.of("command", "listModRequests")))))
				.thenThrow(exception);
		
		failConstruct(c, new ResourceHandlerException(
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
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
						ImmutableMap.of("user1", "a", "user2", "w")))));
		
		assertThat("incorrect admin", h.isAdministrator(
				new ResourceID("24"), new UserName(user)), is(expected));
	}

	private UObjectArgumentMatcher getPermissionsCommandMatcher(final int wsid) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getPermissionsMass",
				"params", ImmutableMap.of("workspaces",
						Arrays.asList(ImmutableMap.of("id", wsid)))));
	}
	
	private UObjectArgumentMatcher setPermissionsCommandMatcher(
			final int wsid,
			final String user,
			final String permission) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "setPermissions",
				"params", ImmutableMap.of(
						"id", wsid,
						"users", Arrays.asList(user),
						"new_permission", permission)));
	}
	
	@Test
	public void isAdminFailBadArgs() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failIsAdmin(h, null, new UserName("u"), new NullPointerException("resource"));
		failIsAdmin(h, new ResourceID("yay"), new UserName("u"),
				new IllegalResourceIDException("yay"));
		failIsAdmin(h, new ResourceID("1"), null, new NullPointerException("user"));
	}
	
	@Test
	public void isAdminFailDeletedWS() throws Exception {
		isAdminFail(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void isAdminFailMissingWS() throws Exception {
		isAdminFail(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void isAdminFailOtherServerException() throws Exception {
		isAdminFail(new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailJsonClientException() throws Exception {
		isAdminFail(new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIOException() throws Exception {
		isAdminFail(new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isAdminFailIllegalStateException() throws Exception {
		isAdminFail(new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}

	private void isAdminFail(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(exception);
		
		failIsAdmin(h, new ResourceID("24"), new UserName("user"), expected);
	}
	
	private void failIsAdmin(
			final SDKClientWorkspaceHandler h,
			final ResourceID id,
			final UserName user,
			final Exception expected) {
		try {
			h.isAdministrator(id, user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isPublicFalse() throws Exception {
		isPublic(false);
	}
	
	@Test
	public void isPublicTrue() throws Exception {
		isPublic(true);
	}

	private void isPublic(final boolean pub) throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(getWorkspaceInfoResponse(3, "name3", "user1", "2018-10-27T02:16:53+0000", pub,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(3)));
		
		assertThat("incorrect is public", h.isPublic(new ResourceID("3")), is(pub));
		
		verify(c, times(2)).administer(any()); // admin() is called in the constructor
	}
	
	@Test
	public void isPublicFailBadArgs() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failIsPublic(h, null, new NullPointerException("resource"));
		failIsPublic(h, new ResourceID("yay"), new IllegalResourceIDException("yay"));
	}
	
	@Test
	public void isPublicFailDeletedWS() throws Exception {
		isPublicFail(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void isPublicFailMissingWS() throws Exception {
		isPublicFail(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void isPublicFailOtherServerException() throws Exception {
		isPublicFail(new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isPublicFailJsonClientException() throws Exception {
		isPublicFail(new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isPublicFailIOException() throws Exception {
		isPublicFail(new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void isPublicFailIllegalStateException() throws Exception {
		isPublicFail(new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	private void isPublicFail(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getWSInfoCommandMatcher(3)))).thenThrow(exception);
		
		failIsPublic(h, new ResourceID("3"), expected);
	}
	
	private void failIsPublic(
			final SDKClientWorkspaceHandler h,
			final ResourceID id,
			final Exception expected) {
		try {
			h.isPublic(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getResourceInformationNoWS() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		final ResourceInformationSet wi = h.getResourceInformation(
				new UserName("u"), set(), ResourceAccess.ALL);
		
		assertThat("incorrect wsi", wi, is(ResourceInformationSet.getBuilder(new UserName("u"))
				.build()));
	}

	@Test
	public void getResourceInformationFull() throws Exception {
		getResourceInformation(
				new UserName("user1"),
				ResourceAccess.ALL,
				ResourceInformationSet.getBuilder(new UserName("user1"))
						.withNonexistentResource(RD8)
						.withNonexistentResource(RD9)
						.withNonexistentResource(RD20)
						.withNonexistentResource(RD21)
						.withNonexistentResource(RD30)
						.withNonexistentResource(RD31)
						.withNonexistentResource(RD40)
						.withNonexistentResource(RD41)
						.withResourceField(RD3, "name", "name3")
						.withResourceField(RD3, "public", false)
						.withResourceField(RD3, "narrname", null)
						.withResourceField(RD3, "narrcreate", null)
						.withResourceField(RD3, "perm", "Own")
						.withResourceField(RD3, "description", "my desc")
						.withResourceField(RD3, "moddate", 1540606613000L)
						.withResourceField(RD5, "name", "name5")
						.withResourceField(RD5, "public", false)
						.withResourceField(RD5, "narrname", "narr_name")
						.withResourceField(RD5, "narrcreate", 1500000000000L)
						.withResourceField(RD5, "perm", "Admin")
						.withResourceField(RD5, "description", null)
						.withResourceField(RD5, "moddate", 0L)
						.withResourceField(RD7, "name", "name7")
						.withResourceField(RD7, "public", false)
						.withResourceField(RD7, "narrname", null)
						.withResourceField(RD7, "narrcreate", null)
						.withResourceField(RD7, "perm", "Write")
						.withResourceField(RD7, "description", "my desc7")
						.withResourceField(RD7, "moddate", 31535999000L)
						.withResourceField(RD10, "name", "name10")
						.withResourceField(RD10, "public", true)
						.withResourceField(RD10, "narrname", null)
						.withResourceField(RD10, "narrcreate", null)
						.withResourceField(RD10, "perm", "Read")
						.withResourceField(RD10, "description", "my desc10")
						.withResourceField(RD10, "moddate", 1500000000000L)
						.withResourceField(RD11, "name", "name11")
						.withResourceField(RD11, "public", true)
						.withResourceField(RD11, "narrname", null)
						.withResourceField(RD11, "narrcreate", null)
						.withResourceField(RD11, "perm", "None")
						.withResourceField(RD11, "description", "my desc11")
						.withResourceField(RD11, "moddate", 549864182000L)
						.build());
	}
	
	@Test
	public void getResourceInformationFullAnonUser() throws Exception {
		getResourceInformation(
				null,
				ResourceAccess.ALL,
				ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(RD8)
						.withNonexistentResource(RD9)
						.withNonexistentResource(RD20)
						.withNonexistentResource(RD21)
						.withNonexistentResource(RD30)
						.withNonexistentResource(RD31)
						.withNonexistentResource(RD40)
						.withNonexistentResource(RD41)
						.withResourceField(RD3, "name", "name3")
						.withResourceField(RD3, "public", false)
						.withResourceField(RD3, "narrname", null)
						.withResourceField(RD3, "narrcreate", null)
						.withResourceField(RD3, "perm", "None")
						.withResourceField(RD3, "description", "my desc")
						.withResourceField(RD3, "moddate", 1540606613000L)
						.withResourceField(RD5, "name", "name5")
						.withResourceField(RD5, "public", false)
						.withResourceField(RD5, "narrname", "narr_name")
						.withResourceField(RD5, "narrcreate", 1500000000000L)
						.withResourceField(RD5, "perm", "None")
						.withResourceField(RD5, "description", null)
						.withResourceField(RD5, "moddate", 0L)
						.withResourceField(RD7, "name", "name7")
						.withResourceField(RD7, "public", false)
						.withResourceField(RD7, "narrname", null)
						.withResourceField(RD7, "narrcreate", null)
						.withResourceField(RD7, "perm", "None")
						.withResourceField(RD7, "description", "my desc7")
						.withResourceField(RD7, "moddate", 31535999000L)
						.withResourceField(RD10, "name", "name10")
						.withResourceField(RD10, "public", true)
						.withResourceField(RD10, "narrname", null)
						.withResourceField(RD10, "narrcreate", null)
						.withResourceField(RD10, "perm", "None")
						.withResourceField(RD10, "description", "my desc10")
						.withResourceField(RD10, "moddate", 1500000000000L)
						.withResourceField(RD11, "name", "name11")
						.withResourceField(RD11, "public", true)
						.withResourceField(RD11, "narrname", null)
						.withResourceField(RD11, "narrcreate", null)
						.withResourceField(RD11, "perm", "None")
						.withResourceField(RD11, "description", "my desc11")
						.withResourceField(RD11, "moddate", 549864182000L)
						.build());
	}
	
	@Test
	public void getResourceInformationAdminPublic() throws Exception {
		getResourceInformation(
				new UserName("user1"),
				ResourceAccess.ADMINISTRATED_AND_PUBLIC,
				ResourceInformationSet.getBuilder(new UserName("user1"))
						.withNonexistentResource(RD9)
						.withNonexistentResource(RD20)
						.withNonexistentResource(RD21)
						.withNonexistentResource(RD30)
						.withNonexistentResource(RD31)
						.withNonexistentResource(RD40)
						.withNonexistentResource(RD41)
						.withResourceField(RD3, "name", "name3")
						.withResourceField(RD3, "public", false)
						.withResourceField(RD3, "narrname", null)
						.withResourceField(RD3, "narrcreate", null)
						.withResourceField(RD3, "perm", "Own")
						.withResourceField(RD3, "description", "my desc")
						.withResourceField(RD3, "moddate", 1540606613000L)
						.withResourceField(RD5, "name", "name5")
						.withResourceField(RD5, "public", false)
						.withResourceField(RD5, "narrname", "narr_name")
						.withResourceField(RD5, "narrcreate", 1500000000000L)
						.withResourceField(RD5, "perm", "Admin")
						.withResourceField(RD5, "description", null)
						.withResourceField(RD5, "moddate", 0L)
						.withResourceField(RD10, "name", "name10")
						.withResourceField(RD10, "public", true)
						.withResourceField(RD10, "narrname", null)
						.withResourceField(RD10, "narrcreate", null)
						.withResourceField(RD10, "perm", "Read")
						.withResourceField(RD10, "description", "my desc10")
						.withResourceField(RD10, "moddate", 1500000000000L)
						.withResourceField(RD11, "name", "name11")
						.withResourceField(RD11, "public", true)
						.withResourceField(RD11, "narrname", null)
						.withResourceField(RD11, "narrcreate", null)
						.withResourceField(RD11, "perm", "None")
						.withResourceField(RD11, "description", "my desc11")
						.withResourceField(RD11, "moddate", 549864182000L)
						.build());
	}
	
	@Test
	public void getResourceInformationAdminPublicAnonUser() throws Exception {
		getResourceInformation(
		null,
		ResourceAccess.ADMINISTRATED_AND_PUBLIC,
		ResourceInformationSet.getBuilder(null)
				.withNonexistentResource(RD9)
				.withNonexistentResource(RD20)
				.withNonexistentResource(RD21)
				.withNonexistentResource(RD30)
				.withNonexistentResource(RD31)
				.withNonexistentResource(RD40)
				.withNonexistentResource(RD41)
				.withResourceField(RD10, "name", "name10")
				.withResourceField(RD10, "public", true)
				.withResourceField(RD10, "narrname", null)
				.withResourceField(RD10, "narrcreate", null)
				.withResourceField(RD10, "perm", "None")
				.withResourceField(RD10, "description", "my desc10")
				.withResourceField(RD10, "moddate", 1500000000000L)
				.withResourceField(RD11, "name", "name11")
				.withResourceField(RD11, "public", true)
				.withResourceField(RD11, "narrname", null)
				.withResourceField(RD11, "narrcreate", null)
				.withResourceField(RD11, "perm", "None")
				.withResourceField(RD11, "description", "my desc11")
				.withResourceField(RD11, "moddate", 549864182000L)
				.build());
	}
	
	@Test
	public void getResourceInformationAdmin() throws Exception {
		getResourceInformation(
				new UserName("user1"),
				ResourceAccess.ADMINISTRATED,
				ResourceInformationSet.getBuilder(new UserName("user1"))
						.withNonexistentResource(RD20)
						.withNonexistentResource(RD21)
						.withResourceField(RD3, "name", "name3")
						.withResourceField(RD3, "public", false)
						.withResourceField(RD3, "narrname", null)
						.withResourceField(RD3, "narrcreate", null)
						.withResourceField(RD3, "perm", "Own")
						.withResourceField(RD3, "description", "my desc")
						.withResourceField(RD3, "moddate", 1540606613000L)
						.withResourceField(RD5, "name", "name5")
						.withResourceField(RD5, "public", false)
						.withResourceField(RD5, "narrname", "narr_name")
						.withResourceField(RD5, "narrcreate", 1500000000000L)
						.withResourceField(RD5, "perm", "Admin")
						.withResourceField(RD5, "description", null)
						.withResourceField(RD5, "moddate", 0L)
						.build());
	}
	
	@Test
	public void getResourceInformationAdminAnonUser() throws Exception {
		getResourceInformation(
				null,
				ResourceAccess.ADMINISTRATED,
				ResourceInformationSet.getBuilder(null)
						.withNonexistentResource(RD20)
						.withNonexistentResource(RD21)
						.build());
	}
	
	private void getResourceInformation(
			final UserName user,
			final ResourceAccess access,
			final ResourceInformationSet expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(3)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(5)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "w", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(7)));

		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "w", "user2", "w")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(8)));

		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(9)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(10)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(11)));
		
		doThrow(new ServerException("Workspace 20 is deleted", -1, "n"))
				.when(c).administer(argThat(getPermissionsCommandMatcher(20)));
		doThrow(new ServerException("No workspace with id 21 exists", -1, "n"))
				.when(c).administer(argThat(getPermissionsCommandMatcher(21)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(30)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(31)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "r", "user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(40)));
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user2", "w", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(41)));

		doReturn(getWorkspaceInfoResponse(3, "name3", "user1", "2018-10-27T02:16:53+0000", false,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(3)));
		
		doReturn(getWorkspaceInfoResponse(5, "name5", "user3", "1970-01-01T00:00:00+0000", false,
				ImmutableMap.of(
						"is_temporary", "false",
						"narrative", "6",
						"narrative_nice_name", "narr_name")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(5)));

		doReturn(getWorkspaceInfoResponse(7, "name7", "user3", "1970-12-31T23:59:59+0000", false,
				ImmutableMap.of(
						"is_temporary", "true",
						"narrative_nice_name", "narr_name2")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(7)));
		
		doThrow(new ServerException("Workspace 8 is deleted", -1, "n"))
				.when(c).administer(argThat(getWSInfoCommandMatcher(8)));
		doThrow(new ServerException("No workspace with id 9 exists", -1, "n"))
				.when(c).administer(argThat(getWSInfoCommandMatcher(9)));
		
		doReturn(getWorkspaceInfoResponse(10, "name10", "user3", "2017-07-14T02:40:00+0000", true,
				ImmutableMap.of("is_temporary", "false")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(10)));

		doReturn(getWorkspaceInfoResponse(11, "name11", "user3", "1987-06-05T04:03:02+0000", true,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(11)));
		
		doReturn(getWorkspaceInfoResponse(30, "name30", "user3", "dontprocess", true,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(30)));

		doReturn(getWorkspaceInfoResponse(31, "name31", "user3", "dontprocess", true,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(31)));
		
		doReturn(getWorkspaceInfoResponse(40, "name30", "user3", "dontprocess", true,
				ImmutableMap.of(
						"is_temporary", "false",
						"narrative", "42")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(40)));

		doReturn(getWorkspaceInfoResponse(41, "name31", "user3", "dontprocess", true,
				ImmutableMap.of(
						"is_temporary", "false",
						"narrative", "43")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(41)));
		
		doReturn(new UObject("my desc")).when(c).administer(argThat(getWSDescCommandMatcher(3)));
		doReturn(null).when(c).administer(argThat(getWSDescCommandMatcher(5)));
		doReturn(new UObject("my desc7")).when(c).administer(argThat(getWSDescCommandMatcher(7)));
		doReturn(new UObject("my desc10")).when(c)
				.administer(argThat(getWSDescCommandMatcher(10)));
		doReturn(new UObject("my desc11")).when(c)
				.administer(argThat(getWSDescCommandMatcher(11)));
		doThrow(new ServerException("Workspace 30 is deleted", -1, "n")).when(c)
				.administer(argThat(getWSDescCommandMatcher(30)));
		doThrow(new ServerException("No workspace with id 31 exists", -1, "n")).when(c)
				.administer(argThat(getWSDescCommandMatcher(31)));
		doReturn(new UObject("no desc")).when(c).administer(argThat(getWSDescCommandMatcher(40)));
		doReturn(new UObject("no desc")).when(c).administer(argThat(getWSDescCommandMatcher(41)));

		doReturn(getObjectInfoResponse("2017-07-14T02:40:00+0000"))
				.when(c).administer(argThat(getObjectInfoCommandMatcher(5, 6, 1)));
		doThrow(new ServerException(
				"Object 42 cannot be accessed: Workspace 40 is deleted", -1, "n"))
				.when(c).administer(argThat(getObjectInfoCommandMatcher(40, 42, 1)));
		doThrow(new ServerException(
				"Object 43 cannot be accessed: No workspace with id 41 exists", -1, "n"))
				.when(c).administer(argThat(getObjectInfoCommandMatcher(41, 43, 1)));
		
		final ResourceInformationSet ri = h.getResourceInformation(
				user,
				set(new ResourceID("3"), new ResourceID("5"), new ResourceID("7"),
						new ResourceID("8"), new ResourceID("9"), new ResourceID("10"),
						new ResourceID("11"), new ResourceID("20"), new ResourceID("21"),
						new ResourceID("30"), new ResourceID("31"),
						new ResourceID("40"), new ResourceID("41")),
				access);
		
		assertThat("incorrect resources", ri, is(expected));
	}

	private UObjectArgumentMatcher getWSInfoCommandMatcher(final int wsid) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getWorkspaceInfo",
				"params", ImmutableMap.of("id", wsid)));
	}
	
	private UObjectArgumentMatcher getWSDescCommandMatcher(final int wsid) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getWorkspaceDescription",
				"params", ImmutableMap.of("id", wsid)));
	}
	
	private UObjectArgumentMatcher getObjectInfoCommandMatcher(
			final int wsid,
			final int objid,
			final int version) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "getObjectInfo",
				"params", ImmutableMap.of("objects", Arrays.asList(
						ImmutableMap.of(
								"wsid", wsid,
								"objid", objid,
								"ver", version)))));
	}
	
	private UObject getWorkspaceInfoResponse(
			final int id,
			final String name,
			final String userName,
			final String timestamp,
			final boolean isPublic,
			final Map<String, String> meta) {
		return new UObject(new Tuple9<Long, String, String, String, Long, String, String, String,
				Map<String, String>>()
				.withE1((long) id)
				.withE2(name)
				.withE4(timestamp)
				.withE3(userName)
				.withE7(isPublic ? "r" : "n")
				.withE9(meta));
		// other fields are currently unused in the handler
	}
	
	private UObject getObjectInfoResponse(
			final String timestamp) {
		return new UObject(ImmutableMap.of(
				"infos", Arrays.asList(new Tuple11<Long, String, String, String, Long, String,
						Long, String,String, Long, Map<String, String>>()
						.withE4(timestamp))));
		// other fields are currently unused in the handler
	}
	
	@Test
	public void getResourceInformationFailBadArgs() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		final ResourceAccess a = ResourceAccess.ALL;
		
		failGetResourceInfo(h, null, a, new NullPointerException("resources"));
		failGetResourceInfo(h, set(new ResourceID("1"), null), a,
				new NullPointerException("Null item in collection resources"));
		failGetResourceInfo(h, set(), null, new NullPointerException("access"));
		failGetResourceInfo(h, set(new ResourceID("bar")), a,
				new IllegalResourceIDException("bar"));
	}
	
	@Test
	public void getResourceInformationFailPermsOtherServerException() throws Exception {
		failGetResourceInfoOnPermissionsCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getResourceInformationFailPermsJsonClientException() throws Exception {
		failGetResourceInfoOnPermissionsCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getResourceInformationFailPermsIOException() throws Exception {
		failGetResourceInfoOnPermissionsCall(
				new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	@Test
	public void getResourceInformationFailPermsIllegalStateException() throws Exception {
		failGetResourceInfoOnPermissionsCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://baz.com"));
	}
	
	private void failGetResourceInfoOnPermissionsCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://baz.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(thrown);
		
		failGetResourceInfo(h, set(new ResourceID("24")), ResourceAccess.ALL, expected);
	}
	
	@Test
	public void getResourceInformationFailGetWSOtherServerException() throws Exception {
		failGetResourceInfoOnGetWSCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetWSJsonClientException() throws Exception {
		failGetResourceInfoOnGetWSCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetWSIOException() throws Exception {
		failGetResourceInfoOnGetWSCall(
				new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetWSIllegalStateException() throws Exception {
		failGetResourceInfoOnGetWSCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	private void failGetResourceInfoOnGetWSCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://bat.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(24)));

		doThrow(thrown).when(c).administer(argThat(getWSInfoCommandMatcher(24)));
		
		failGetResourceInfo(h, set(new ResourceID("24")), ResourceAccess.ALL, expected);
	}
	
	@Test
	public void getResourceInformationFailGetDescriptionOtherServerException() throws Exception {
		failGetResourceInfoOnGetDescriptionCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetDescriptionJsonClientException() throws Exception {
		failGetResourceInfoOnGetDescriptionCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetDescriptionIOException() throws Exception {
		failGetResourceInfoOnGetDescriptionCall(
				new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetDescriptionIllegalStateException() throws Exception {
		failGetResourceInfoOnGetDescriptionCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	private void failGetResourceInfoOnGetDescriptionCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://bat.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(24)));

		doReturn(getWorkspaceInfoResponse(3, "name3", "user1", "2018-10-27T02:16:53+0000", false,
				Collections.emptyMap()))
				.when(c).administer(argThat(getWSInfoCommandMatcher(24)));
		
		doThrow(thrown).when(c).administer(argThat(getWSDescCommandMatcher(24)));
		
		failGetResourceInfo(h, set(new ResourceID("24")), ResourceAccess.ALL, expected);
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoObjectDeleted() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new ServerException("Object 4 (name foo) in workspace 24 (name name3) " +
						"has been deleted", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoNoObject() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new ServerException("No object with id 4 exists in workspace 4 (name foo)",
						-1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoOtherServerException() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoJsonClientException() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoIOException() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	@Test
	public void getResourceInformationFailGetObjectInfoIllegalStateException() throws Exception {
		failGetResourceInfoOnGetObjectInfoCall(
				new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://bat.com"));
	}
	
	private void failGetResourceInfoOnGetObjectInfoCall(
			final Exception thrown,
			final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://bat.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		doReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(
				ImmutableMap.of("user1", "a", "*", "r")))))
				.when(c).administer(argThat(getPermissionsCommandMatcher(24)));

		doReturn(getWorkspaceInfoResponse(24, "name3", "user1", "2018-10-27T02:16:53+0000", false,
				ImmutableMap.of(
						"is_temporary", "false",
						"narrative", "4")))
				.when(c).administer(argThat(getWSInfoCommandMatcher(24)));
		
		doReturn(new UObject("my desc")).when(c)
				.administer(argThat(getWSDescCommandMatcher(24)));
		
		doThrow(thrown).when(c).administer(argThat(getObjectInfoCommandMatcher(24, 4, 1)));
		
		failGetResourceInfo(h, set(new ResourceID("24")), ResourceAccess.ALL, expected);
	}
	
	private void failGetResourceInfo(
			final SDKClientWorkspaceHandler h,
			final Set<ResourceID> ids,
			final ResourceAccess access,
			final Exception expected) {
		try {
			// no way to cause a fail via user or adminOnly param
			h.getResourceInformation(null, ids, access);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getAdministratedResources() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getListWorkspaceIDsCommandMatcher("user"))))
				.thenReturn(new UObject(ImmutableMap.of("workspaces", Arrays.asList(
						4L, 8L, 86L))));

		assertThat("incorrect ids", h.getAdministratedResources(new UserName("user")),
				is(set(new ResourceAdministrativeID("4"), new ResourceAdministrativeID("8"),
						new ResourceAdministrativeID("86"))));
	}
	
	@Test
	public void getAdministratedResourcesFailNull() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failGetAdministratedResources(h, null, new NullPointerException("user"));
	}
	
	@Test
	public void getAdministratedResourcesFailIOException() throws Exception {
		getAdministratedResourcesFail(new IOException("oh dookybutts"),
				new ResourceHandlerException(
						"Error contacting workspace at http://nudewombats.com"));
	}

	@Test
	public void getAdministratedResourcesFailJSONClientException() throws Exception {
		getAdministratedResourcesFail(new JsonClientException("oh dookybutts"),
				new ResourceHandlerException(
						"Error contacting workspace at http://nudewombats.com"));
	}

	private void getAdministratedResourcesFail(final Exception thrown, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://nudewombats.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getListWorkspaceIDsCommandMatcher("user"))))
				.thenThrow(thrown);

		failGetAdministratedResources(h, new UserName("user"), expected);
	}
	
	private void failGetAdministratedResources(
			final SDKClientWorkspaceHandler h,
			final UserName user,
			final Exception expected) {
		try {
			h.getAdministratedResources(user);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	private UObjectArgumentMatcher getListWorkspaceIDsCommandMatcher(final String user) {
		return new UObjectArgumentMatcher(ImmutableMap.of(
				"command", "listWorkspaceIDs",
				"user", user,
				"params", ImmutableMap.of("perm", "a")));
	}
	
	@Test
	public void getAdministrators() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(ImmutableMap.of(
						"user1", "a", "user2", "w", "user3", "r", "user4", "a", "*", "r")))));
		
		assertThat("incorrect admins", h.getAdministrators(new ResourceID("24")), is(
				set(new UserName("user1"), new UserName("user4"))));
	}
	
	@Test
	public void getAdministratorsFailBadArgs() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failGetAdministrators(h, null, new NullPointerException("resource"));
		failGetAdministrators(h, new ResourceID("foo"), new IllegalResourceIDException("foo"));
	}
	
	@Test
	public void getAdministratorsFailDeletedWS() throws Exception {
		failGetAdministrators(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void getAdministratorsFailMissingWS() throws Exception {
		failGetAdministrators(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void getAdministratorsFailOtherServerException() throws Exception {
		failGetAdministrators(new ServerException("You pootied real bad I can smell it", -1, "n"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void getAdministratorsFailJsonClientException() throws Exception {
		failGetAdministrators(new JsonClientException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void getAdministratorsFailIOException() throws Exception {
		failGetAdministrators(new IOException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void getAdministratorsFailIllegalStateException() throws Exception {
		failGetAdministrators(new IllegalStateException("You pootied real bad I can smell it"),
				new ResourceHandlerException("Error contacting workspace at http://foo.com"));
	}
	
	@Test
	public void getAdministratorsFailMissingUser() throws Exception {
		// can't be null, since JSON doesn't allow null keys
		getAdministratorsFailIllegalUser("   \t   ", new RuntimeException(
				"Unexpected illegal user name returned from workspace: " +
				"30000 Missing input parameter: user name"));
	}
	
	@Test
	public void getAdministratorsFailIllegalUser() throws Exception {
		getAdministratorsFailIllegalUser("illegal*user", new RuntimeException(
				"Unexpected illegal user name returned from workspace: " +
				"30010 Illegal user name: Illegal character in user name illegal*user: *"));
	}

	private void getAdministratorsFailIllegalUser(final String user, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(ImmutableMap.of(
						user, "a", "user2", "w", "user3", "r", "user4", "a", "*", "r")))));
		
		failGetAdministrators(h, new ResourceID("24"), expected);
	}
	
	private void failGetAdministrators(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(exception);
		
		failGetAdministrators(h, new ResourceID("24"), expected);
	}
	
	private void failGetAdministrators(
			final SDKClientWorkspaceHandler h,
			final ResourceID id,
			final Exception expected) {
		try {
			h.getAdministrators(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void setReadPermission() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(56))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(ImmutableMap.of(
				"user1", "a", "user2", "w", "user3", "r", "user4", "a")))));
		
		new SDKClientWorkspaceHandler(c).setReadPermission(
				new ResourceID("56"), new UserName("user5"));
		
		verify(c).administer(argThat(setPermissionsCommandMatcher(56, "user5", "r")));
	}
	
	@Test
	public void setReadPermissionsNoopHasAdmin() throws Exception {
		setReadPermissionNoop(ImmutableMap.of("user", "a"), "user");
	}
	
	@Test
	public void setReadPermissionsNoopHasWrite() throws Exception {
		setReadPermissionNoop(ImmutableMap.of("user", "w"), "user");
	}
	
	@Test
	public void setReadPermissionsNoopHasRead() throws Exception {
		setReadPermissionNoop(ImmutableMap.of("user", "r"), "user");
	}
	
	@Test
	public void setReadPermissionsNoopPublicRead() throws Exception {
		setReadPermissionNoop(ImmutableMap.of("*", "r"), "user");
	}
	
	private void setReadPermissionNoop(final Map<String, String> returnedPerms, final String user)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(56))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(returnedPerms))));
		
		new SDKClientWorkspaceHandler(c).setReadPermission(
				new ResourceID("56"), new UserName(user));
		
		verify(c, never()).administer(argThat(setPermissionsCommandMatcher(56, user, "r")));
	}
	
	@Test
	public void setReadPermissionFailBadArgs() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		failSetReadPermission(h, null, new UserName("n"), new NullPointerException("resource"));
		failSetReadPermission(h, new ResourceID("24"), null, new NullPointerException("user"));
		failSetReadPermission(h, new ResourceID("whoo"), new UserName("n"),
				new IllegalResourceIDException("whoo"));
	}
	
	// I'm not bothering to test all the getPermissionsMass failure modes again, they're identical
	// to the tests for getAdministrators and isAdmin. If you make changes to the fn, be sure
	// to add tests.
	// Just test that a deleted workspace exception throws an exception (e.g. test the boolean
	// in the getPermissions call is true.
	
	@Test
	public void setReadPermissionsFailDeletedWS() throws Exception {
		failSetReadPermissionAtGet(new ServerException("Workspace 24 is deleted", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	@Test
	public void setReadPermissionsFailMissingWS() throws Exception {
		failSetReadPermissionAtGet(new ServerException("No workspace with id 24 exists", -1, "n"),
				new NoSuchResourceException("24"));
	}
	
	private void failSetReadPermissionAtGet(final Exception exception, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		final SDKClientWorkspaceHandler h = new SDKClientWorkspaceHandler(c);
		
		when(c.administer(argThat(getPermissionsCommandMatcher(24)))).thenThrow(exception);
		
		failSetReadPermission(h, new ResourceID("24"), new UserName("foo"), expected);
	}
	
	
	@Test
	public void setReadPermissionFailIOException() throws Exception {
		setReadPermissionFail(new IOException("foo"), new ResourceHandlerException(
				"Error contacting workspace at http://hotchacha.com"));
	}
	
	@Test
	public void setReadPermissionFailJsonClientException() throws Exception {
		setReadPermissionFail(new JsonClientException("foo"), new ResourceHandlerException(
				"Error contacting workspace at http://hotchacha.com"));
	}

	private void setReadPermissionFail(final Exception thrown, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		
		when(c.ver()).thenReturn(MIN_WS_VER);
		when(c.getURL()).thenReturn(new URL("http://hotchacha.com"));
		
		when(c.administer(argThat(getPermissionsCommandMatcher(56))))
				.thenReturn(new UObject(ImmutableMap.of("perms", Arrays.asList(ImmutableMap.of(
				"user1", "a", "user2", "w", "user3", "r", "user4", "a")))));
		
		doThrow(thrown).when(c)
				.administer(argThat(setPermissionsCommandMatcher(56, "user5", "r")));
		
		failSetReadPermission(new SDKClientWorkspaceHandler(c), new ResourceID("56"),
				new UserName("user5"), expected);
	}
	
	private void failSetReadPermission(
			final SDKClientWorkspaceHandler h,
			final ResourceID id,
			final UserName n,
			final Exception expected) {
		try {
			h.setReadPermission(id, n);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getDescriptor() throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		when(c.ver()).thenReturn(MIN_WS_VER);
		
		final ResourceDescriptor d = new SDKClientWorkspaceHandler(c)
				.getDescriptor(new ResourceID("82"));
		
		assertThat("incorrect descriptor", d, is(new ResourceDescriptor(
				new ResourceAdministrativeID("82"), new ResourceID("82"))));
	}
	
	@Test
	public void getDecriptorFailBadArgs() throws Exception {
		failGetDescriptor(null, new NullPointerException("resource"));
		failGetDescriptor(new ResourceID("foo"), new IllegalResourceIDException("foo"));
		
	}
	
	private void failGetDescriptor(final ResourceID rid, final Exception expected)
			throws Exception {
		final WorkspaceClient c = mock(WorkspaceClient.class);
		when(c.ver()).thenReturn(MIN_WS_VER);
		try {
			new SDKClientWorkspaceHandler(c).getDescriptor(rid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
