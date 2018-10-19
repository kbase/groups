package us.kbase.groups.core.request;

/** The type of the status of a group request. Either {@link #OPEN} or one of the four closed
 * types.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupRequestStatusType {
	
	// NOTE do not change the enum values. They may be stored in a DB.
	
	OPEN		("Open"),
	ACCEPTED	("Accepted"),
	DENIED		("Denied"),
	CANCELED	("Canceled"),
	EXPIRED		("Expired");
	
	private final String representation;
	
	private GroupRequestStatusType(final String representation) {
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
