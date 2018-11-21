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
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.test.groups.MapBuilder;
import us.kbase.test.groups.TestCommon;

public class SDKClientCatalogHandlerTest {

	private static boolean DEBUG = true;

	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final Map<String, String> BAD_NAMES = MapBuilder.<String, String>newHashMap()
			.with("m.   \t  ", "m.")
			.with(" \t  .n", ".n")
			.with("m  .n", "m  .n" )
			.with("m.    n", "m.    n")
			.with("m  .    n", "m  .    n" )
			.with("m.n.o", "m.n.o")
			.with("m", "m")
			.with(".", ".")
			.build();
	
	
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
		
		failConstruct(c, new ResourceHandlerException(
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
	public void isAdministratorTrue() throws Exception {
		isAdministrator("u1", true);
	}
	
	@Test
	public void isAdministratorFalse() throws Exception {
		isAdministrator("u3", false);
	}

	private void isAdministrator(final String name, final boolean expected) throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2")));
		
		assertThat("incorrect is owner", new SDKClientCatalogHandler(c)
				.isAdministrator(new ResourceID("modname.mod"), new UserName(name)), is(expected));
	}
	
	@Test
	public void isAdministratorFailBadArgs() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		failIsAdministrator(c, null, new UserName("u"), new NullPointerException("resource"));
		failIsAdministrator(c, new ResourceID("m.n"), null, new NullPointerException("user"));
		
		for (final String n: BAD_NAMES.keySet()) {
			failIsAdministrator(c, new ResourceID(n), new UserName("u"),
					new IllegalResourceIDException("Illegal catalog method name: " +
							BAD_NAMES.get(n)));
		}
	}
	
