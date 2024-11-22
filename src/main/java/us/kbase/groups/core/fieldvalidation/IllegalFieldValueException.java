package us.kbase.groups.core.fieldvalidation;

/** 
 * Thrown when a custom field contains an illegal value.
 * @author gaprice@lbl.gov
 *
 */
public class IllegalFieldValueException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public IllegalFieldValueException(String message) { super(message); }
	public IllegalFieldValueException(String message, Throwable cause) {
		super(message, cause);
	}
}
