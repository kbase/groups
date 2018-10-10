package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.InvalidTokenException;

public interface UserHandler {
	
	// TODO JAVADOC

	UserName getUser(Token token) throws InvalidTokenException, AuthenticationException;
}
