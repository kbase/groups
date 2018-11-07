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

/** Validates that a field is one of a set of values. It takes one required parameter,
 * allowed-values, that is a comma separated list of the allowed values. The values may not
 * contain control characters and may not be longer than 50 Unicode code points.
 * @author gaprice@lbl.gov
 *
 */
public class EnumFieldValidatorFactory implements FieldValidatorFactory {

	private static final int MAX_LENGTH = 50;
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		final String allowedValues = configuration.get("allowed-values");
		if (isNullOrEmpty(allowedValues)) {
			throw new IllegalParameterException(
					"allowed-values configuration setting is required");
		}
		if (containsControlCharacters(allowedValues)) {
			throw new IllegalParameterException("allowed-values contains control characters");
		}
		final Set<String> av = new HashSet<>();
		for (String s: allowedValues.split(",")) {
			s = s.trim();
			if (!s.isEmpty()) {
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