	@Test
	public void isAdministratorFailIOException() throws Exception {
		failIsAdministrator(new IOException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://foo.com"));
	}
	
	@Test
	public void isAdministratorFailJsonClientException() throws Exception {
		failIsAdministrator(new JsonClientException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://foo.com"));
	}
	
	@Test
	public void isAdministratorFailNoModule() throws Exception {
		failIsAdministrator(new JsonClientException("foo module/repo is not registered bar"),
				new NoSuchResourceException("modname.mod"));
	}

	private void failIsAdministrator(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://foo.com"));
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenThrow(e);

		failIsAdministrator(c, new ResourceID("modname.mod"), new UserName("foo"), expected);
	}
	
	private void failIsAdministrator(
			final CatalogClient cli,
			final ResourceID mod,
			final UserName u,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(cli).isAdministrator(mod, u);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getAdministratorsEmpty() throws Exception {
		getAdministrators(Collections.emptyList(), set());
	}
	
	@Test
	public void getAdministrators() throws Exception {
		getAdministrators(Arrays.asList("u1", "u2"), set(new UserName("u1"), new UserName("u2")));
	}

	private void getAdministrators(final List<String> returned, final Set<UserName> expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(returned));
		
		assertThat("incorrect owners", new SDKClientCatalogHandler(c)
				.getAdminstrators(new ResourceID("modname.m")),
				is(expected));
	}
	
	@Test
	public void getAdministratorsFailBadArgs() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		failGetAdministrators(c, null, new NullPointerException("resource"));
		
		for (final String n: BAD_NAMES.keySet()) {
			failGetAdministrators(c, new ResourceID(n),
					new IllegalResourceIDException("Illegal catalog method name: " +
							BAD_NAMES.get(n)));
		}
	}
	
	@Test
	public void getAdministratorsFailIOException() throws Exception {
		failGetAdministrators(new IOException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://bar.com"));
	}
	
	@Test
	public void getAdministratorsFailJsonClientException() throws Exception {
		failGetAdministrators(new JsonClientException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://bar.com"));
	}
	
	@Test
	public void getAdministratorsFailNoModule() throws Exception {
		failGetAdministrators(new JsonClientException("foo module/repo is not registered bar"),
				new NoSuchResourceException("modname2.m"));
	}
	
	@Test
	public void getAdministratorsFailBadUserNull() throws Exception {
		getAdministratorsFailBadUser(null, new ResourceHandlerException(
				"Illegal user name returned from catalog: null"));
	}
	
	@Test
	public void getAdministratorsFailBadUserControlChars() throws Exception {
		getAdministratorsFailBadUser("foo\tbar", new ResourceHandlerException(
				"Illegal user name returned from catalog: foo\tbar"));
	}

	private void getAdministratorsFailBadUser(
			final String badUser,
			final ResourceHandlerException expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname2"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("foo", badUser)));
		
		failGetAdministrators(c, new ResourceID("modname2.m"), expected);
	}
	
	private void failGetAdministrators(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://bar.com"));
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname2"))))
				.thenThrow(e);

		failGetAdministrators(c, new ResourceID("modname2.m"), expected);
	}
	
	private void failGetAdministrators(
			final CatalogClient cli,
			final ResourceID mod,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(cli).getAdminstrators(mod);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isResourceExtantNarrative() throws Exception {
		isResourceExtant("modname.n2", true);
	}
	
	@Test
	public void isResourceExtantNarrativeFalse() throws Exception {
		isResourceExtant("modname.n3", false);
	}
	
	@Test
	public void isResourceExtantLocal() throws Exception {
		isResourceExtant("modname.f1", true);
	}
	
	@Test
	public void isResourceExtantLocalFalse() throws Exception {
		isResourceExtant("modname.f4", false);
	}
	
	@Test
	public void isResourceExtantFailNoModule() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname5", "release"))))
				.thenThrow(new JsonClientException("foo Module cannot be found bar"));

		assertThat("incorrect extant", new SDKClientCatalogHandler(c)
				.isResourceExtant(new ResourceID("modname5.yay")), is(false));
	}

	private void isResourceExtant(final String method, final boolean expected) throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		final ModuleVersion mv = new ModuleVersion();
		mv.setAdditionalProperties("narrative_methods", Arrays.asList("n1", "n2"));
		mv.setAdditionalProperties("local_functions", Arrays.asList("f1", "f2"));
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname", "release")))).thenReturn(mv);
		
		assertThat("incorrect extant", new SDKClientCatalogHandler(c)
				.isResourceExtant(new ResourceID(method)), is(expected));
	}

	@Test
	public void isResourceExtantFailNull() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		failIsResourceExtant(c, null, new NullPointerException("resource"));
	}
	
	@Test
	public void isResourceExtantFailIOException() throws Exception {
		failIsResourceExtant(new IOException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://whee.com"));
	}
	
	@Test
	public void isResourceExtantFailJsonClientException() throws Exception {
		failIsResourceExtant(new JsonClientException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://whee.com"));
	}
	
	private void failIsResourceExtant(final Exception e, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://whee.com"));
		
		when(c.getModuleVersion(argThat(
				new SelectModuleVersionParamsMatcher("modname4", "release")))).thenThrow(e);

		failIsResourceExtant(c, new ResourceID("modname4.foo"), expected);
	}
	
	private void failIsResourceExtant(
			final CatalogClient c,
			final ResourceID m,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(c).isResourceExtant(m);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void getAdministratedResourcesEmpty() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Collections.emptyList());
		
		assertThat("incorrect modules", new SDKClientCatalogHandler(c)
				.getAdministratedResources(new UserName("u1")), is(set()));
	}
	
	@Test
	public void getAdministratedResources() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Arrays.asList(
						new BasicModuleInfo().withModuleName("m1"),
						new BasicModuleInfo().withModuleName("m2"),
						new BasicModuleInfo().withModuleName("m3")));
		
		assertThat("incorrect modules", new SDKClientCatalogHandler(c)
				.getAdministratedResources(new UserName("u1")), is(set(
						new ResourceAdministrativeID("m1"), new ResourceAdministrativeID("m2"),
						new ResourceAdministrativeID("m3"))));
	}
	
