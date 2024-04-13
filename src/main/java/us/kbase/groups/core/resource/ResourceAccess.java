package us.kbase.groups.core.resource;

/** An enum designating what resources should be made available to a user.
 * @author gaprice@lbl.gov
 *
 */
public enum ResourceAccess {
	
	/** All resources. */
	ALL,
	
	/** Resources administrated by the user and public resources. */
	ADMINISTRATED_AND_PUBLIC,
	
	/** Resources administrated by the user. */
	ADMINISTRATED;

}
