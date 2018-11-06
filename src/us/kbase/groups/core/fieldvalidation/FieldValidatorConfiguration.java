package us.kbase.groups.core.fieldvalidation;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FieldValidatorConfiguration {
	
	// TODO JAVADOC
	// TODO TEST
	
	private final CustomField field;
	private final String validatorClass;
	private final boolean isNumberedField;
	private final Map<String, String> validatorConfiguration;
	
	// probably won't need a builder ...?
	public FieldValidatorConfiguration(
			final CustomField field,
			final String validatorClass,
			final boolean isNumberedField,
			final Map<String, String> validatorConfiguration) {
		checkNotNull(field, "field");
		exceptOnEmpty(validatorClass, "validatorClass");
		checkNotNull(validatorConfiguration, "validatorConfiguration");
		for (final String key: validatorConfiguration.keySet()) {
			exceptOnEmpty(key, "Validator configuration key");
			exceptOnEmpty(validatorConfiguration.get(key),
					"Validator configuration value for key " + key);
		}
		this.field = field;
		this.validatorClass = validatorClass;
		this.isNumberedField = isNumberedField;
		this.validatorConfiguration = Collections.unmodifiableMap(
				new HashMap<>(validatorConfiguration));
	}

	public CustomField getField() {
		return field;
	}

	public String getValidatorClass() {
		return validatorClass;
	}

	public boolean isNumberedField() {
		return isNumberedField;
	}

	public Map<String, String> getValidatorConfiguration() {
		return validatorConfiguration;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + (isNumberedField ? 1231 : 1237);
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
		if (isNumberedField != other.isNumberedField) {
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
}
