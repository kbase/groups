package us.kbase.groups.core.fieldvalidation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.groups.core.Name;
import us.kbase.groups.core.exceptions.ErrorType;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A custom field name for a value. Valid fields are lowercase ASCII letters and numbers and
 * no longer than 50 Unicode code points. The field must start with a letter.
 * @author gaprice@lbl.gov
 *
 */
public class CustomField extends Name {

	/** The maximum size of the field in Unicode code points. */
	public static final int MAXIMUM_FIELD_SIZE = 50;
	
	private static final String INVALID_CHARS_REGEX = "[^a-z\\d]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);

	/** Create the new field.
	 * @param field the field name.
	 * @throws MissingParameterException if the field is null or whitespace only.
	 * @throws IllegalParameterException if the field is too long, contains illegal characters,
	 * or doesn't start with a letter.
	 */
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
