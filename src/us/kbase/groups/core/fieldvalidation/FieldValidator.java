package us.kbase.groups.core.fieldvalidation;

public interface FieldValidator {
	
	// TODO JAVADOC

	public void validate(final String fieldValue) throws IllegalFieldValueException;
}
