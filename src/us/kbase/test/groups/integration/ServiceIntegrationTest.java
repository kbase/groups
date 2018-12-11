package us.kbase.test.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockserver.model.JsonBody.json;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoDatabase;

import us.kbase.auth.AuthToken;
import us.kbase.common.test.RegexMatcher;
import us.kbase.groups.core.exceptions.GroupsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.fieldvalidators.SimpleFieldValidatorFactory;
import us.kbase.groups.notifications.SLF4JNotifierFactory;
import us.kbase.test.auth2.MapBuilder;
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
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Client CLI = ClientBuilder.newClient();
	
	private static MongoStorageTestManager MANAGER = null;
	private static AuthController AUTH = null;
	private static WorkspaceController WS = null;
	private static URL WS_URL;
	private static WorkspaceClient WS_CLI1 = null;
	private static WorkspaceClient WS_CLI2 = null;
	private static MongoDatabase WSDB = null;
	private static ClientAndServer mockCatalogClientAndServer;
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
				"user2",
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

		// TODO TEST replace mockserver with actual catalog service? Might be too much work. Docker?
		mockCatalogClientAndServer = setUpCatalogMockServer();
		
		// set up the Groups server
		final Path cfgfile = generateTempConfigFile(MANAGER, DB_NAME, authURL, TOKEN2, WS_URL,
				new URL("http://localhost:" + mockCatalogClientAndServer.getLocalPort()));
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
	
	// sets up a version response for starting up the groups server
	private static ClientAndServer setUpCatalogMockServer() throws Exception {
		// comment out these lines to see mockserver logs, which are very helpful for debugging
		// using org.mockserver does not work, must include .mock
		// https://github.com/jamesdbloom/mockserver/issues/561
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("org.mockserver.mock"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		mockCatalogClientAndServer = ClientAndServer
				.startClientAndServer(TestCommon.findFreePort());
		final HttpResponse resp = new HttpResponse()
				.withStatusCode(200)
				.withBody(MAPPER.writeValueAsString(ImmutableMap.of(
						"version", "1.1",
						"id", "3",
						"result", Arrays.asList("2.1.3"))));
		mockCatalogClientAndServer.when(
				new HttpRequest()
					.withMethod("POST")
					.withBody(json(MAPPER.writeValueAsString(ImmutableMap.of(
							"method", "Catalog.version",
							"params", Collections.emptyList())),
							MatchType.ONLY_MATCHING_FIELDS))
			).respond(
				resp
			);
		return mockCatalogClientAndServer;
	}

	public static Path generateTempConfigFile(
			final MongoStorageTestManager manager,
			final String dbName,
			final URL authURL,
			final String wsToken,
			final URL wsURL,
			final URL catalogURL)
			throws IOException {
		
		final Ini ini = new Ini();
		final Section sec = ini.add("groups");
		sec.add("mongo-host", "localhost:" + manager.mongo.getServerPort());
		sec.add("mongo-db", dbName);
		sec.add("auth-url", authURL.toString());
		sec.add("workspace-admin-token", wsToken);
		sec.add("workspace-url", wsURL.toString());
		sec.add("catalog-url", catalogURL.toString());
		//TODO TEST with actual notifier? depends how hard it is to run feeds. Or test with mock notifier
		sec.add("notifier-factory", SLF4JNotifierFactory.class.getName());
		sec.add("allow-insecure-urls", "true");
		sec.add("field-f1-validator", SimpleFieldValidatorFactory.class.getName());
		sec.add("field-f1-is-numbered", "true");
		sec.add("field-f1-param-max-length", "4");
		sec.add("field-f2-validator", SimpleFieldValidatorFactory.class.getName());
		sec.add("field-user-f1-validator", SimpleFieldValidatorFactory.class.getName());
		sec.add("field-user-f1-is-numbered", "true");
		sec.add("field-user-f1-param-max-length", "4");
		sec.add("field-user-f1-is-user-settable", "true");
		sec.add("field-user-f2-validator", SimpleFieldValidatorFactory.class.getName());
		
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
		mockCatalogClientAndServer.reset();
	}
	
	
	public static void failRequestJSON(
			final Response res,
			final int httpCode,
			final String httpStatus,
			final Exception e)
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
			final Exception expectedException,
			final Map<String, Object> error) {
		
		final Map<String, Object> innerExpected = new HashMap<>();
		innerExpected.put("httpcode", expectedHTTPCode);
		innerExpected.put("httpstatus", expectedHTTPStatus);
		if (expectedException instanceof GroupsException) {
			final GroupsException e = (GroupsException) expectedException;
			innerExpected.put("appcode", e.getErr().getErrorCode());
			innerExpected.put("apperror", e.getErr().getError());
		}
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
	
	//TODO TEST add more tests for create/update/get group. This test is to verify the JDK8 Jackson additions are working.
	@Test
	public void createUpdateGetGroup() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.put(Entity.json(ImmutableMap.of(
				"name", "myname",
				"custom", ImmutableMap.of("f1-62", "yay!", "f2", "yo"))));

		assertSimpleGroupCorrect(res, "myid", "myname",
				ImmutableMap.of("f1-62", "yay!", "f2", "yo"));
		
		assertGroupExists("myid", true);
		assertGroupExists("myid2", false);
		
		final URI updateURI = UriBuilder.fromUri(HOST).path("/group/myid/update").build();
		
		final WebTarget updateTarget = CLI.target(updateURI);
		final Builder req2 = updateTarget.request().header("authorization", TOKEN1);

		final Response res2 = req2.put(Entity.json(MapBuilder.<String, Object>newHashMap()
				.with("name", null) // no change
				// no description, so no change
				.with("custom", MapBuilder.newHashMap().with("f2", null).build())
				.build()));

		assertThat("incorrect response code", res2.getStatus(), is(204));
		
		final ImmutableMap<String, String> custom = ImmutableMap.of("f1-62", "yay!");
		
		assertSimpleGroupCorrect("myid", "myname", custom);
		
		final Builder req5 = updateTarget.request().header("authorization", TOKEN1);
		
		final Response res5 = req5.put(Entity.json(MapBuilder.<String, Object>newHashMap()
				.with("name", "new name")
				.build()));

		assertThat("incorrect response code", res5.getStatus(), is(204));
		
		assertSimpleGroupCorrect("myid", "new name", custom);
	}
	
	private void assertGroupExists(final String gid, boolean exists) {
		final URI uri = UriBuilder.fromUri(HOST).path("/group/" + gid + "/exists").build();
		
		final WebTarget target = CLI.target(uri);
		final Response req = target.request().get();
		
		assertThat("incorrect code", req.getStatus(), is(200));
		
		assertThat("incorrect body", req.readEntity(Map.class),
				is(ImmutableMap.of("exists", exists)));
	}

	@Test
	public void createGroupFailBadJson() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.put(Entity.json(ImmutableMap.of(
				"name", "myname",
				"custom", ImmutableMap.of("foo", 1))));
		
		failRequestJSON(res, 400, "Bad Request", new IllegalParameterException(
				"Value of 'foo' field in 'custom' map is not a string"));
	}
	
	@Test
	public void createGroupFailNumberedField() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.put(Entity.json(ImmutableMap.of(
				"name", "myname",
				"custom", ImmutableMap.of("f2-1", "val"))));
		
		failRequestJSON(res, 400, "Bad Request", new IllegalParameterException(
				"Field f2-1 may not be a numbered field"));
	}
	
	@Test
	public void createGroupFailFieldExceedsGlobalLength() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final String looong = TestCommon.LONG1001 + TestCommon.LONG1001 + TestCommon.LONG1001 +
				TestCommon.LONG1001 + TestCommon.LONG1001;
		
		final Response res = req.put(Entity.json(ImmutableMap.of(
				"name", "myname",
				"custom", ImmutableMap.of("f2", looong.substring(0, 5001)))));
		
		failRequestJSON(res, 400, "Bad Request", new IllegalParameterException(
				"Value for field f2 size greater than limit 5000"));
	}
	
	@Test
	public void updateGroupFailValidationFails() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.put(Entity.json(ImmutableMap.of("name", "myname")));

		assertSimpleGroupCorrect(res, "myid", "myname", Collections.emptyMap());

		final URI updateURI = UriBuilder.fromUri(HOST).path("/group/myid/update").build();
		
		final WebTarget updateTarget = CLI.target(updateURI);
		final Builder req2 = updateTarget.request().header("authorization", TOKEN1);

		final Response res2 = req2.put(Entity.json(MapBuilder.<String, Object>newHashMap()
				.with("custom", MapBuilder.newHashMap().with("f1", "12345").build())
				.build()));
		
		failRequestJSON(res2, 400, "Bad Request", new IllegalParameterException(
				"Field f1 has an illegal value: value is greater than maximum length 4"));
	}
	
	@Test
	public void updateGroupFailNoSuchField() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/group/myid").build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.put(Entity.json(ImmutableMap.of("name", "myname")));

		assertSimpleGroupCorrect(res, "myid", "myname", Collections.emptyMap());

		final URI updateURI = UriBuilder.fromUri(HOST).path("/group/myid/update").build();
		
		final WebTarget updateTarget = CLI.target(updateURI);
		final Builder req2 = updateTarget.request().header("authorization", TOKEN1);

		final Response res2 = req2.put(Entity.json(MapBuilder.<String, Object>newHashMap()
				.with("custom", MapBuilder.newHashMap().with("f3", "a").build())
				.build()));
		
		failRequestJSON(res2, 400, "Bad Request", new NoSuchCustomFieldException(
				"Field f3 is not a configured field"));
	}

	private void assertSimpleGroupCorrect(
			final String groupID,
			final String name,
			final Map<String, String> custom) {
		final URI target = UriBuilder.fromUri(HOST).path("/group/" + groupID).build();
		
		final WebTarget groupTarget = CLI.target(target);
		final Builder req = groupTarget.request().header("authorization", TOKEN1);

		final Response res = req.get();
		
		assertSimpleGroupCorrect(res, groupID, name, custom);
	}
	
	// assumes user = user1, and all lists are empty
	private void assertSimpleGroupCorrect(
			final Response res,
			final String groupID,
			final String name,
			final Map<String, String> custom) {
		assertThat("incorrect response code", res.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> g = res.readEntity(Map.class);
		
		final long create = (long) g.get("createdate");
		final long mod = (long) g.get("moddate");
		g.remove("createdate");
		g.remove("moddate");
		TestCommon.assertCloseToNow(create);
		TestCommon.assertCloseToNow(mod);
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> owner = (Map<String, Object>) g.get("owner");
		final long join = (long) owner.get("joined");
		owner.remove("joined");
		TestCommon.assertCloseToNow(join);
		
		final Map<String, Object> expected = MapBuilder.<String, Object>newHashMap()
				.with("owner", ImmutableMap.of("name", "user1", "custom", Collections.emptyMap()))
				.with("private", false)
				.with("ismember", true)
				.with("members", Collections.emptyList())
				.with("name", name)
				.with("id", groupID)
				.with("resources", ImmutableMap.of(
						"workspace", Collections.emptyList(),
						"catalogmethod", Collections.emptyList()))
				.with("admins", Collections.emptyList())
				.with("custom", custom)
				.build();
		
		assertThat("incorrect group", g, is(expected));
	}
	
	//TODO TEST add more updateUser tests as needed. Not comprehensive ATM.
	
	@Test
	public void updateUser() throws Exception {
		createGroupAndAddUser("myid", TOKEN1, TOKEN2, "user2");
		
		// update user
		final URI updateUserTarget = UriBuilder.fromUri(HOST).path("/group/myid/user/user2/update")
				.build();
		
		final Builder updateUserReq = CLI.target(updateUserTarget).request()
				.header("authorization", TOKEN2);

		final Response updateUserRes = updateUserReq.put(Entity.json(ImmutableMap.of("custom",
				ImmutableMap.of("f1-3", "vala   \t  "))));
		
		assertThat("incorrect response code", updateUserRes.getStatus(), is(204));
		
		// check user record
		final Map<String, Object> g3 = getGroup("myid", TOKEN2);
		
		checkSingleMemberCorrect(g3, "user2", ImmutableMap.of("f1-3", "vala"));
	}
	
	@Test
	public void updateUserFailUnauthorizedToSetField() throws Exception {
		createGroupAndAddUser("myid", TOKEN1, TOKEN2, "user2");
		
		// update user
		final URI updateUserTarget = UriBuilder.fromUri(HOST).path("/group/myid/user/user2/update")
				.build();
		
		final Builder updateUserReq = CLI.target(updateUserTarget).request()
				.header("authorization", TOKEN2);

		final Response updateUserRes = updateUserReq.put(Entity.json(ImmutableMap.of("custom",
				ImmutableMap.of("f2", "val"))));
		
		failRequestJSON(updateUserRes, 403, "Forbidden", new UnauthorizedException(
				"User user2 is not authorized to set field f2 for group myid"));
	}

	// note ownerToken must be TOKEN1 for now
	private void createGroupAndAddUser(
			final String gid,
			final String ownerToken,
			final String userToken,
			final String userName) {
		// create group
		final URI createTarget = UriBuilder.fromUri(HOST).path("/group/" + gid).build();
		
		final Builder req = CLI.target(createTarget).request().header("authorization", ownerToken);

		final Response res = req.put(Entity.json(ImmutableMap.of("name", "myname")));
		
		assertSimpleGroupCorrect(res, gid, "myname", Collections.emptyMap());
		
		// request group membership
		final URI reqUserTarget = UriBuilder.fromUri(HOST)
				.path("/group/" + gid + "/requestmembership").build();
		final Builder reqUser = CLI.target(reqUserTarget).request()
				.header("authorization", userToken);
		
		final Response reqRes = reqUser.post(Entity.json(""));
		
		assertThat("incorrect response code", reqRes.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> g = reqRes.readEntity(Map.class);
		final String id = (String) g.get("id");
		
		// approve group membership
		final URI acceptUserTarget = UriBuilder.fromUri(HOST)
				.path("/request/id/" + id + "/accept").build();
		final Builder acceptUser = CLI.target(acceptUserTarget).request()
				.header("authorization", ownerToken);
		
		final Response acceptRes = acceptUser.put(Entity.json(""));
		
		assertThat("incorrect response code", acceptRes.getStatus(), is(200));
		
		// check user record
		final Map<String, Object> g2 = getGroup(gid, userToken);
		
		checkSingleMemberCorrect(g2, userName, Collections.emptyMap());
	}

	private Map<String, Object> getGroup(final String gid, final String token) {
		final URI getGrpTarget = UriBuilder.fromUri(HOST).path("/group/" + gid).build();
		
		final Builder getGrpReq = CLI.target(getGrpTarget).request()
				.header("authorization", token);

		final Response getGrpRes = getGrpReq.get();
		
		assertThat("incorrect response code", getGrpRes.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> g = getGrpRes.readEntity(Map.class);
		return g;
	}

	private void checkSingleMemberCorrect(
			final Map<String, Object> group,
			final String name,
			final Map<String, Object> custom) {
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> users = (List<Map<String, Object>>) group.get("members");
		assertThat("incorrect user count", users.size(), is(1));
		final Map<String, Object> user = users.get(0);
		
		assertUserCorrect(user, name, custom);
	}

	private void assertUserCorrect(
			final Map<String, Object> user,
			final String name,
			final Map<String, Object> custom) {
		final long joined = (long) user.get("joined");
		user.remove("joined");
		TestCommon.assertCloseToNow(joined);
		
		assertThat("incorrect user", user, is(ImmutableMap.of(
				"name", name, "custom", custom)));
	}
}
	
