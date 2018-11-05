package us.kbase.groups.fieldvalidators;

import static us.kbase.groups.util.Util.containsControlCharacters;
import static us.kbase.groups.util.Util.codePoints;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;

public class EnumFieldValidatorFactory implements FieldValidatorFactory {

	private static final int MAX_LENGTH = 50;
	
	// TODO JAVADOC
	// TODO TEST
	
	//TODO NOW gravatar validator
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		final String allowedValues = configuration.get("allowed-values");
		if (isNullOrEmpty(allowedValues)) {
			throw new IllegalParameterException("allowed-values configuation setting is required");
		}
		if (containsControlCharacters(allowedValues)) {
			throw new IllegalParameterException("allowed-values contains control characters");
		}
		final String[] split = allowedValues.split(",");
		final Set<String> av = new HashSet<>();
		for (String s: split) {
			if (!s.trim().isEmpty()) {
				s = s.trim();
				if (codePoints(s) > MAX_LENGTH) {
					throw new IllegalParameterException(
							"allowed-values contains value longer than maximum length " +
							MAX_LENGTH);
				}
				av.add(s);
			}
		}
		return new EnumFieldValidator(av);
	}
	
	private class EnumFieldValidator implements FieldValidator {
		
		private final Set<String> allowedValues;
		
		private EnumFieldValidator(final Set<String> allowedValues) {
			this.allowedValues = allowedValues;
		}

		@Override
		public void validate(final String fieldValue) throws IllegalFieldValueException {
			if (!allowedValues.contains(fieldValue)) {
				throw new IllegalFieldValueException(String.format(
						"Value %s is not in the configured set of allowed values", fieldValue));
			}
		}
	}


}
