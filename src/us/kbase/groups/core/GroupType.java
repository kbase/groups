package us.kbase.groups.core;

import java.util.HashMap;
import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;

/** The type of a group.
 * @author gaprice@lbl.gov
 *
 */
public enum GroupType {

	// NOTE: enum values are stored in the database, so do not change them
	
	ORGANIZATION	("Organization"),
	PROJECT			("Project"),
	TEAM			("Team");
	
	private static final Map<String, GroupType> TYPE_MAP = new HashMap<>();
	static {
		for (final GroupType gt: GroupType.values()) {
			TYPE_MAP.put(gt.getRepresentation(), gt);
		}
	}

	private final String representation;

	private GroupType(final String representation) {
		this.representation = representation;
	}
	
	/** Get the representation of this group type. Used for display purposes.
	 * @return the representation.
	 */
	public String getRepresentation() {
		return representation;
	}
	
	/** Get a group type based on a supplied representation.
	 * @param representation the representation of the group type as a string.
	 * @return a group type.
	 * @throws IllegalParameterException if there is no group type matching the representation.
	 */
	public static GroupType fromRepresentation(final String representation)
			throws IllegalParameterException {
		if (!TYPE_MAP.containsKey(representation)) {
			throw new IllegalParameterException("Invalid group type: " + representation);
		}
		return TYPE_MAP.get(representation);
	}
}
