package us.kbase.groups.core;

import us.kbase.groups.core.request.RequestType;

/** An enum representing whether a particular group has open incoming
 * (e.g. of type {@link RequestType#REQUEST}) requests.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupHasRequests {
	
	/** The group has no open incoming requests. */
	NONE	("None"),
	/** The group has open incoming requests that are older than some specified date. */
	OLD		("Old"),
	/** The group has open incoming requests that are older than some specified date,
	 *  or has any open requests at all if no date is specified.
	 */
	NEW		("New");
	
	private final String representation;
	
	private GroupHasRequests(final String representation) {
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
