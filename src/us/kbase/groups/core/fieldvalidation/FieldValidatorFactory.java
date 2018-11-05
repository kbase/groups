package us.kbase.groups.core.fieldvalidation;

import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;

public interface FieldValidatorFactory {

	// TODO JAVADOC
	
	FieldValidator getValidator(Map<String, String> configuration)
			throws IllegalParameterException;
	
}
