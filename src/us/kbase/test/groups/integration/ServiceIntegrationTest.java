package us.kbase.test.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoDatabase;

import us.kbase.auth.AuthToken;
import us.kbase.common.test.RegexMatcher;
import us.kbase.groups.core.exceptions.GroupsException;
import us.kbase.test.auth2.authcontroller.AuthController;
import us.kbase.test.groups.MongoStorageTestManager;
import us.kbase.test.groups.StandaloneGroupsServer;
import us.kbase.test.groups.StandaloneGroupsServer.ServerThread;
import us.kbase.test.groups.TestCommon;
import us.kbase.test.groups.controllers.workspace.WorkspaceController;
import us.kbase.test.groups.service.api.RootTest;
import us.kbase.workspace.WorkspaceClient;

public class ServiceIntegrationTest {
	
	/* These tests check basic integration of the various classes that comprise the service.
	 * They are not intended to provide high levels of coverage - that is the purpose of the
	 * unit tests.
	 * 
	 * These tests just ensure that the basic end to end operations work, and usually test
	 * one happy test and one unhappy test per endpoint.
	 */

	/* Not tested via integration tests:
	 * 1) ignoring IP headers - test manually for now.
	 * 2) logging - test manually.
	 * 3) Mongo with auth - test manually.
	 * 4) The 2 ways of specifying the config file path
	 * 5) Some of the startup code for the server, dealing with cases where there's already
	 * a MongoClient created (not sure if this can actually happen)
	 */
	
	private static final String DB_NAME = "test_groups_service";
	
	private static final Client CLI = ClientBuilder.newClient();
	
	private static MongoStorageTestManager MANAGER = null;
	private static AuthController AUTH = null;
	private static WorkspaceController WS = null;
	private static URL WS_URL;
	private static WorkspaceClient WS_CLI1 = null;
	private static WorkspaceClient WS_CLI2 = null;
	private static MongoDatabase WSDB = null;
	private static StandaloneGroupsServer SERVER = null;
	private static int PORT = -1;
	private static String HOST = null;
	private static Path TEMP_DIR = null;

	private static String TOKEN1 = null;
	private static String TOKEN2 = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		TestCommon.stfuLoggers();
		TEMP_DIR = TestCommon.getTempDir().resolve("ServiceIntegTest_" +
				UUID.randomUUID().toString());
		Files.createDirectories(TEMP_DIR);
		
		MANAGER = new MongoStorageTestManager(DB_NAME);

		// set up auth
		AUTH = new AuthController(
				TestCommon.getJarsDir(),
				"localhost:" + MANAGER.mongo.getServerPort(),
				"GroupsServiceIntgrationTestAuth",
				TEMP_DIR);
		final URL authURL = new URL("http://localhost:" + AUTH.getServerPort() + "/testmode");
		System.out.println("started auth server at " + authURL);
		TestCommon.createAuthUser(authURL, "user1", "display1");
		TOKEN1 = TestCommon.createLoginToken(authURL, "user1");
		TestCommon.createAuthUser(authURL, "user2", "display2");
		TOKEN2 = TestCommon.createLoginToken(authURL, "user2");

		// set up Workspace
		WS = new WorkspaceController(
				TestCommon.getJarsDir(),
				"localhost:" + MANAGER.mongo.getServerPort(),
				"GroupsServiceIntegTestWSDB",
				"fakeadmin",
				authURL,
				TEMP_DIR);
		WSDB = MANAGER.mc.getDatabase("GroupsServiceIntegTestWSDB");

		WS_URL = new URL("http://localhost:" + WS.getServerPort());
		WS_CLI1 = new WorkspaceClient(WS_URL, new AuthToken(TOKEN1, "user1"));
		WS_CLI1.setIsInsecureHttpConnectionAllowed(true);
		WS_CLI2 = new WorkspaceClient(WS_URL, new AuthToken(TOKEN2, "user2"));
		WS_CLI2.setIsInsecureHttpConnectionAllowed(true);
		System.out.println(String.format("Started workspace service %s at %s",
				WS_CLI1.ver(), WS_URL));

