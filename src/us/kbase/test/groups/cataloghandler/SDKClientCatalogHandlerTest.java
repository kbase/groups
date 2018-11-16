package us.kbase.test.groups.cataloghandler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import us.kbase.catalog.BasicModuleInfo;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ListModuleParams;
import us.kbase.catalog.ModuleInfo;
import us.kbase.catalog.ModuleVersion;
import us.kbase.catalog.SelectModuleVersion;
import us.kbase.catalog.SelectOneModuleParams;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.test.TestException;
import us.kbase.groups.cataloghandler.SDKClientCatalogHandler;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.catalog.CatalogModule;
import us.kbase.groups.core.exceptions.CatalogHandlerException;
import us.kbase.groups.core.exceptions.NoSuchCatalogEntryException;
import us.kbase.test.groups.TestCommon;

public class SDKClientCatalogHandlerTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static boolean DEBUG = true;
	
	private static class SelectOneModuleParamsMatcher implements
			ArgumentMatcher<SelectOneModuleParams> {

		private final String moduleName;

		public SelectOneModuleParamsMatcher(final String moduleName) {
			this.moduleName = moduleName;
		}
		
		@Override
		public boolean matches(final SelectOneModuleParams somp) {
			final Map<String, Object> params;
			try {
				final String p = MAPPER.writeValueAsString(somp);
				params = MAPPER.readValue(p, new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				throw new TestException(e.getMessage(), e);
			}
			final Map<String, String> expected = ImmutableMap.of("module_name", moduleName);
			if (params.equals(expected)) {
				return true;
			} else {
				if (DEBUG) {
					System.out.println(String.format("Expected:\n%s\nGot:\n%s\n",
							expected, params));
				}
				return false;
			}
		}
	}
	
	// pretty similar w/ the above, maybe factor together
	private static class SelectModuleVersionParamsMatcher implements
			ArgumentMatcher<SelectModuleVersion> {
		
		private final String moduleName;
		private final String version;
		
		public SelectModuleVersionParamsMatcher(final String moduleName, final String version) {
			this.moduleName = moduleName;
			this.version = version;
		}
		
		@Override
		public boolean matches(final SelectModuleVersion smv) {
			final Map<String, Object> params;
			try {
				final String p = MAPPER.writeValueAsString(smv);
				params = MAPPER.readValue(p, new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				throw new TestException(e.getMessage(), e);
			}
			final Map<String, String> expected = ImmutableMap.of(
					"module_name", moduleName,
					"version", version);
			if (params.equals(expected)) {
				return true;
			} else {
				if (DEBUG) {
					System.out.println(String.format("Expected:\n%s\nGot:\n%s\n",
							expected, params));
				}
				return false;
			}
		}
	}
	
	// pretty similar w/ the above, maybe factor together
	private static class ListModuleParamsMatcher implements ArgumentMatcher<ListModuleParams> {
		
		private final String owner;
		private final int includeDisabled;
		
		public ListModuleParamsMatcher(final String owner, final int includeDisabled) {
			this.owner = owner;
			this.includeDisabled = includeDisabled;
		}
		
		@Override
		public boolean matches(final ListModuleParams smv) {
			final Map<String, Object> params;
			try {
				final String p = MAPPER.writeValueAsString(smv);
				params = MAPPER.readValue(p, new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				throw new TestException(e.getMessage(), e);
			}
			final Map<String, Object> expected = ImmutableMap.of(
					"owners", Arrays.asList(owner),
					"include_disabled", includeDisabled);
			if (params.equals(expected)) {
				return true;
			} else {
				if (DEBUG) {
					System.out.println(String.format("Expected:\n%s\nGot:\n%s\n",
							expected, params));
				}
				return false;
			}
		}
	}
	
	@Test
	public void constructFailNull() throws Exception {
		failConstruct(null, new NullPointerException("client"));
	}
	
	@Test
	public void constructFailIOException() throws Exception {
		failConstruct(new IOException("foo"));
	}
	
	@Test
	public void constructFailJsonClientException() throws Exception {
		failConstruct(new JsonClientException("foo"));
	}

	private void failConstruct(final Exception e) throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		when(c.version()).thenThrow(e);
		
		failConstruct(c, new CatalogHandlerException(
				"Error contacting catalog service at http://foo.com"));
	}
	
	private void failConstruct(final CatalogClient client, final Exception expected) {
		try {
			new SDKClientCatalogHandler(client);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isOwnerTrue() throws Exception {
		isOwner("u1", true);
	}
	
	@Test
	public void isOwnerFalse() throws Exception {
		isOwner("u3", false);
	}

	private void isOwner(final String name, final boolean expected) throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2")));
		
		assertThat("incorrect is owner", new SDKClientCatalogHandler(c)
				.isOwner(new CatalogModule("modname"), new UserName(name)), is(expected));
	}
	
	@Test
	public void isOwnerFailNulls() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		failIsOwner(c, null, new UserName("u"), new NullPointerException("module"));
		failIsOwner(c, new CatalogModule("m"), null, new NullPointerException("user"));
	}
	
	@Test
	public void isOwnerFailIOException() throws Exception {
		failIsOwner(new IOException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://foo.com"));
	}
	
	@Test
	public void isOwnerFailJsonClientException() throws Exception {
		failIsOwner(new JsonClientException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://foo.com"));
	}
	
	@Test
	public void isOwnerFailNoModule() throws Exception {
		failIsOwner(new JsonClientException("foo module/repo is not registered bar"),
				new NoSuchCatalogEntryException("modname"));
	}

	private void failIsOwner(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenThrow(e);

		failIsOwner(c, new CatalogModule("modname"), new UserName("foo"), expected);
	}
	
	private void failIsOwner(
			final CatalogClient cli,
			final CatalogModule mod,
			final UserName u,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(cli).isOwner(mod, u);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getOwnersEmpty() throws Exception {
		getOwners(Collections.emptyList(), set());
	}
	
	@Test
	public void getOwners() throws Exception {
		getOwners(Arrays.asList("u1", "u2"), set(new UserName("u1"), new UserName("u2")));
	}

	private void getOwners(final List<String> returned, final Set<UserName> expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(returned));
		
		assertThat("incorrect owners", new SDKClientCatalogHandler(c)
				.getOwners(new CatalogModule("modname")),
				is(expected));
	}
	
	@Test
	public void getOwnersFailNull() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		failGetOwners(c, null, new NullPointerException("module"));
	}
	
	@Test
	public void getOwnersFailIOException() throws Exception {
		failGetOwners(new IOException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://bar.com"));
	}
	
	@Test
	public void getOwnersFailJsonClientException() throws Exception {
		failGetOwners(new JsonClientException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://bar.com"));
	}
	
	@Test
	public void getOwnersFailNoModule() throws Exception {
		failGetOwners(new JsonClientException("foo module/repo is not registered bar"),
				new NoSuchCatalogEntryException("modname2"));
	}
	
	@Test
	public void getOwnersFailBadUserNull() throws Exception {
		getOwnersFailBadUser(null, new CatalogHandlerException(
				"Illegal user name returned from catalog: null"));
	}
	
	@Test
	public void getOwnersFailBadUserControlChars() throws Exception {
		getOwnersFailBadUser("foo\tbar", new CatalogHandlerException(
				"Illegal user name returned from catalog: foo\tbar"));
	}

	private void getOwnersFailBadUser(final String badUser, final CatalogHandlerException expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname2"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("foo", badUser)));
		
		failGetOwners(c, new CatalogModule("modname2"), expected);
	}
	
	private void failGetOwners(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://bar.com"));
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname2"))))
				.thenThrow(e);

		failGetOwners(c, new CatalogModule("modname2"), expected);
	}
	
	private void failGetOwners(
			final CatalogClient cli,
			final CatalogModule mod,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(cli).getOwners(mod);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isMethodExtantNarrative() throws Exception {
		isMethodExtant("modname.n2", true);
	}
	
	@Test
	public void isMethodExtantNarrativeFalse() throws Exception {
		isMethodExtant("modname.n3", false);
	}
	
	@Test
	public void isMethodExtantLocal() throws Exception {
		isMethodExtant("modname.f1", true);
	}
	
	@Test
	public void isMethodExtantLocalFalse() throws Exception {
		isMethodExtant("modname.f4", false);
	}
	
	@Test
	public void isMethodExtantFailNoModule() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname5", "release"))))
				.thenThrow(new JsonClientException("foo Module cannot be found bar"));

		assertThat("incorrect extant", new SDKClientCatalogHandler(c)
				.isMethodExtant(new CatalogMethod("modname5.yay")), is(false));
	}

	private void isMethodExtant(final String method, final boolean expected) throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		final ModuleVersion mv = new ModuleVersion();
		mv.setAdditionalProperties("narrative_methods", Arrays.asList("n1", "n2"));
		mv.setAdditionalProperties("local_functions", Arrays.asList("f1", "f2"));
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname", "release")))).thenReturn(mv);
		
		assertThat("incorrect extant", new SDKClientCatalogHandler(c)
				.isMethodExtant(new CatalogMethod(method)), is(expected));
	}

	@Test
	public void isMethodExtantFailNull() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		failIsMethodExtant(c, null, new NullPointerException("method"));
	}
	
	@Test
	public void isMethodExtantFailIOException() throws Exception {
		failIsMethodExtant(new IOException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://whee.com"));
	}
	
	@Test
	public void isMethodExtantFailJsonClientException() throws Exception {
		failIsMethodExtant(new JsonClientException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://whee.com"));
	}
	
	private void failIsMethodExtant(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://whee.com"));
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname4", "release")))).thenThrow(e);

		failIsMethodExtant(c, new CatalogMethod("modname4.foo"), expected);
	}
	
	private void failIsMethodExtant(
			final CatalogClient c,
			final CatalogMethod m,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(c).isMethodExtant(m);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void getOwnedModulesEmpty() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect modules", new SDKClientCatalogHandler(c)
				.getOwnedModules(new UserName("u1")), is(set()));
	}
	
	@Test
	public void getOwnedModules() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Arrays.asList(
						new BasicModuleInfo().withModuleName("m1"),
						new BasicModuleInfo().withModuleName("m2"),
						new BasicModuleInfo().withModuleName("m3")));
		
		assertThat("incorrect modules", new SDKClientCatalogHandler(c)
				.getOwnedModules(new UserName("u1")), is(set(new CatalogModule("m1"),
						new CatalogModule("m2"), new CatalogModule("m3"))));
	}
	
	@Test
	public void getOwnedModulesFailNull() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		failGetOwnedModules(c, null, new NullPointerException("user"));
	}
	
	@Test
	public void getOwnedModulesFailNullModule() throws Exception {
		failGetOwnedModules((String) null, new CatalogHandlerException(
				"Illegal module name returned from catalog: null"));
	}
	
	@Test
	public void getOwnedModulesFailModuleControlChars() throws Exception {
		failGetOwnedModules("foo\tbar", new CatalogHandlerException(
				"Illegal module name returned from catalog: foo\tbar"));
	}
	
	@Test
	public void getOwnedModulesFailIOException() throws Exception {
		failGetOwnedModules(new IOException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://whoo.com"));
	}
	
	@Test
	public void getOwnedModulesFailJsonClientException() throws Exception {
		failGetOwnedModules(new JsonClientException("foo"), new CatalogHandlerException(
				"Error contacting catalog service at http://whoo.com"));
	}

	private void failGetOwnedModules(final Exception thrown, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://whoo.com"));
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenThrow(thrown);
		
		failGetOwnedModules(c, new UserName("u1"), expected);
	}
	
	private void failGetOwnedModules(final String badModule, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Arrays.asList(
						new BasicModuleInfo().withModuleName("m1"),
						new BasicModuleInfo().withModuleName(badModule)));
		
		failGetOwnedModules(c, new UserName("u1"), expected);
	}
	
	private void failGetOwnedModules(
			final CatalogClient c,
			final UserName n,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(c).getOwnedModules(n);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
