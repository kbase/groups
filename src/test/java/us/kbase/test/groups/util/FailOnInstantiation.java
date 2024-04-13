package us.kbase.test.groups.util;

import java.util.Map;

import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;

public class FailOnInstantiation implements FieldValidatorFactory {

	public FailOnInstantiation() {
		throw new IllegalArgumentException("foo");
	}
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration) {
		return null;
	}

	
}
