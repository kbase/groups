package us.kbase.groups.core.exceptions;

/** Thrown when the specified group does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchGroupException extends NoDataException {

	//TODO TEST
	
	public NoSuchGroupException(final String message) {
		super(ErrorType.NO_SUCH_GROUP, message);
	}
}
