package us.kbase.groups.core.fieldvalidation;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import java.util.HashMap;
import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;

public class FieldValidators {
	
	// TODO JAVADOC
	// TODO TEST

	private final int maximumFieldSize;
	private final Map<CustomField, FieldValidator> validators;
	private final Map<CustomField, Boolean> isNumbered;
	
	private FieldValidators(
			final int maximumFieldSize,
			final Map<CustomField, FieldValidator> validators,
			final Map<CustomField, Boolean> isNumbered) {
		this.maximumFieldSize = maximumFieldSize;
		this.validators = validators;
		this.isNumbered = isNumbered;
	}

	public void validate(final NumberedCustomField field, final String value)
			throws MissingParameterException, IllegalParameterException,
				NoSuchCustomFieldException, FieldValidatorException {
		checkNotNull(field, "field");
		if (!validators.containsKey(field.getFieldRoot())) {
			throw new NoSuchCustomFieldException(String.format(
					"Field %s is not a configured field", field.getField()));
		}
		
		// allow numbered fields without a number
		if (!isNumbered.get(field.getFieldRoot()) && field.isNumberedField()) {
			throw new IllegalParameterException(String.format(
					"Field %s may not be a numbered field", field.getField()));
		}
		checkString(value, "Field " + field.getField(), maximumFieldSize);
		try {
			validators.get(field.getFieldRoot()).validate(value);
		} catch (IllegalFieldValueException e) {
			throw new IllegalParameterException(String.format("Field %s has an illegal value: %s",
					field.getField(), e.getMessage()), e);
		}
	}
	
	public int getMaximumFieldSize() {
		return maximumFieldSize;
	}
	
	public static Builder getBuilder(final int maximumFieldSize) {
		return new Builder(maximumFieldSize);
	}
	
	public static class Builder {

		private final int maximumFieldSize;
		private final Map<CustomField, FieldValidator> validators = new HashMap<>();
		private final Map<CustomField, Boolean> isNumbered = new HashMap<>();

		private Builder(final int maximumFieldSize) {
			if (maximumFieldSize < 1) {
				throw new IllegalArgumentException("maximumSize must be > 0");
			}
			this.maximumFieldSize = maximumFieldSize;
		}
		
		public Builder withValidator(
				final CustomField field,
				final boolean numberedField,
				final FieldValidator validator) {
			checkNotNull(field, "field");
			checkNotNull(validator, "validator");
			validators.put(field, validator);
			isNumbered.put(field, numberedField);
			return this;
		}
		
		public FieldValidators build() {
			return new FieldValidators(maximumFieldSize, validators, isNumbered);
		}
	}
}
