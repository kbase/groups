package us.kbase.groups.core.exceptions;

/** Thrown when a request is closed and the requested operation is therefore not permitted..
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class ClosedRequestException extends GroupsException {

	//TODO TEST
	
	public ClosedRequestException(final String message) {
		super(ErrorType.REQUEST_CLOSED, message);
	}
}
