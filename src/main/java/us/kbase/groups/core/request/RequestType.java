package us.kbase.groups.core.request;

/** The type of the request, either a request to add a resource to a group or an invitation
 * to add a resource to a group.
 * @author gaprice@lbl.gov
 *
 */
public enum RequestType {
	
	// NOTE do not change the enum values. They may be stored in a DB.

	/** A request type for adding a resource to a group where the requester is not a group
	 * administrator, but is responsible for the resource.
	 */
	REQUEST		("Request"),
	/** A request type for adding a resource to a group where the requester is a group
	 * administrator, but is not responsible for the resource.
	 */
	INVITE		("Invite");
	
	private final String representation;

	private RequestType(final String representation) {
		this.representation = representation;
	}
	
	/** Get the request type representation, which should be used for presentation purposes.
	 * @return the representation.
	 */
	public String getRepresentation() {
		return representation;
	}
}
