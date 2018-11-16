package us.kbase.test.groups.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.groups.TestCommon.set;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import us.kbase.groups.config.GroupsConfig;
import us.kbase.groups.config.GroupsConfigurationException;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.fieldvalidation.CustomField;
import us.kbase.groups.core.fieldvalidation.FieldValidatorConfiguration;
import us.kbase.groups.service.SLF4JAutoLogger;
import us.kbase.groups.util.FileOpener;
import us.kbase.test.groups.TestCommon;

public class GroupsConfigTest {

	private GroupsConfig getConfig(final FileOpener opener) throws Throwable {
		final Constructor<GroupsConfig> con =
				GroupsConfig.class.getDeclaredConstructor(FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	private GroupsConfig getConfig(
			final Path iniFilePath,
			final boolean nullLogger,
			final FileOpener opener)
			throws Throwable {
		final Constructor<GroupsConfig> con =
				GroupsConfig.class.getDeclaredConstructor(
						Path.class, boolean.class, FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(iniFilePath, nullLogger, opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	@Test
	public void sysPropNoUserNoBools() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			System.setProperty(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file2");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "notifier-factory=     factoryclass   \n" + 
					 "auth-url=     http://auth.com       \n" +
					 "catalog-url=     http://cat.com       \n" +
					 "workspace-admin-token=wstoken      \n" +
					 "workspace-url=http://ws.com\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(GroupsConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("http://auth.com")));
		assertThat("incorrect catalog url", cfg.getCatalogURL(), is(new URL("http://cat.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("http://ws.com")));
		assertThat("incorrect ws token", cfg.getWorkspaceAdminToken(), is(new Token("wstoken")));
		assertThat("incorrect notfac", cfg.getNotifierFactory(), is("factoryclass"));
		assertThat("incorrect fac params", cfg.getNotifierParameters(),
				is(Collections.emptyMap()));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect fields", cfg.getFieldConfigurations(), is(set()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void sysPropNoUserNoBoolsWhitespaceFields() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			System.setProperty(GroupsConfig.ENV_VAR_KB_DEP, "some file2");
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=\n" +
					 "mongo-pwd=\n" +
					 "notifier-factory=     factoryclass   \n" +
					 "notifier-param-p1    =     np1    \n" +
					 "notifier-param-p2    =     np2    \n" +
					 "auth-url=http://auth.com\n" +
					 "catalog-url=     http://cat.com       \n" +
					 "workspace-url=http://ws.com\n" +
					 "workspace-admin-token=               wstoken3      \n" +
					 "field-foo-validator    =    foovalclass  \n" +
					 "field-bar-validator    =    barvalclass   \n" +
					 "field-bar-is-numbered  =   nope  \n" +
					 "field-bar-param-p1   =    p1val  \n" +
					 "field-bar-param-p2   =    p2val  \n" +
					 "field-baz-validator  =   bazvalclass  \n" +
					 "field-baz-is-numbered  =   true  \n" +
					 "allow-insecure-urls=true1\n" +
					 "dont-trust-x-ip-headers=true1\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(GroupsConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("http://auth.com")));
		assertThat("incorrect catalog url", cfg.getCatalogURL(), is(new URL("http://cat.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("http://ws.com")));
		assertThat("incorrect ws token", cfg.getWorkspaceAdminToken(), is(new Token("wstoken3")));
		assertThat("incorrect notfac", cfg.getNotifierFactory(), is("factoryclass"));
		assertThat("incorrect fac params", cfg.getNotifierParameters(),
				is(ImmutableMap.of("p1", "np1", "p2", "np2")));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect fields", cfg.getFieldConfigurations(), is(set(
				new FieldValidatorConfiguration(new CustomField("foo"), "foovalclass", false,
						Collections.emptyMap()),
				new FieldValidatorConfiguration(new CustomField("bar"), "barvalclass", false,
						ImmutableMap.of("p1", "p1val", "p2", "p2val")),
				new FieldValidatorConfiguration(new CustomField("baz"), "bazvalclass", true,
						Collections.emptyMap()))));
		testLogger(cfg.getLogger(), false);
		
		try {
			cfg.getFieldConfigurations().add(new FieldValidatorConfiguration(
					new CustomField("f"), "c", false, Collections.emptyMap()));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// is immutable, test passes
		}
		
		try {
			cfg.getNotifierParameters().put("p3", "np3");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// is immutable test passes
		}
	}
	
	@Test
	public void envVarWithUserWithBools() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final GroupsConfig cfg;
		try {
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, "some file");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[groups]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=userfoo\n" +
					 "mongo-pwd=somepwd\n" +
					 "notifier-factory=     factoryclass   \n" + 
					 "auth-url=https://auth.com\n" +
					 "catalog-url=     http://cat.com       \n" +
					 "workspace-url=https://ws.com\n" +
					 "workspace-admin-token=wstoken      \n" +
					 "allow-insecure-urls=true\n" +
					 "dont-trust-x-ip-headers=true\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect catalog url", cfg.getCatalogURL(), is(new URL("http://cat.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect ws token", cfg.getWorkspaceAdminToken(), is(new Token("wstoken")));
		assertThat("incorrect notfac", cfg.getNotifierFactory(), is("factoryclass"));
		assertThat("incorrect fac params", cfg.getNotifierParameters(),
				is(Collections.emptyMap()));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(true));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		assertThat("incorrect fields", cfg.getFieldConfigurations(), is(set()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathNoUserNoBoolsStdLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[groups]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "notifier-factory=     factoryclass   \n" + 
				 "auth-url=https://auth.com\n" +
				 "catalog-url=     http://cat.com       \n" +
				 "workspace-admin-token=wstoken      \n" +
				 "workspace-url=https://ws.com\n")
				.getBytes()));
		final GroupsConfig cfg = getConfig(Paths.get("some file2"), false, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect catalog url", cfg.getCatalogURL(), is(new URL("http://cat.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect ws token", cfg.getWorkspaceAdminToken(), is(new Token("wstoken")));
		assertThat("incorrect notfac", cfg.getNotifierFactory(), is("factoryclass"));
		assertThat("incorrect fac params", cfg.getNotifierParameters(),
				is(Collections.emptyMap()));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(false));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect fields", cfg.getFieldConfigurations(), is(set()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathWithUserWithBoolsNullLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[groups]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "mongo-user=userfoo\n" +
				 "mongo-pwd=somepwd\n" +
				 "notifier-factory=     factoryclass   \n" + 
				 "auth-url=https://auth.com\n" +
				 "catalog-url=     http://cat.com       \n" +
				 "workspace-url=https://ws.com\n" +
				 "workspace-admin-token=wstoken      \n" +
				 "allow-insecure-urls=true\n" +
				 "dont-trust-x-ip-headers=true\n")
				.getBytes()));
		final GroupsConfig cfg = getConfig(Paths.get("some file2"), true, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect auth url", cfg.getAuthURL(), is(new URL("https://auth.com")));
		assertThat("incorrect catalog url", cfg.getCatalogURL(), is(new URL("http://cat.com")));
		assertThat("incorrect ws url", cfg.getWorkspaceURL(), is(new URL("https://ws.com")));
		assertThat("incorrect ws token", cfg.getWorkspaceAdminToken(), is(new Token("wstoken")));
		assertThat("incorrect notfac", cfg.getNotifierFactory(), is("factoryclass"));
		assertThat("incorrect fac params", cfg.getNotifierParameters(),
				is(Collections.emptyMap()));
		assertThat("incorrect allow insecure", cfg.isAllowInsecureURLs(), is(true));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		assertThat("incorrect fields", cfg.getFieldConfigurations(), is(set()));
		testLogger(cfg.getLogger(), true);
	}
	
	private void testLogger(final SLF4JAutoLogger logger, final boolean nullLogger) {
		// too much of a pain to really test. Just test manually which is trivial.
		logger.setCallInfo("GET", "foo", "0.0.0.0");
		
		assertThat("incorrect ID", logger.getCallID(), is(nullLogger ? (String) null : "foo"));
	}
	
	@Test
	public void configFailNoEnvPath() throws Throwable {
		failConfig(new FileOpener(), new GroupsConfigurationException(
				"Could not find deployment configuration file from the " +
				"environment variable / system property KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFailWhiteSpaceEnvPath() throws Throwable {
		// can't put nulls into the sysprops or env
		failConfig("     \t    ", new FileOpener(), new GroupsConfigurationException(
				"Could not find deployment configuration file from the " +
						"environment variable / system property KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFail1ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig("some file", fo, new GroupsConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFail3ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig(Paths.get("some file"), fo, new GroupsConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFailBadIni() throws Throwable {
		failConfigBoth("foobar", new GroupsConfigurationException(
				"Could not read configuration file some file: parse error (at line: 1): foobar"));
	}
	
	@Test
	public void configFailNoSection() throws Throwable {
		failConfigBoth("", new GroupsConfigurationException(
				"No section groups in config file some file"));
	}
	
	@Test
	public void configFailNoHost() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-db=bar\n" +
				"mongo-host=     \t     \n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailNoDB() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=     \t     \n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailUserNoPwd() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n" +
				"mongo-user=user",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n" +
				"mongo-user=user\n" +
				"mongo-pwd=   \t    ",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailPwdNoUser() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n" +
				"mongo-pwd=pwd",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n" +
				"mongo-pwd=pwd\n" +
				"mongo-user=   \t    ",
				new GroupsConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section groups if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailNoAuth() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter auth-url not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=     \t     \n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter auth-url not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailBadAuth() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=   htp://foo.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException("Value htp://foo.com of parameter auth-url " +
						"in section groups of config file some file is not a valid URL"));
	}
	
	@Test
	public void configFailNoCatalog() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" +
				"auth-url=   http://foo.com\n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter catalog-url not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" +
				"auth-url=   http://foo.com\n" +
				"catalog-url=     \t      \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException(
						"Required parameter catalog-url not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailBadCatalog() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=   http://foo.com\n" +
				"catalog-url=     htp://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=https://ws.com\n",
				new GroupsConfigurationException("Value htp://cat.com of parameter catalog-url " +
						"in section groups of config file some file is not a valid URL"));
	}
	
	@Test
	public void configFailNoWS() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"workspace-admin-token=wstoken      \n" +
				"catalog-url=     http://cat.com       \n" +
				"auth-url=https://auth.com\n",
				new GroupsConfigurationException(
						"Required parameter workspace-url not provided in configuration file " +
						"some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=     \t     \n",
				new GroupsConfigurationException(
						"Required parameter workspace-url not provided in configuration file " +
						"some file, section groups"));
	}
	
	@Test
	public void configFailBadWS() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=wstoken      \n" +
				"workspace-url=htp://foo.com\n",
				new GroupsConfigurationException("Value htp://foo.com of parameter " +
						"workspace-url in section groups of config file some file is not a " +
						"valid URL"));
	}
	
