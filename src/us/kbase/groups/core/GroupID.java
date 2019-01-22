package us.kbase.groups.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A group ID. Lowercase ASCII letters, numbers, and hyphens are allowed, but the first
 * character must be a letter. The maximum length is 100 characters.
 * @author gaprice@lbl.gov
 *
 */
public class GroupID extends Name {
	
	private static final String INVALID_CHARS_REGEX = "[^a-z\\d-]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);

	/** Create a new ID. The ID will be {@link String#trim()}ed.
	 * @param id the ID.
	 * @throws MissingParameterException if the ID is null or whitespace only.
	 * @throws IllegalParameterException if the ID is too long or has illegal characters.
	 */
	public GroupID(final String id) throws MissingParameterException, IllegalParameterException {
		super(id, "group id", 100);
		final Matcher m = INVALID_CHARS.matcher(getName());
		if (m.find()) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID, String.format(
					"Illegal character in group id %s: %s", id, m.group()));
		}
		if (!Character.isLetter(getName().codePointAt(0))) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_GROUP_ID,
					"Group ID must start with a letter");
		}
	}

}
