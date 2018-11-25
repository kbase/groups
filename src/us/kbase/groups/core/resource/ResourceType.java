package us.kbase.groups.core.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The type of a resource.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceType extends Name {
	
	private static final String INVALID_CHARS_REGEX = "[^a-z\\d]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	
	/** Create the resource type. A type must be no more than 20 lowercase ASCII letters.
	 * @param type the type.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 * @throws IllegalParameterException if the input contains control characters.
	 */
	public ResourceType(final String type)
			throws MissingParameterException, IllegalParameterException {
		super(type, "resource type", 20);
		final Matcher m = INVALID_CHARS.matcher(type);
		if (m.find()) {
			throw new IllegalParameterException(String.format(
					"Illegal character in resource type %s: %s", type, m.group()));
		}
	}

}
