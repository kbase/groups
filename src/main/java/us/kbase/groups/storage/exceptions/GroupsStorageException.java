package us.kbase.groups.storage.exceptions;

/** 
 * Thrown when an exception occurs regarding the groups storage system.
 * @author gaprice@lbl.gov
 *
 */
public class GroupsStorageException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public GroupsStorageException(String message) { super(message); }
	public GroupsStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
