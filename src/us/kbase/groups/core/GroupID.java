package us.kbase.groups.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class GroupID extends Name {
	
	//TODO JAVADOC
	//TODO TEST
	
	private static final String INVALID_CHARS_REGEX = "[^a-z\\d_-]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	public final static int MAX_NAME_LENGTH = 100;
	
	public GroupID(final String id) throws MissingParameterException, IllegalParameterException {
		super(id, " group id", 100);
		final Matcher m = INVALID_CHARS.matcher(id);
		if (m.find()) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME, String.format(
					"Illegal character in group id %s: %s", id, m.group()));
		}
		if (!Character.isLetter(id.codePointAt(0))) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_USER_NAME,
					"Group ID must start with a letter");
		}
	}

}
