package us.kbase.groups.core.request;

/** The type of a request to modify a group.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupRequestType {

	// NOTE the enum values are stored in the DB. Don't change them.
	
	/** Invite a user to a group you administrate. */
	INVITE_TO_GROUP				("Invite to group"),
	/** Request membership to a group. */
	REQUEST_GROUP_MEMBERSHIP	("Request group membership"),
	/** Invite a workspace (via its administrators) to a group you administrate. */
	INVITE_WORKSPACE			("Invite workspace to group"),
	/** Request that a workspace you administrate is added to a group. */
	REQUEST_ADD_WORKSPACE		("Request add workspace to group"),
	/** Invite a catalog method (via its owners) to a group you administrate. */
	INVITE_CATALOG_METHOD		("Invite catalog method to group"),
	/** Request that a catalog method you own is added to a group. */
	REQUEST_ADD_CATALOG_METHOD	("Request add catalog method to group");
	
	private final String representation;
	
	private GroupRequestType(final String representation) {
		this.representation = representation;
	}

	// may need a rep -> enum lookup fn. YAGNI for now.
	
	/** Get a representation of the enum that should be used for presentation purposes.
	 * @return the representation.
	 */
	public String getRepresentation() {
		return representation;
	}
}