		// set up the Groups server
		final Path cfgfile = generateTempConfigFile(MANAGER, DB_NAME);
		TestCommon.getenv().put("KB_DEPLOYMENT_CONFIG", cfgfile.toString());
		SERVER = new StandaloneGroupsServer();
		new ServerThread(SERVER).start();
		System.out.println("Main thread waiting for server to start up");
		while (SERVER.getPort() == null) {
			Thread.sleep(1000);
		}
		PORT = SERVER.getPort();
		HOST = "http://localhost:" + PORT;
	}
	
	public static Path generateTempConfigFile(
			final MongoStorageTestManager manager,
			final String dbName)
			throws IOException {
		
		final Ini ini = new Ini();
		final Section sec = ini.add("groups");
		sec.add("mongo-host", "localhost:" + manager.mongo.getServerPort());
		sec.add("mongo-db", dbName);
		
		final Path deploy = Files.createTempFile(TEMP_DIR, "cli_test_deploy", ".cfg");
		ini.store(deploy.toFile());
		deploy.toFile().deleteOnExit();
		System.out.println("Generated temporary config file " + deploy);
		return deploy.toAbsolutePath();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (SERVER != null) {
			SERVER.stop();
		}
		if (WS != null) {
			WS.destroy(deleteTempFiles);
		}
		if (AUTH != null) {
			AUTH.destroy(deleteTempFiles);
		}
		if (MANAGER != null) {
			MANAGER.destroy();
		}
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void clean() {
		TestCommon.destroyDB(MANAGER.db);
		TestCommon.destroyDB(WSDB);
	}
	
	
	public static void failRequestJSON(
			final Response res,
			final int httpCode,
			final String httpStatus,
			final GroupsException e)
			throws Exception {
		
		if (res.getStatus() != httpCode) {
			String text = null; 
			try {
				text = res.readEntity(String.class);
			} catch (Exception exp) {
				exp.printStackTrace();
			}
			if (text == null) {
				text = "Unable to get entity text - see error stream for exception";
			}
			fail(String.format("unexpected http code %s, wanted %s. Entity contents:\n%s",
					res.getStatus(), httpCode, text));
		}
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> error = res.readEntity(Map.class);
		
		assertErrorCorrect(httpCode, httpStatus, e, error);
	}
	
	public static void assertErrorCorrect(
			final int expectedHTTPCode,
			final String expectedHTTPStatus,
			final GroupsException expectedException,
			final Map<String, Object> error) {
		
		final Map<String, Object> innerExpected = new HashMap<>();
		innerExpected.put("httpcode", expectedHTTPCode);
		innerExpected.put("httpstatus", expectedHTTPStatus);
		innerExpected.put("appcode", expectedException.getErr().getErrorCode());
		innerExpected.put("apperror", expectedException.getErr().getError());
		innerExpected.put("message", expectedException.getMessage());
		
		final Map<String, Object> expected = ImmutableMap.of("error", innerExpected);
		
		if (!error.containsKey("error")) {
			fail("error object has no error key");
		}
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> inner = (Map<String, Object>) error.get("error");
		
		final String callid = (String) inner.get("callid");
		final long time = (long) inner.get("time");
		inner.remove("callid");
		inner.remove("time");
		
		assertThat("incorrect error structure less callid and time", error, is(expected));
		assertThat("incorrect call id", callid, RegexMatcher.matches("\\d{16}"));
		TestCommon.assertCloseToNow(time);
	}
	
	@Test
	public void root() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/").build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.get();
		
		assertThat("incorrect response code", res.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> r = res.readEntity(Map.class);
		
		final long servertime = (long) r.get("servertime");
		r.remove("servertime");
		TestCommon.assertCloseToNow(servertime);
		
		final String gitcommit = (String) r.get("gitcommithash");
		r.remove("gitcommithash");
		RootTest.assertGitCommitFromRootAcceptable(gitcommit);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"version", RootTest.SERVER_VER,
				"servname", "Groups service");
		
		assertThat("root json incorrect", r, is(expected));
	}
}
	
