package us.kbase.groups.core.fieldvalidation;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;

/** A set of {@link FieldValidator}s.
 * @author gaprice@lbl.gov
 *
 */
public class FieldValidators {
	
	private final int maximumValueSize;
	private final Map<CustomField, FieldValidator> validators;
	private final Map<CustomField, FieldConfiguration> fieldConfig;
	private final Map<CustomField, FieldValidator> userValidators;
	private final Map<CustomField, FieldConfiguration> userFieldConfig;
	
	private FieldValidators(
			final int maximumValueSize,
			final Map<CustomField, FieldValidator> validators,
			final Map<CustomField, FieldConfiguration> fieldConfig,
			final Map<CustomField, FieldValidator> userValidators,
			final Map<CustomField, FieldConfiguration> userFieldConfig) {
		this.maximumValueSize = maximumValueSize;
		this.validators = Collections.unmodifiableMap(validators);
		this.fieldConfig = fieldConfig;
		this.userValidators = Collections.unmodifiableMap(userValidators);
		this.userFieldConfig = userFieldConfig;
	}

	/** Validate a field. Runs the appropriate {@link FieldValidator#validate(String)} for the
	 * field.
	 * @param field the field to validate.
	 * @param value the value of the field that will be validated.
	 * @throws MissingParameterException if the value is null or the empty string.
	 * @throws IllegalParameterException if the value is above the maximum length, if the field
	 * is a numbered field when it is not configured as one, or when the value fails to validate.
	 * @throws NoSuchCustomFieldException if the field is not registered.
	 * @throws FieldValidatorException if the value could not be validated for reasons other than
	 * the contents of the the value.
	 */
	public void validate(final NumberedCustomField field, final String value)
			throws MissingParameterException, IllegalParameterException,
				NoSuchCustomFieldException, FieldValidatorException {
		validate(field, value, validators, fieldConfig, "Field");
	}
	
	/** Validate a user field. Runs the appropriate {@link FieldValidator#validate(String)} for the
	 * field.
	 * @param field the field to validate.
	 * @param value the value of the field that will be validated.
	 * @throws MissingParameterException if the value is null or the empty string.
	 * @throws IllegalParameterException if the value is above the maximum length, if the field
	 * is a numbered field when it is not configured as one, or when the value fails to validate.
	 * @throws NoSuchCustomFieldException if the field is not registered.
	 * @throws FieldValidatorException if the value could not be validated for reasons other than
	 * the contents of the the value.
	 */
	public void validateUserField(final NumberedCustomField field, final String value)
			throws MissingParameterException, IllegalParameterException,
				NoSuchCustomFieldException, FieldValidatorException {
		validate(field, value, userValidators, userFieldConfig, "User field");
	}

	private void validate(
			final NumberedCustomField field,
			final String value,
			final Map<CustomField, FieldValidator> validators,
			final Map<CustomField, FieldConfiguration> fieldConfig,
			final String fieldName)
			throws NoSuchCustomFieldException, IllegalParameterException,
				MissingParameterException, FieldValidatorException {
		checkNotNull(field, "field");
		if (!validators.containsKey(field.getFieldRoot())) {
			throw new NoSuchCustomFieldException(String.format(
					"%s %s is not a configured field", fieldName, field.getField()));
		}
		// allow numbered fields without a number
		if (!fieldConfig.get(field.getFieldRoot()).isNumberedField() && field.isNumberedField()) {
			throw new IllegalParameterException(String.format(
					"%s %s may not be a numbered field", fieldName, field.getField()));
		}
		checkString(value,
				String.format("Value for %s %s", fieldName.toLowerCase(), field.getField()),
				maximumValueSize);
		try {
			validators.get(field.getFieldRoot()).validate(value);
		} catch (IllegalFieldValueException e) {
			throw new IllegalParameterException(String.format("%s %s has an illegal value: %s",
					fieldName, field.getField(), e.getMessage()), e);
		}
	}
	
	/** Get the maximum size for a field value.
	 * @return the maximum size.
	 */
	public int getMaximumFieldValueSize() {
		return maximumValueSize;
	}
	
	/** Get the fields with registered validators.
	 * @return the fields.
	 */
	public Set<CustomField> getValidationTargetFields() {
		return validators.keySet();
	}
	
	/** Get the user fields with registered validators.
	 * @return the fields.
	 */
	public Set<CustomField> getValidationTargetUserFields() {
		return userValidators.keySet();
	}
	
