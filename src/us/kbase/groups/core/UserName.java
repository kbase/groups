package us.kbase.groups.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A user name.
 * 
 * Valid user names are strings of up to 100 characters consisting of lowercase ASCII letters,
 * digits, and the underscore. The first character must be a letter.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class UserName extends Name {

	private static final String INVALID_CHARS_REGEX = "[^a-z\\d_]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	public final static int MAX_NAME_LENGTH = 100;
	
	/** Create a new user name.
	 * @param name the user name.
	 * @throws MissingParameterException if the name supplied is null or empty.
	 * @throws IllegalParameterException if the name supplied has illegal characters or is too
	 * long.
	 */
	public UserName(final String name)
			throws MissingParameterException, IllegalParameterException {
		super(name, "user name", MAX_NAME_LENGTH);
		final Matcher m = INVALID_CHARS.matcher(name);
		if (m.find()) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME, String.format(
					"Illegal character in user name %s: %s", name, m.group()));
		}
		if (!Character.isLetter(name.codePointAt(0))) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
					"Username must start with a letter");
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserName [getName()=");
		builder.append(getName());
		builder.append("]");
		return builder.toString();
	}
}
