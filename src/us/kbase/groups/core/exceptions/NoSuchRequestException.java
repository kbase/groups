package us.kbase.groups.core.exceptions;

/** Thrown when the specified request does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchRequestException extends NoDataException {

	//TODO TEST
	
	public NoSuchRequestException(final String message) {
		super(ErrorType.NO_SUCH_GROUP, message);
	}
}