	@Test
	public void getAdministratedResourcesFailNull() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		failGetAdministratedResources(c, null, new NullPointerException("user"));
	}
	
	@Test
	public void getAdministratedResourcesFailNullModule() throws Exception {
		failGetAdministratedResources((String) null, new ResourceHandlerException(
				"Illegal module name returned from catalog: null"));
	}
	
	@Test
	public void getAdministratedResourcesFailModuleControlChars() throws Exception {
		failGetAdministratedResources("foo\tbar", new ResourceHandlerException(
				"Illegal module name returned from catalog: foo\tbar"));
	}
	
	@Test
	public void getAdministratedResourcesFailIOException() throws Exception {
		failGetAdministratedResources(new IOException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://whoo.com"));
	}
	
	@Test
	public void getAdministratedResourcesFailJsonClientException() throws Exception {
		failGetAdministratedResources(new JsonClientException("foo"), new ResourceHandlerException(
				"Error contacting catalog service at http://whoo.com"));
	}

	private void failGetAdministratedResources(final Exception thrown, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getURL()).thenReturn(new URL("http://whoo.com"));
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenThrow(thrown);
		
		failGetAdministratedResources(c, new UserName("u1"), expected);
	}
	
	private void failGetAdministratedResources(final String badModule, final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.listBasicModuleInfo(argThat(new ListModuleParamsMatcher("u1", 1))))
				.thenReturn(Arrays.asList(
						new BasicModuleInfo().withModuleName("m1"),
						new BasicModuleInfo().withModuleName(badModule)));
		
		failGetAdministratedResources(c, new UserName("u1"), expected);
	}
	
	private void failGetAdministratedResources(
			final CatalogClient c,
			final UserName n,
			final Exception expected) {
		try {
			new SDKClientCatalogHandler(c).getAdministratedResources(n);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void setReadPermission() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		new SDKClientCatalogHandler(c).setReadPermission(null, null);
		// nothing to test other than it doesn't fail
	}
	
	@Test
	public void getDescriptor() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		final ResourceDescriptor d = new SDKClientCatalogHandler(c)
				.getDescriptor(new ResourceID(" \t  mod.meth2     \t "));
		
		assertThat("incorrect descriptor", d,
				is(new ResourceDescriptor(
						new ResourceAdministrativeID("mod"),
						new ResourceID("mod.meth2"))));
		
	}
	
	@Test
	public void getDescriptorFailBadArgs() throws Exception {
		failGetDescriptor(null, new NullPointerException("resource"));
	
		for (final String n: BAD_NAMES.keySet()) {
			failGetDescriptor(new ResourceID(n),
					new IllegalResourceIDException("Illegal catalog method name: " +
							BAD_NAMES.get(n)));
		}
	}

	private void failGetDescriptor(final ResourceID r, final Exception expected) {
		final CatalogClient c = mock(CatalogClient.class);
		try {
			new SDKClientCatalogHandler(c).getDescriptor(r);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getResourceInformationMinimal() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		final ResourceInformationSet ris = new SDKClientCatalogHandler(c)
				.getResourceInformation(null, set(), true);
		
		assertThat("incorrect infos", ris, is(ResourceInformationSet.getBuilder(null).build()));
	}
	
	@Test
	public void getResourceInformationMaximal() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		final ResourceInformationSet ris = new SDKClientCatalogHandler(c)
				.getResourceInformation(new UserName("foo"),
						set(new ResourceID("foo.bar"), new ResourceID("x.y")),
						false);
		
		assertThat("incorrect infos", ris, is(ResourceInformationSet.getBuilder(
				new UserName("foo"))
				.withResourceDescriptor(new ResourceDescriptor(
						new ResourceAdministrativeID("foo"), new ResourceID("foo.bar")))
				.withResourceDescriptor(new ResourceDescriptor(
						new ResourceAdministrativeID("x"), new ResourceID("x.y")))
				.build()));
	}
	
	@Test
	public void getResourceInformationFailBadArgs() throws Exception {
		failGetResourceInformation(null, new NullPointerException("resources"));
		failGetResourceInformation(set(new ResourceID("i"), null),
				new NullPointerException("Null item in collection resources"));
		
		for (final String n: BAD_NAMES.keySet()) {
			failGetResourceInformation(set(new ResourceID("x.y"), new ResourceID(n)),
					new IllegalResourceIDException("Illegal catalog method name: " +
							BAD_NAMES.get(n)));
		}
	}
	
	private void failGetResourceInformation(
			final Set<ResourceID> resources,
			final Exception expected) {
		final CatalogClient c = mock(CatalogClient.class);
		try {
			new SDKClientCatalogHandler(c).getResourceInformation(null, resources, true);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
