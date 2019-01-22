package us.kbase.groups.core.fieldvalidation;

/** A validator for the string value of a field.
 * @author gaprice@lbl.gov
 *
 */
public interface FieldValidator {
	
	/** Validate that the field value is correct.
	 * @param fieldValue the value to validate.
	 * @throws IllegalFieldValueException if the value is illegal.
	 * @throws FieldValidatorException if the validation could not be completed.
	 */
	public void validate(final String fieldValue)
			throws IllegalFieldValueException, FieldValidatorException;
}
