package us.kbase.groups.core.fieldvalidation;

import static us.kbase.groups.util.Util.checkString;

import java.util.Optional;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class NumberedCustomField implements Comparable<NumberedCustomField> {

	// TODO JAVADOC
	// TODO TEST
	
	private static final String SEP = "-";
	
	private final CustomField field;
	private final int number;
	
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
		this.field = new CustomField(split[0]);
		this.number = number;
	}

	public CustomField getFieldRoot() {
		return field;
	}

	public Optional<Integer> getNumber() {
		return number > 0 ? Optional.of(number) : Optional.empty();
	}
	
	public String getField() {
		return field.getName() + (number > 0 ? SEP + number : "");
	}
	
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
