package us.kbase.groups.core.workspace;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum WorkspacePermission {
	
	// TODO JAVADOC
	// TODO TEST

	NONE	("None", "n"),
	READ	("Read", "r"),
	WRITE	("Write", "w"),
	ADMIN	("Admin", "a"),
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

	public String getRepresentation() {
		return representation;
	}

	public Optional<String> getWorkspaceRepresentation() {
		return Optional.ofNullable(workspaceRepresentation);
	}
	
	public boolean isAdmin() {
		return this.equals(ADMIN) || this.equals(OWN);
	}
	
	public static WorkspacePermission fromRepresentation(final String representation) {
		return getRepresentation(representation, REPS, "");
	}
	
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
					"No such %srepresentation %s", err, representation));
		}
		return reps.get(representation);
	}
}
