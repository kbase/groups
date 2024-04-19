package us.kbase.groups.core.exceptions;

/** Thrown when a required parameter was not provided.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class MissingParameterException extends GroupsException {

	//TODO TEST
	
	public MissingParameterException(final String message) {
		super(ErrorType.MISSING_PARAMETER, message);
	}
}
