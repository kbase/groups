package us.kbase.groups.core.fieldvalidation;

/** 
 * Thrown when a field validator cannot validate the field for reasons other than the contents of
 * the field.
 * @author gaprice@lbl.gov
 *
 */
public class FieldValidatorException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public FieldValidatorException(String message) { super(message); }
	public FieldValidatorException(String message, Throwable cause) {
		super(message, cause);
	}
}
