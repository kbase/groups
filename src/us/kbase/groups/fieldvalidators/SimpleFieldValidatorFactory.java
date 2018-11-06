package us.kbase.groups.fieldvalidators;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.codePoints;
import static us.kbase.groups.util.Util.containsControlCharacters;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;

public class SimpleFieldValidatorFactory implements FieldValidatorFactory {

	// TODO JAVADOC
	// TODO TEST
	// TODO VAL add force 1st char = letter
	// TODO VAL regex
	// TODO VAL regex for illegal characters
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		checkNotNull(configuration, "configuration");
		final int maxLength;
		if (isNullOrEmpty(configuration.get("max-length"))) {
			maxLength = -1;
		} else {
			try {
				maxLength = Integer.parseInt(configuration.get("max-length"));
				if (maxLength < 1) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				throw new IllegalParameterException("max-length parameter must be an integer > 0");
			}
		}
		final boolean allowLineFeedsAndTabs = "true".equals(
				configuration.get("allow-line-feeds-and-tabs"));
		return new SimpleFieldValidator(maxLength, allowLineFeedsAndTabs);
	}
	
	private static class SimpleFieldValidator implements FieldValidator {
		
		private final int maxLength;
		private final boolean allowLineFeedsAndTabs;
		
		private SimpleFieldValidator(
				final int maxLength,
				final boolean allowLineFeedsAndTabs) {
			this.maxLength = maxLength;
			this.allowLineFeedsAndTabs = allowLineFeedsAndTabs;
		}

		@Override
		public void validate(String fieldValue) throws IllegalFieldValueException {
			if (maxLength > 0 && codePoints(fieldValue) > maxLength) {
				throw new IllegalFieldValueException(
						"value is greater than maximum length " + maxLength);
			}
			if (allowLineFeedsAndTabs) {
				fieldValue = fieldValue.replace("\r", "");
				fieldValue = fieldValue.replace("\n", "");
				fieldValue = fieldValue.replace("\t", "");
			}
			if (containsControlCharacters(fieldValue)) {
				throw new IllegalFieldValueException("value contains control characters");
			}
		}
		
	}


}
