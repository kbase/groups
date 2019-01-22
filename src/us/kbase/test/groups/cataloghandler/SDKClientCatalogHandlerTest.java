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
import us.kbase.catalog.ModuleVersionInfo;
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
		isAdministrator("u1", true, Collections.emptyList(), Arrays.asList("mod", "v"));
	}
	
	@Test
	public void isAdministratorFalse() throws Exception {
		isAdministrator("u3", false, Arrays.asList("x", "mod"), Collections.emptyList());
	}

	private void isAdministrator(
			final String name,
			final boolean expected,
			final List<String> narrmethods,
			final List<String> localmethods)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2"))
						.withRelease(getMVI(narrmethods, localmethods)));
		
		assertThat("incorrect is owner", new SDKClientCatalogHandler(c)
				.isAdministrator(new ResourceID("modname.mod"), new UserName(name)), is(expected));
	}
	
	public ModuleVersionInfo getMVI(final List<String> narrs, final List<String> locals) {
		final ModuleVersionInfo mvi = new ModuleVersionInfo();
		mvi.setAdditionalProperties("local_functions", locals);
		mvi.setAdditionalProperties("narrative_methods", narrs);
		return mvi;
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
	
	@Test
	public void isAdminstratorFailNotReleased() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2")));
		
		failIsAdministrator(c, new ResourceID("modname.methname"), new UserName("u1"),
				new NoSuchResourceException("modname.methname"));
	}
	
	@Test
	public void isAdminstratorFailNoMethod() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2"))
						.withRelease(getMVI(
								Arrays.asList("methname2", "m"),
								Arrays.asList("methnam", "methname3"))));
		
		failIsAdministrator(c, new ResourceID("modname.methname"), new UserName("u1"),
				new NoSuchResourceException("modname.methname"));
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
		getAdministrators(Collections.emptyList(), set(),
				Arrays.asList("m", "x"), Arrays.asList("v"));
	}
	
	@Test
	public void getAdministrators() throws Exception {
		getAdministrators(Arrays.asList("u1", "u2"), set(new UserName("u1"), new UserName("u2")),
				Arrays.asList("n", "x"), Arrays.asList("m"));
	}

	private void getAdministrators(
			final List<String> returned,
			final Set<UserName> expected,
			final List<String> narrmeths,
			final List<String> localmeths)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(returned)
						.withRelease(getMVI(narrmeths, localmeths)));
		
		assertThat("incorrect owners", new SDKClientCatalogHandler(c)
				.getAdministrators(new ResourceID("modname.m")),
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
	public void getAdminstratorsFailNotReleased() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2")));
		
		failGetAdministrators(c, new ResourceID("modname.methname"),
				new NoSuchResourceException("modname.methname"));
	}
	
	@Test
	public void getAdminstratorsFailNoMethod() throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("u1", "u2"))
						.withRelease(getMVI(
								Arrays.asList("methname2", "m"),
								Arrays.asList("methnam", "methname3"))));
		
		failGetAdministrators(c, new ResourceID("modname.methname"),
				new NoSuchResourceException("modname.methname"));
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
			final Exception expected)
			throws Exception {
		final CatalogClient c = mock(CatalogClient.class);
		
		when(c.getModuleInfo(argThat(new SelectOneModuleParamsMatcher("modname2"))))
				.thenReturn(new ModuleInfo().withOwners(Arrays.asList("foo", badUser))
						.withRelease(getMVI(Collections.emptyList(), Arrays.asList("m"))));
		
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
			new SDKClientCatalogHandler(cli).getAdministrators(mod);
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
				.withResource(new ResourceID("foo.bar"))
				.withResource(new ResourceID("x.y"))
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
