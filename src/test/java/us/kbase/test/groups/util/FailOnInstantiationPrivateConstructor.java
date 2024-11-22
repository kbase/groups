package us.kbase.test.groups.util;

import java.util.Map;

import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;

public class FailOnInstantiationPrivateConstructor implements FieldValidatorFactory {

	private FailOnInstantiationPrivateConstructor() {}
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration) {
		return null;
	}

	
}
