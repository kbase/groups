package us.kbase.groups.core.fieldvalidation;

import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;

/** A factory for a {@link FieldValidator}. The factory is responsible for creating the validator
 * instance given a configuration. The configuration will differ from implementation to
 * implementation of the factory / validator combination.
 * @author gaprice@lbl.gov
 *
 */
public interface FieldValidatorFactory {

	/** Get a validator given a configuration.
	 * @param configuration the configuration.
	 * @return a validator.
	 * @throws IllegalParameterException if the configuration was invalid.
	 */
	FieldValidator getValidator(Map<String, String> configuration)
			throws IllegalParameterException;
	
}
