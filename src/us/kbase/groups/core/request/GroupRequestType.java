package us.kbase.groups.core.request;

/** The type of a request to modify a group.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupRequestType {

	// NOTE the enum values are stored in the DB. Don't change them.
	
	INVITE_TO_GROUP				("Invite to group"),
	REQUEST_GROUP_MEMBERSHIP	("Request group membership");
	
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
