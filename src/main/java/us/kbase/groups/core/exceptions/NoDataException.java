package us.kbase.groups.core.exceptions;


/** Base class of all exceptions caused by trying to get data that doesn't
 * exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoDataException extends GroupsException {

	//TODO TEST
	
	public NoDataException(final ErrorType err, final String message) {
		super(err, message);
	}

	public NoDataException(final ErrorType err, final String message, final Throwable cause) {
		super(err, message, cause);
	}
}