	/** Get the configuration for a field.
	 * @param field the field to check.
	 * @throws IllegalArgumentException if the field does not exist.
	 * @return the configuration.
	 */
	public FieldConfiguration getConfig(final CustomField field) {
		return getConfigOrThrow(field, validators, fieldConfig);
	}
	
	/** Get the configuration for a user field.
	 * @param field the field to check.
	 * @throws IllegalArgumentException if the field does not exist.
	 * @return the configuration.
	 */
	public FieldConfiguration getUserFieldConfig(final CustomField field) {
		return getConfigOrThrow(field, userValidators, userFieldConfig);
	}
	
	/** Get the configuration for a field, or {@link Optional#empty()} if the field does not exist.
	 * @param field the field to check.
	 * @return the configuration.
	 */
	public Optional<FieldConfiguration> getConfigOrEmpty(final CustomField field) {
		return getConfig(field, validators, fieldConfig);
	}
	
	/** Get the configuration for a user field, or {@link Optional#empty()} if the field does
	 * not exist.
	 * @param field the field to check.
	 * @return the configuration.
	 */
	public Optional<FieldConfiguration> getUserFieldConfigOrEmpty(final CustomField field) {
		return getConfig(field, userValidators, userFieldConfig);
	}

	private Optional<FieldConfiguration> getConfig(
			final CustomField field,
			final Map<CustomField, FieldValidator> validators,
			final Map<CustomField, FieldConfiguration> fieldConfig) {
		checkNotNull(field, "field");
		return Optional.ofNullable(fieldConfig.get(field));
	}
	
	private FieldConfiguration getConfigOrThrow(
			final CustomField field,
			final Map<CustomField, FieldValidator> validators,
			final Map<CustomField, FieldConfiguration> fieldConfig) {
		return getConfig(field, validators, fieldConfig).orElseThrow(
				() -> new IllegalArgumentException("No such custom field: " + field.getName()));
	}
	
	/** Get a builder for a {@link FieldValidators}.
	 * @param maximumFieldValueSize the maximum size for any field value.
	 * @return the maximum size.
	 */
	public static Builder getBuilder(final int maximumFieldValueSize) {
		return new Builder(maximumFieldValueSize);
	}
	
	/** A builder for a {@link FieldValidators}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {

		private final int maximumValueSize;
		private final Map<CustomField, FieldValidator> validators = new HashMap<>();
		private final Map<CustomField, FieldConfiguration> fieldConfig = new HashMap<>();
		private final Map<CustomField, FieldValidator> userValidators = new HashMap<>();
		private final Map<CustomField, FieldConfiguration> userFieldConfig = new HashMap<>();

		private Builder(final int maximumFieldValueSize) {
			if (maximumFieldValueSize < 1) {
				throw new IllegalArgumentException("maximumFieldValueSize must be > 0");
			}
			this.maximumValueSize = maximumFieldValueSize;
		}
		
		/** Add a validator.
		 * @param field the field the validator will validate.
		 * @param config the field configuration.
		 * @param validator the validator.
		 * @return this builder.
		 */
		public Builder withValidator(
				final CustomField field,
				final FieldConfiguration config,
				final FieldValidator validator) {
			return withValidator(field, config, validator, validators, fieldConfig);
		}
		
		/** Add a validator for user fields.
		 * @param field the field the validator will validate.
		 * @param config the field configuration.
		 * @param validator the validator.
		 * @return this builder.
		 */
		public Builder withUserFieldValidator(
				final CustomField field,
				final FieldConfiguration config,
				final FieldValidator validator) {
			return withValidator(field, config, validator, userValidators, userFieldConfig);
		}

		private Builder withValidator(
				final CustomField field,
				final FieldConfiguration config,
				final FieldValidator validator,
				final Map<CustomField, FieldValidator> validators,
				final Map<CustomField, FieldConfiguration> fieldConfig) {
			checkNotNull(field, "field");
			checkNotNull(config, "config");
			checkNotNull(validator, "validator");
			validators.put(field, validator);
			fieldConfig.put(field, config);
			return this;
		}
		
		/** Build the {@link FieldValidators}.
		 * @return the validators.
		 */
		public FieldValidators build() {
			return new FieldValidators(maximumValueSize, validators, fieldConfig,
					userValidators, userFieldConfig);
		}
	}
}
