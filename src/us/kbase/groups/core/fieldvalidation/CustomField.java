package us.kbase.groups.core.fieldvalidation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class CustomField extends Name {

	// TODO JAVADOC
	// TODO TEST
	
	public static final int MAXIMUM_FIELD_SIZE = 50;
	
	private static final String INVALID_CHARS_REGEX = "[^a-z\\d]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);

	public CustomField(final String field)
			throws MissingParameterException, IllegalParameterException {
		super(field, "custom field", MAXIMUM_FIELD_SIZE);
		final Matcher m = INVALID_CHARS.matcher(getName());
		if (m.find()) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER, String.format(
					"Illegal character in custom field %s: %s", field, m.group()));
		}
		if (!Character.isLetter(getName().codePointAt(0))) {
			throw new IllegalParameterException(ErrorType.ILLEGAL_PARAMETER, String.format(
					"Custom field %s must start with a letter", field));
		}
	}
}
