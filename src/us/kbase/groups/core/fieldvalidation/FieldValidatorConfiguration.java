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
	private final FieldConfiguration fieldConfiguration;
	private final Map<String, String> validatorConfiguration;
	
	private FieldValidatorConfiguration(
			final CustomField field,
			final String validatorClass,
			final FieldConfiguration fieldConfiguration,
			final Map<String, String> validatorConfiguration) {
		this.field = field;
		this.validatorClass = validatorClass;
		this.fieldConfiguration = fieldConfiguration;
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

	/** Get the configuration for the field.
	 * @return the field configuration.
	 */
	public FieldConfiguration getFieldConfiguration() {
		return fieldConfiguration;
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
		result = prime * result + ((fieldConfiguration == null) ? 0 : fieldConfiguration.hashCode());
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
		if (fieldConfiguration == null) {
			if (other.fieldConfiguration != null) {
				return false;
			}
		} else if (!fieldConfiguration.equals(other.fieldConfiguration)) {
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
		private FieldConfiguration fieldConfiguration = FieldConfiguration.getBuilder().build();
		private final Map<String, String> validatorConfiguration = new HashMap<>();
		
		private Builder(final CustomField field, final String validatorClass) {
			checkNotNull(field, "field");
			exceptOnEmpty(validatorClass, "validatorClass");
			this.field = field;
			this.validatorClass = validatorClass;
		}
		
		/** Set the configuration for the field.
		 * @param config
		 * @return this builder.
		 */
		public Builder withFieldConfiguration(final FieldConfiguration config) {
			checkNotNull(config, "config");
			this.fieldConfiguration = config;
			return this;
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
			return new FieldValidatorConfiguration(field, validatorClass, fieldConfiguration,
					validatorConfiguration);
		}
	}
}
