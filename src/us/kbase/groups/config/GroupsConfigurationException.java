package us.kbase.groups.config;

/** Thrown when a configuration is invalid.
 * @author gaprice@lbl.gov
 *
 */
public class GroupsConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	public GroupsConfigurationException(final String message) {
		super(message);
	}
	
	public GroupsConfigurationException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}
}