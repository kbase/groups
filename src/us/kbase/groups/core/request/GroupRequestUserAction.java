package us.kbase.groups.core.request;

/** An action that a user may take for a request.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupRequestUserAction {

	/** Cancel a request. Only the creator may cancel their requests. */
	CANCEL	("Cancel"),
	/** Accept a request. Who can accept a request depends on the target of a request, but
	 * will be one of a particular user to whom a request is targeted, the admins of a group
	 * to which a request is targeted, or admins of a workspace to which a request is targeted.
	 */
	ACCEPT	("Accept"),
	/** Deny a request. Who can deny a request depends on the target of a request, but
	 * will be one of a particular user to whom a request is targeted, the admins of a group
	 * to which a request is targeted, or admins of a workspace to which a request is targeted.
	 */
	DENY	("Deny");
	
	private final String representation;
	
	private GroupRequestUserAction(final String representation) {
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
