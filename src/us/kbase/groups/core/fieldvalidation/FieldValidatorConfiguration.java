package us.kbase.groups.core.fieldvalidation;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** A configuration for a {@link FieldValidator}.
 * @author gaprice@lbl.gov
 *
 */
public class FieldValidatorConfiguration {
	
	private final CustomField field;
	private final String validatorClass;
	private final boolean isNumberedField;
	private final boolean isPublicField;
	private final boolean isMinimalViewField;
	private final Map<String, String> validatorConfiguration;
	
	private FieldValidatorConfiguration(
			final CustomField field,
			final String validatorClass,
			final boolean isNumberedField,
			final boolean isPublicField,
			final boolean isMinimalViewField,
			final Map<String, String> validatorConfiguration) {
		this.field = field;
		this.validatorClass = validatorClass;
		this.isNumberedField = isNumberedField;
		this.isPublicField = isPublicField;
		this.isMinimalViewField = isMinimalViewField;
		this.validatorConfiguration = Collections.unmodifiableMap(validatorConfiguration);
	}

	/** Get the field that the validator will validate.
	 * @return the field.
	 */
	public CustomField getField() {
		return field;
	}

	/** Get the class name for the {@link FieldValidatorFactory} that builds the validator.
	 * @return the class name.
	 */
	public String getValidatorClass() {
		return validatorClass;
	}

	/** Get whether the field may be a numbered field, as specified by
	 * {@link NumberedCustomField#isNumberedField()}.
	 * @return true if the field is a numbered field.
	 */
	public boolean isNumberedField() {
		return isNumberedField;
	}
	
	/** Get whether the field is a public field and should be available to all users regardless
	 * of appropriate authorization.
	 * @return whether the field is public.
	 */
	public boolean isPublicField() {
		return isPublicField;
	}
	
	/** Get whether the field should be shown in minimal views of the containing object.
	 * @return whether the field show be shown in minimal views.
	 */
	public boolean isMinimalViewField() {
		return isMinimalViewField;
	}

	/** Get the configuration for the validator, as provided to
	 * {@link FieldValidatorFactory#getValidator(Map)}.
	 * @return the validator configuration.
	 */
	public Map<String, String> getValidatorConfiguration() {
		return validatorConfiguration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + (isMinimalViewField ? 1231 : 1237);
		result = prime * result + (isNumberedField ? 1231 : 1237);
		result = prime * result + (isPublicField ? 1231 : 1237);
		result = prime * result + ((validatorClass == null) ? 0 : validatorClass.hashCode());
		result = prime * result + ((validatorConfiguration == null) ? 0 : validatorConfiguration.hashCode());
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
		FieldValidatorConfiguration other = (FieldValidatorConfiguration) obj;
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		if (isMinimalViewField != other.isMinimalViewField) {
			return false;
		}
		if (isNumberedField != other.isNumberedField) {
			return false;
		}
		if (isPublicField != other.isPublicField) {
			return false;
		}
		if (validatorClass == null) {
			if (other.validatorClass != null) {
				return false;
			}
		} else if (!validatorClass.equals(other.validatorClass)) {
			return false;
		}
		if (validatorConfiguration == null) {
			if (other.validatorConfiguration != null) {
				return false;
			}
		} else if (!validatorConfiguration.equals(other.validatorConfiguration)) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link FieldValidatorConfiguration}.
	 * @param field the field that the validator will validate.
	 * @param validatorClass the class name of the {@link FieldValidatorFactory} class.
	 * @return the builder.
	 */
	public static Builder getBuilder(final CustomField field, final String validatorClass) {
		return new Builder(field, validatorClass);
	}
	
	/** A builder for a {@link FieldValidatorConfiguration}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final CustomField field;
		private final String validatorClass;
		private boolean isNumberedField = false;
		private boolean isPublicField = false;
		private boolean isMinimalViewField = false;
		private final Map<String, String> validatorConfiguration = new HashMap<>();
		
		private Builder(final CustomField field, final String validatorClass) {
			checkNotNull(field, "field");
			exceptOnEmpty(validatorClass, "validatorClass");
			this.field = field;
			this.validatorClass = validatorClass;
		}
		
		/** Set whether this field is a numbered field.
		 * @param isNumberedField whether the field may be a numbered field, as specified by
		 * {@link NumberedCustomField#isNumberedField()}. Null results in a false (the default)
		 * value.
		 * @return this builder.
		 */
		public Builder withNullableIsNumberedField(final Boolean isNumberedField) {
			this.isNumberedField = bool(isNumberedField);
			return this;
		}
		
		/** Set whether the field is a public field and should be available to all users without
		 * regard for appropriate authorization.
		 * @param isPublicField whether the field is public. Null results in a false
		 * (the default) value.
		 * @return this builder.
		 */
		public Builder withNullableIsPublicField(final Boolean isPublicField) {
			this.isPublicField = bool(isPublicField);
			return this;
		}
		
		/** Set whether the field should be shown in minimal views of the containing object.
		 * @param isMinimalViewField whether the field show be shown in minimal views. Null
		 * results in a false (the default) value.
		 * @return this builder.
		 */
		public Builder withNullableIsMinimalViewField(final Boolean isMinimalViewField) {
			this.isMinimalViewField = bool(isMinimalViewField);
			return this;
		}
		
		// null == false
		private boolean bool(final Boolean b) {
			return b != null && b;
		}
		
		/** Add a configuration item for the validator, as provided to
		 * {@link FieldValidatorFactory#getValidator(Map)}
		 * @param key the configuration item key.
		 * @param value the configuration item value.
		 * @return this builder.
		 */
		public Builder withConfigurationEntry(final String key, final String value) {
			exceptOnEmpty(key, "key");
			exceptOnEmpty(value, "value");
			validatorConfiguration.put(key, value);
			return this;
		}
		
		/** Build the validator configuration.
		 * @return a new configuration.
		 */
		public FieldValidatorConfiguration build() {
			return new FieldValidatorConfiguration(field, validatorClass, isNumberedField,
					isPublicField, isMinimalViewField, validatorConfiguration);
		}
	}
}
