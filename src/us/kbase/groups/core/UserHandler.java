package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.InvalidTokenException;

/** A handler for getting user information.
 * @author gaprice@lbl.gov
 *
 */
public interface UserHandler {
	
	/** Get a user's username given a token.
	 * @param token the user's token.
	 * @return the user name.
	 * @throws InvalidTokenException if the token is invalid.
	 * @throws AuthenticationException if an error occurs getting the user name.
	 */
	UserName getUser(Token token) throws InvalidTokenException, AuthenticationException;

	/** Validate that a user name is a legitimate, existing name.
	 * @param userName the user name.
	 * @return true if the user name is valid, false otherwise.
	 * @throws AuthenticationException if an error occurs checking the name.
	 */
	boolean isValidUser(UserName userName) throws AuthenticationException;
}
