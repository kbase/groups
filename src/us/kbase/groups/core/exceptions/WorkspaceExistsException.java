package us.kbase.groups.core.exceptions;

/** Thrown when a workspace already exists in a group.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class WorkspaceExistsException extends GroupsException {

	//TODO TEST
	
	public WorkspaceExistsException(final String message) {
		super(ErrorType.WORKSPACE_IN_GROUP, message);
	}

	public WorkspaceExistsException(
			final String message,
			final Throwable cause) {
		super(ErrorType.WORKSPACE_IN_GROUP, message, cause);
	}
}
