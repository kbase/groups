package us.kbase.groups.core.exceptions;

/** Thrown when the specified user does not exist within some context, like a group.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchUserException extends NoDataException {

	//TODO TEST
	
	public NoSuchUserException(final String message) {
		super(ErrorType.NO_SUCH_USER, message);
	}
}