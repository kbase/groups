package us.kbase.groups.storage.exceptions;

/** 
 * Thrown when an exception occurs regarding initialization of the groups storage system
 * @author gaprice@lbl.gov
 *
 */
public class StorageInitException extends GroupsStorageException {

	private static final long serialVersionUID = 1L;
	
	public StorageInitException(String message) { super(message); }
	public StorageInitException(String message, Throwable cause) {
		super(message, cause);
	}
}
