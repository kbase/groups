package us.kbase.groups.core.workspace;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import us.kbase.groups.core.UserName;

/** A set of {@link WorkspaceInformation}.
 * @author gaprice@lbl.gov
 *
 */
public class WorkspaceInfoSet {

	private final Optional<UserName> user;
	// might need to think about sorting here. YAGNI for now.
	private final Map<WorkspaceInformation, WorkspacePermission> perms;
	private final Set<Integer> nonexistent;
	
	private WorkspaceInfoSet(
			final Optional<UserName> user,
			final Map<WorkspaceInformation, WorkspacePermission> perms,
			final Set<Integer> nonexistent) {
		this.user = user;
		this.perms = Collections.unmodifiableMap(perms);
		this.nonexistent = Collections.unmodifiableSet(nonexistent);
	}
	
	/** Get the user associated with the set. The the permission returned by
	 * {@link #getPermission(WorkspaceInformation)} refers to this user.
	 * @return the user, or {@link Optional#absent()} if the user is anonymous.
	 */
	public Optional<UserName> getUser() {
		return user;
	}
	
	/** Get the {@link WorkspaceInformation} in this set.
	 * @return the workspace information.
	 */
	public Set<WorkspaceInformation> getWorkspaceInformation() {
		return perms.keySet();
	}
	
	/** Get the user's permission to a particular workspace.
	 * @param wsInfo the workspace to check.
	 * @return the user's permission.
	 */
	public WorkspacePermission getPermission(final WorkspaceInformation wsInfo) {
		checkNotNull(wsInfo, "wsInfo");
		if (!perms.containsKey(wsInfo)) {
			throw new IllegalArgumentException("Provided workspace info not included in set");
		} else {
			return perms.get(wsInfo);
		}
	}
	
	/** Get the workspace IDs of any workspaces that were found to be deleted or missing
	 * altogether when building this set.
	 * @return the workspace IDs.
	 */
	public Set<Integer> getNonexistentWorkspaces() {
		return nonexistent;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((perms == null) ? 0 : perms.hashCode());
		result = prime * result + ((nonexistent == null) ? 0 : nonexistent.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WorkspaceInfoSet other = (WorkspaceInfoSet) obj;
		if (perms == null) {
			if (other.perms != null) {
				return false;
			}
		} else if (!perms.equals(other.perms)) {
			return false;
		}
		if (nonexistent == null) {
			if (other.nonexistent != null) {
				return false;
			}
		} else if (!nonexistent.equals(other.nonexistent)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link WorkspaceInfoSet}.
	 * @param user the user associated with this set. The workspace admininstration information
	 * passed to
	 * {@link Builder#withWorkspaceInformation(WorkspaceInformation, WorkspacePermission)} must
	 * be based on this user. Pass null for an anonymous user.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final UserName user) {
		return new Builder(user);
	}
	
	/** A builder for {@link WorkspaceInfoSet}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {

		private final Optional<UserName> user;
		// might need to think about sorting here. YAGNI for now.
		private final Map<WorkspaceInformation, WorkspacePermission> perms = new HashMap<>();
		private final Set<Integer> nonexistent = new HashSet<>();
		
		private Builder(final UserName user) {
			this.user = Optional.fromNullable(user);
		}
		
		/** Add a workspace to the builder.
		 * @param wsInfo the workspace information.
		 * @param permission the user provided in {@link WorkspaceInfoSet#getBuilder(UserName)}'s
		 * permission for the workspace.
		 * @return this builder.
		 */
		public Builder withWorkspaceInformation(
				final WorkspaceInformation wsInfo,
				final WorkspacePermission permission) {
			checkNotNull(wsInfo, "wsInfo");
			checkNotNull(permission, "permission");
			this.perms.put(wsInfo, permission);
			return this;
		}
		
		/** Add a workspace id that was found to be missing or deleted altogether when building
		 * this set.
		 * @param wsid the deleted or missing workspace ID.
		 * @return this builder.
		 */
		public Builder withNonexistentWorkspace(final int wsid) {
			if (wsid < 1) {
				throw new IllegalArgumentException("Workspace IDs must be > 0");
			}
			nonexistent.add(wsid);
			return this;
		}
		
		/** Build the {@link WorkspaceInfoSet}.
		 * @return the new set.
		 */
		public WorkspaceInfoSet build() {
			return new WorkspaceInfoSet(user, perms, nonexistent);
		}
	}
}
