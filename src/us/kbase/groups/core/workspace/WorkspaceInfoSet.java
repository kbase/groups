package us.kbase.groups.core.workspace;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jersey.repackaged.com.google.common.base.Optional;
import us.kbase.groups.core.UserName;

public class WorkspaceInfoSet {

	// TODO JAVADOC
	// TODO TEST
	
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
	
	// absent if anon user
	public Optional<UserName> getUser() {
		return user;
	}
	
	public Set<WorkspaceInformation> getWorkspaceInformation() {
		return isAdmin.keySet();
	}
	
	public boolean isAdministrator(final WorkspaceInformation wsInfo) {
		checkNotNull(wsInfo);
		if (!isAdmin.containsKey(wsInfo)) {
			throw new IllegalArgumentException("Provided workspace info not included in set");
		} else {
			return isAdmin.get(wsInfo);
		}
	}
	
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
	
	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("WorkspaceInfoSet [user=");
		builder2.append(user);
		builder2.append(", isAdmin=");
		builder2.append(isAdmin);
		builder2.append(", nonexistant=");
		builder2.append(nonexistent);
		builder2.append("]");
		return builder2.toString();
	}

	// pass null for an anonymous user.
	public static Builder getBuilder(final UserName user) {
		return new Builder(user);
	}
	
	public static class Builder {

		private final Optional<UserName> user;
		// might need to think about sorting here. YAGNI for now.
		private final Map<WorkspaceInformation, Boolean> isAdmin = new HashMap<>();
		private final Set<Integer> nonexistent = new HashSet<>();
		
		public Builder(final UserName user) {
			this.user = Optional.fromNullable(user);
		}
		
		public Builder withWorkspaceInformation(
				final WorkspaceInformation wsInfo,
				final boolean isAdmin) {
			checkNotNull(wsInfo, "wsInfo");
			this.isAdmin.put(wsInfo, isAdmin);
			return this;
		}
		
		public Builder withNonexistentWorkspace(final int wsid) {
			nonexistent.add(wsid);
			return this;
		}
		
		public WorkspaceInfoSet build() {
			return new WorkspaceInfoSet(user, isAdmin, nonexistent);
		}
	}
}
