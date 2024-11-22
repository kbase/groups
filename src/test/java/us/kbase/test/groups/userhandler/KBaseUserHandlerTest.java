package us.kbase.test.groups.userhandler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.userhandler.KBaseUserHandler;
import us.kbase.test.auth2.authcontroller.AuthController;
import us.kbase.test.groups.TestCommon;
import us.kbase.testutils.controllers.mongo.MongoController;

public class KBaseUserHandlerTest {
	
	private static MongoController MONGO;
	private static AuthController AUTH = null;
	private static URL AUTHURL = null;
	private static Path TEMP_DIR = null;
	
	private static String TOKEN1 = null;
	private static String TOKEN2 = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		TestCommon.stfuLoggers();
		TEMP_DIR = TestCommon.getTempDir().resolve(KBaseUserHandlerTest.class.getSimpleName() +
				UUID.randomUUID().toString());
		Files.createDirectories(TEMP_DIR);
		
		MONGO = new MongoController(
				TestCommon.getMongoExe().toString(),
				TEMP_DIR,
				TestCommon.useWiredTigerEngine());

		AUTH = new AuthController(
				"localhost:" + MONGO.getServerPort(),
				KBaseUserHandlerTest.class.getSimpleName() + "_test",
				TEMP_DIR);
		AUTHURL = new URL("http://localhost:" + AUTH.getServerPort() + "/testmode");
		System.out.println("started auth server at " + AUTHURL);
		TestCommon.createAuthUser(AUTHURL, "user1", "display1");
		TOKEN1 = TestCommon.createLoginToken(AUTHURL, "user1");
		TestCommon.createAuthUser(AUTHURL, "user2", "display2");
		TOKEN2 = TestCommon.createLoginToken(AUTHURL, "user2");
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (AUTH != null) {
			AUTH.destroy(deleteTempFiles);
		}
		if (MONGO != null) {
			MONGO.destroy(deleteTempFiles);
		}
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Test
	public void getUser() throws Exception {
		final KBaseUserHandler kuh = new KBaseUserHandler(AUTHURL, new Token(TOKEN2));
		
		final UserName user = kuh.getUser(new Token(TOKEN1));
		assertThat("incorrect username", user, is(new UserName("user1")));
	}
	
	@Test
	public void isValidUser() throws Exception {
		final KBaseUserHandler kuh = new KBaseUserHandler(AUTHURL, new Token(TOKEN2));
		
		assertThat("incorrect user valid", kuh.isValidUser(new UserName("user1")), is(true));
		assertThat("incorrect user valid", kuh.isValidUser(new UserName("user3")), is(false));
	}
	
	@Test
	public void constructFailNulls() throws Exception {
		failConstruct(null, new Token("t"), new NullPointerException("rootAuthURL"));
		failConstruct(AUTHURL, null, new NullPointerException("serviceToken"));
	}
	
	@Test
	public void constructFailBadArgs() throws Exception {
		failConstruct(new URL(AUTHURL.toString() + "/foo"), new Token(TOKEN1),
				new AuthenticationException(
						ErrorType.AUTHENTICATION_FAILED,
						"Failed to contact the auth service: Auth service returned an error: "
						+ "HTTP 404 Not Found"));
		failConstruct(AUTHURL, new Token("fake"), new InvalidTokenException(
				"Auth service returned an error: 10020 Invalid token"));
	}
	
	private void failConstruct(final URL url, final Token token, final Exception expected) {
		try {
			new KBaseUserHandler(url, token);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getUserFail() throws Exception {
		final KBaseUserHandler kuh = new KBaseUserHandler(AUTHURL, new Token(TOKEN2));
		
		failGetUser(kuh, null, new NullPointerException("token"));
		failGetUser(kuh, new Token("fake"), new InvalidTokenException(
				"Auth service returned an error: 10020 Invalid token"));
	}
	
	private void failGetUser(
			final KBaseUserHandler kuh,
			final Token token,
			final Exception expected) {
		try {
			kuh.getUser(token);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void isValidUserFail() throws Exception {
		final KBaseUserHandler kuh = new KBaseUserHandler(AUTHURL, new Token(TOKEN2));
		try {
			kuh.isValidUser(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("userName"));
		}
	}
}
