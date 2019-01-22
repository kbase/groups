package us.kbase.groups.core.fieldvalidation;

import static us.kbase.groups.util.Util.checkString;

import java.util.Optional;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A {@link CustomField} that may optionally be suffixed with a positive integer,
 * separated by a '-'.
 * @author gaprice@lbl.gov
 *
 */
public class NumberedCustomField implements Comparable<NumberedCustomField> {

	private static final String SEP = "-";
	
	private final CustomField field;
	private final int number;
	
	/** Create the custom field. The field will be {@link String#trim()}ed.
	 * @param customField the field.
	 * @throws IllegalParameterException if the field root doesn't meet any of the requirements
	 * of {@link CustomField} or the suffix is not an integer.
	 * @throws MissingParameterException if the field is null or whitespace only.
	 */
	public NumberedCustomField(String customField)
			throws IllegalParameterException, MissingParameterException {
		checkString(customField, "customField", CustomField.MAXIMUM_FIELD_SIZE);
		customField = customField.trim();
		final String[] split = customField.split(SEP, 2);
		final int number;
		if (split.length > 1) {
			try {
				number = Integer.parseInt(split[1]);
				if (number < 1) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new IllegalParameterException(String.format(
						"Suffix after - of field %s must be an integer > 0", customField));
			}
		} else {
			number = -1;
		}
		if (split[0].trim().isEmpty()) {
			throw new IllegalParameterException("Illegal custom field: " + customField);
		}
		this.field = new CustomField(split[0]);
		this.number = number;
	}

	/** Get the field root - the portion of the field without the integer suffix.
	 * @return the field root.
	 */
	public CustomField getFieldRoot() {
		return field;
	}

	/** Get the suffix, if present, as an integer.
	 * @return the suffix.
	 */
	public Optional<Integer> getNumber() {
		return number > 0 ? Optional.of(number) : Optional.empty();
	}
	
	/** Get the entire field.
	 * @return the field.
	 */
	public String getField() {
		return field.getName() + (number > 0 ? SEP + number : "");
	}
	
	/** True if the field is suffixed by a number, false otherwise.
	 * @return if the field is suffixed.
	 */
	public boolean isNumberedField() {
		return number > 0;
	}
	
	@Override
	public int compareTo(final NumberedCustomField other) {
		final int compareField = this.field.compareTo(other.field);
		if (compareField != 0) {
			return compareField;
		}
		return number - other.number;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + number;
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
		NumberedCustomField other = (NumberedCustomField) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		if (number != other.number) {
			return false;
		}
		return true;
	}
}
