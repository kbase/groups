package us.kbase.groups.core.workspace;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A workspace service user permission for a particular workspace..
 * @author gaprice@lbl.gov
 *
 */
public enum WorkspacePermission {
	
	/** No permissions. */
	NONE	("None", "n"),
	/** Read permissions. */
	READ	("Read", "r"),
	/** Write permissions. */
	WRITE	("Write", "w"),
	/** Administrator permissions. */
	ADMIN	("Admin", "a"),
	/** Owner permissions. */
	OWN		("Own", null);
	
	private final String representation;
	private final String workspaceRepresentation;
	
	private static final Map<String, WorkspacePermission> REPS = new HashMap<>();
	private static final Map<String, WorkspacePermission> WSREPS = new HashMap<>();
	static {
		for (final WorkspacePermission ws: values()) {
			REPS.put(ws.representation, ws);
			if (!ws.equals(OWN)) {
				WSREPS.put(ws.workspaceRepresentation, ws);
			}
		}
	}
	
	private WorkspacePermission(
			final String representation,
			final String workspaceRepresentation) {
		this.representation = representation;
		this.workspaceRepresentation = workspaceRepresentation;
	}

	/** Get a representation of the workspace permission.
	 * @return the representation.
	 */
	public String getRepresentation() {
		return representation;
	}

	/** Get the workspace service's representation of the permission. {@link Optional#empty()}
	 * for owner permissions.
	 * @return the representation.
	 */
	public Optional<String> getWorkspaceRepresentation() {
		return Optional.ofNullable(workspaceRepresentation);
	}
	
	/** Check if this permission is an administration permission.
	 * @return true if this permission is {@link WorkspacePermission#ADMIN} or
	 * {@link WorkspacePermission#OWN}.
	 */
	public boolean isAdmin() {
		return this.equals(ADMIN) || this.equals(OWN);
	}
	
	/** Get a workspace permission from a representation.
	 * @param representation the representation.
	 * @return the permission.
	 */
	public static WorkspacePermission fromRepresentation(final String representation) {
		return getRepresentation(representation, REPS, "");
	}

	/** Get a workspace permission from a workspace representation.
	 * @param workspaceRepresentation the representation.
	 * @return the permission.
	 */
	public static WorkspacePermission fromWorkspaceRepresentation(
			final String workspaceRepresentation) {
		return getRepresentation(workspaceRepresentation, WSREPS, "workspace ");
	}
	
	private static WorkspacePermission getRepresentation(
			final String representation,
			final Map<String, WorkspacePermission> reps,
			final String err) {
		if (!reps.containsKey(representation)) {
			throw new IllegalArgumentException(String.format(
					"No such %srepresentation: %s", err, representation));
		}
		return reps.get(representation);
	}
}
