package us.kbase.groups.userhandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthException;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserHandler;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class KBaseUserHandler implements UserHandler {

	// TODO JAVADOC
	// TODO TEST
	
	// note the configurable auth service handles its own caching.
	
	private static final String GLOBUS_URL_SUFFIX = "api/legacy/globus";
	private static final String KBASE_URL_SUFFIX = "api/legacy/KBase/Sessions/Login";
	
	private final URL rootAuthURL;
	private final ConfigurableAuthService auth;
	
	public KBaseUserHandler(final URL rootAuthURL) throws IOException, URISyntaxException {
		this(rootAuthURL, false);
	}
	
	public KBaseUserHandler(final URL rootAuthURL, final boolean allowInsecureURL)
			throws IOException, URISyntaxException {
		checkNotNull(rootAuthURL, "rootAuthURL");
		if (rootAuthURL.toString().endsWith("/")) {
			this.rootAuthURL = rootAuthURL;
		} else {
			this.rootAuthURL = new URL(rootAuthURL.toString() + "/");
		}
		auth = new ConfigurableAuthService(new AuthConfig()
				.withAllowInsecureURLs(allowInsecureURL)
				.withKBaseAuthServerURL(this.rootAuthURL.toURI().resolve(KBASE_URL_SUFFIX).toURL())
				.withGlobusAuthURL(this.rootAuthURL.toURI().resolve(GLOBUS_URL_SUFFIX).toURL()));
	}
	
	@Override
	public UserName getUser(final Token token)
			throws InvalidTokenException, AuthenticationException {
		checkNotNull(token, "token");
		try {
			final AuthToken user = auth.validateToken(token.getToken());
			return new UserName(user.getUserName());
		} catch (IOException e) {
			throw new AuthenticationException(ErrorType.AUTHENTICATION_FAILED,
					"Failed contacting authentication server: " + e.getMessage(), e);
		} catch (AuthException e) {
			throw new InvalidTokenException(e.getMessage(), e);
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException(
					"The auth service is returning invalid usernames, something is very wrong", e);
		}
	}

	public static void main(final String[] args) throws Exception {
		final String token = args[0];
		final UserHandler uh = new KBaseUserHandler(new URL("https://ci.kbase.us/services/auth"));
		System.out.println(uh.getUser(new Token(token)));
	}
	
}
