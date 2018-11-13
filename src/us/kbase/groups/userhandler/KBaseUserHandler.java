package us.kbase.groups.userhandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

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

/** A user handler for the KBase authentication service.
 * @author gaprice@lbl.gov
 *
 */
public class KBaseUserHandler implements UserHandler {

	// TODO TEST
	
	// note the configurable auth service handles its own caching.
	
	private static final String GLOBUS_URL_SUFFIX = "api/legacy/globus";
	private static final String KBASE_URL_SUFFIX = "api/legacy/KBase/Sessions/Login";
	
	private final URI rootAuthURI;
	private final ConfigurableAuthService auth;
	private final Token serviceToken;
	
	/** Create the handler.
	 * @param rootAuthURL the root url of the KBase authentication service.
	 * @param serviceToken a service token for the KBase authentication service. This is used
	 * to check that user names are valid.
	 * @param allowInsecureURL allow a non-https URL.
	 * @throws IOException if the authentication service could not be contacted.
	 * @throws URISyntaxException if the URL is not a valid URI.
	 * @throws InvalidTokenException if the service token is invalid.
	 * @throws AuthenticationException if an error occurs while validating the token.
	 */
	public KBaseUserHandler(
			final URL rootAuthURL,
			final Token serviceToken,
			final boolean allowInsecureURL)
			throws IOException, URISyntaxException, InvalidTokenException,
				AuthenticationException {
		checkNotNull(rootAuthURL, "rootAuthURL");
		checkNotNull(serviceToken, "serviceToken");
		if (rootAuthURL.toString().endsWith("/")) {
			this.rootAuthURI = rootAuthURL.toURI();
		} else {
			this.rootAuthURI = new URL(rootAuthURL.toString() + "/").toURI();
		}
		auth = new ConfigurableAuthService(new AuthConfig()
				.withAllowInsecureURLs(allowInsecureURL)
				.withKBaseAuthServerURL(this.rootAuthURI.resolve(KBASE_URL_SUFFIX).toURL())
				.withGlobusAuthURL(this.rootAuthURI.resolve(GLOBUS_URL_SUFFIX).toURL()));
		this.serviceToken = serviceToken;
		getUser(this.serviceToken); // check token is valid
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
	
	@Override
	public boolean isValidUser(final UserName userName) throws AuthenticationException {
		checkNotNull(userName, "userName");
		try {
			return auth.isValidUserName(
					Arrays.asList(userName.getName()),
					new AuthToken(serviceToken.getToken(), "fakeuser"))
					.get(userName.getName());
		} catch (IOException | AuthException e) {
			LoggerFactory.getLogger(getClass()).error("Unexpected auth service response", e);
			throw new AuthenticationException(ErrorType.AUTHENTICATION_FAILED,
					"Recieved unexpected response from authentication server.", e);
		}
	}

	public static void main(final String[] args) throws Exception {
		final String token = args[0];
		final UserHandler uh = new KBaseUserHandler(new URL("https://ci.kbase.us/services/auth"),
				new Token(token), false);
		System.out.println(uh.getUser(new Token(token)));
		
		System.out.println(uh.isValidUser(new UserName("kkeller")));
		System.out.println(uh.isValidUser(new UserName("veryfakeindeed")));
	}
	
}