	@Test
	public void configFailNoWSToken() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter workspace-admin-token not provided in " +
						"configuration file some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=  \t       \n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter workspace-admin-token not provided in " +
						"configuration file some file, section groups"));
	}
	
	@Test
	public void configFailNoNotifierClass() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=  t     \n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter notifier-factory not provided in " +
						"configuration file some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     \t   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=  t       \n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter notifier-factory not provided in " +
								"configuration file some file, section groups"));
	}
	
	@Test
	public void configFailNoNotifierParamName() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" +
				"notifier-param-=     pval   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Error building configuration in section groups of " +
						"config file some file: Illegal parameter notifier-param-"));
	}
	
	@Test
	public void configFailNoNotifierParamValue() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" +
				"notifier-param-p1=     \t   \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Parameter notifier-param-p1 in section groups of configfile some " +
						"file has no value"));
	}
	
	@Test
	public void configFailBadFieldKey() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo=bleah\n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Error building configuration for field in section groups of config " +
						"file some file: Unknown field parameter field-foo"));
	}
	
	@Test
	public void configFailBadFieldKeyMissingField() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-    \t   -foo=bleah\n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Error building configuration for field in section groups of config " +
						"file some file: Missing field name from parameter field-    \t   -foo"));
	}
	
	@Test
	public void configFailIllegalFieldValue() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo_1-validator=bleah\n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Error building configuration for field in section groups of config " +
						"file some file: 30001 Illegal input parameter: " +
						"Illegal character in custom field foo_1: _"));
	}
	
	@Test
	public void configFailNoValidator() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo-param-p1=   p2    \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter field-foo-validator not provided in " +
						"configuration file some file, section groups"));
		
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo-validator=       \t    \n" +
				"field-foo-param-p1=   p2    \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Required parameter field-foo-validator not provided in " +
						"configuration file some file, section groups"));
	}
	
	@Test
	public void configFailNoFieldParamName() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo-validator=    val    \n" +
				"field-foo-param-=   p2    \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Error building configuration in section groups of " +
						"config file some file: Illegal parameter field-foo-param-"));
	}
	
	@Test
	public void configFailNoFieldParamValue() throws Throwable {
		failConfigBoth(
				"[groups]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"notifier-factory=     factoryclass   \n" + 
				"field-foo-validator=    val    \n" +
				"field-foo-param-p1=   \t    \n" + 
				"auth-url=https://auth.com\n" +
				"catalog-url=     http://cat.com       \n" +
				"workspace-admin-token=token\n" +
				"workspace-url=http://foo.com\n",
				new GroupsConfigurationException(
						"Parameter field-foo-param-p1 in section groups of configfile some " +
						"file has no value"));
	}
	
	private InputStream toStr(final String input) {
		return new ByteArrayInputStream(input.getBytes());
	}
	
	private void failConfig(final FileOpener opener, final Exception expected) throws Throwable {
		try {
			getConfig(opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig(
			final String filename,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			TestCommon.getenv().put(GroupsConfig.ENV_VAR_KB_DEP, filename);
			failConfig(opener, expected);
		} finally {
			TestCommon.getenv().remove(GroupsConfig.ENV_VAR_KB_DEP);
		}
	}
	
	private void failConfig1Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig("some file", fo, expected);
	}
	
	private void failConfig(
			final Path pathToIni,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			getConfig(pathToIni, false, opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig3Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig(Paths.get("some file"), fo, expected);
	}
	
	private void failConfigBoth(final String fileContents, final Exception expected)
			throws Throwable {
		failConfig1Arg(fileContents, expected);
		failConfig3Arg(fileContents, expected);
	}
}
