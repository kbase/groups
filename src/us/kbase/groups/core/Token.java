package us.kbase.groups.core;

import static us.kbase.groups.util.Util.checkString;

import us.kbase.groups.core.exceptions.MissingParameterException;


/** An authentication token used to identify a user.
 * @author gaprice@lbl.gov
 *
 */
public class Token {
	
	private final String token;

	/** Create a token.
	 * @param token the token string
	 * @throws MissingParameterException if the token string is null or whitespace only.
	 */
	public Token(final String token) throws MissingParameterException {
		checkString(token, "token");
		this.token = token;
	}

	/** Get the token string.
	 * @return the token string.
	 */
	public String getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Token other = (Token) obj;
		if (token == null) {
			if (other.token != null) {
				return false;
			}
		} else if (!token.equals(other.token)) {
			return false;
		}
		return true;
	}
}
