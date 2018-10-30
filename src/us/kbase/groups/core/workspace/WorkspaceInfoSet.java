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
	private final Map<WorkspaceInformation, Boolean> isAdmin;
	private final Set<Integer> nonexistent;
	
	private WorkspaceInfoSet(
			final Optional<UserName> user,
			final Map<WorkspaceInformation, Boolean> isAdmin,
			final Set<Integer> nonexistent) {
		this.user = user;
		this.isAdmin = Collections.unmodifiableMap(isAdmin);
		this.nonexistent = Collections.unmodifiableSet(nonexistent);
	}
	
	/** Get the user associated with the set. The the admin status returned by
	 * {@link #isAdministrator(WorkspaceInformation)} refers to this user.
	 * @return the user, or {@link Optional#absent()} if the user is anonymous.
	 */
	public Optional<UserName> getUser() {
		return user;
	}
	
	/** Get the {@link WorkspaceInformation} in this set.
	 * @return
	 */
	public Set<WorkspaceInformation> getWorkspaceInformation() {
		return isAdmin.keySet();
	}
	
	/** Determine whether the user is an administrator of a particular workspace.
	 * @param wsInfo the workspace to check.
	 * @return true if the user is an administrator, false otherwise.
	 */
	public boolean isAdministrator(final WorkspaceInformation wsInfo) {
		checkNotNull(wsInfo, "wsInfo");
		if (!isAdmin.containsKey(wsInfo)) {
			throw new IllegalArgumentException("Provided workspace info not included in set");
		} else {
			return isAdmin.get(wsInfo);
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
		result = prime * result + ((isAdmin == null) ? 0 : isAdmin.hashCode());
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
		if (isAdmin == null) {
			if (other.isAdmin != null) {
				return false;
			}
		} else if (!isAdmin.equals(other.isAdmin)) {
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
	 * passed to {@link Builder#withWorkspaceInformation(WorkspaceInformation, boolean)} must
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
		private final Map<WorkspaceInformation, Boolean> isAdmin = new HashMap<>();
		private final Set<Integer> nonexistent = new HashSet<>();
		
		private Builder(final UserName user) {
			this.user = Optional.fromNullable(user);
		}
		
		/** Add a workspace to the builder.
		 * @param wsInfo the workspace information.
		 * @param isAdmin whether the user provided in
		 * {@link WorkspaceInfoSet#getBuilder(UserName)} is an administrator of the workspace.
		 * @return this builder.
		 */
		public Builder withWorkspaceInformation(
				final WorkspaceInformation wsInfo,
				final boolean isAdmin) {
			checkNotNull(wsInfo, "wsInfo");
			this.isAdmin.put(wsInfo, isAdmin);
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
			return new WorkspaceInfoSet(user, isAdmin, nonexistent);
		}
	}
}
