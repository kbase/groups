package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Optional;

import us.kbase.groups.core.Group.Role;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

/** Parameters for getting a list of groups.
 * @author gaprice@lbl.gov
 *
 */
public class GetGroupsParams {
	
	// pretty similar to GetRequestsParams, but inheritance + builders is a pain.
	
	private final boolean sortAscending;
	private final Optional<String> excludeUpTo;
	private final Role role;
	private final Optional<ResourceType> resourceType;
	private final Optional<ResourceID> resourceID;
	
	private GetGroupsParams(
			final boolean sortAscending,
			final Optional<String> excludeUpTo,
			final Role role,
			final Optional<ResourceType> resourceType,
			final Optional<ResourceID> resourceID) {
		this.sortAscending = sortAscending;
		this.excludeUpTo = excludeUpTo;
		this.role = role;
		this.resourceType = resourceType;
		this.resourceID = resourceID;
	}

	/** Get whether the list should be sorted in ascending or descending order.
	 * @return true if the sort should be ascending, false if descending.
	 */
	public boolean isSortAscending() {
		return sortAscending;
	}

	/** Get a string that determines where a list of groups should begin. If the sort is
	 * ascending, the group list should begin at a string strictly after this string, and
	 * vice versa for descending sorts.
	 * @return the exclusion string.
	 */
	public Optional<String> getExcludeUpTo() {
		return excludeUpTo;
	}
	
	/** Get the minimum role the user should have in the returned groups.
	 * @return the role.
	 */
	public Role getRole() {
		return role;
	}
	
	/** Get the resource type that must limit the list of groups. If the type is present,
	 * {@link #getResourceID()} will always return a resource ID. The combination of the two
	 * must limit the list of groups.
	 * @return the resource type that all the groups must possess.
	 */
	public Optional<ResourceType> getResourceType() {
		return resourceType;
	}
	
	/** Get the resource ID that must limit the list of groups. If the ID is present,
	 * {@link #getResourceType()} will always return a resource type. The combination of the two
	 * must limit the list of groups.
	 * @return the resource ID that all the groups must possess.
	 */
	public Optional<ResourceID> getResourceID() {
		return resourceID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((excludeUpTo == null) ? 0 : excludeUpTo.hashCode());
		result = prime * result + ((resourceID == null) ? 0 : resourceID.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + (sortAscending ? 1231 : 1237);
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
		GetGroupsParams other = (GetGroupsParams) obj;
		if (excludeUpTo == null) {
			if (other.excludeUpTo != null) {
				return false;
			}
		} else if (!excludeUpTo.equals(other.excludeUpTo)) {
			return false;
		}
		if (resourceID == null) {
			if (other.resourceID != null) {
				return false;
			}
		} else if (!resourceID.equals(other.resourceID)) {
			return false;
		}
		if (resourceType == null) {
			if (other.resourceType != null) {
				return false;
			}
		} else if (!resourceType.equals(other.resourceType)) {
			return false;
		}
		if (role != other.role) {
			return false;
		}
		if (sortAscending != other.sortAscending) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link GetGroupsParams}.
	 * @return the builder.
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/** A builder for a {@link GetGroupsParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private boolean sortAscending = true;
		private Optional<String> excludeUpTo = Optional.empty();
		private Role role = Role.NONE;
		private Optional<ResourceType> resourceType = Optional.empty();
		private Optional<ResourceID> resourceID = Optional.empty();
		
		private Builder() {}
		
		/** Set whether the list should be sorted in ascending or descending order.
		 * If null, the default of true is used.
		 * @param sortAscending true sort ascending, false for descending.
		 * @return this builder.
		 */
		public Builder withNullableSortAscending(final Boolean sortAscending) {
			if (sortAscending == null) {
				this.sortAscending = true;
			} else {
				this.sortAscending = sortAscending;
			}
			return this;
		}
		
		/** Set a string that determines where a list of groups should begin. If the sort is
		 * ascending, the group list should begin at a string strictly after this string,
		 * and vice versa for descending sorts.
		 * If null or whitespace only, no string is set.
		 * The string is {@link String#trim()}ed.
		 * @param excludeUpTo the exclusion string.
		 * @return this builder.
		 */
		public Builder withNullableExcludeUpTo(final String excludeUpTo) {
			if (isNullOrEmpty(excludeUpTo)) {
				this.excludeUpTo = Optional.empty();
			} else {
				this.excludeUpTo = Optional.of(excludeUpTo.trim());
			}
			return this;
		}
		
		/** Set the minimum role the user must have in the returned groups.
		 * @param role the role.
		 * @return this builder.
		 */
		public Builder withRole(final Role role) {
			this.role = requireNonNull(role, "role");
			return this;
		}
		
		/** Set a resource ID that limits the list of groups to include only groups
		 * that contain that resource ID.
		 * @param type the type of the resource.
		 * @param id the resource ID.
		 * @return this builder.
		 */
		public Builder withResource(final ResourceType type, final ResourceID id) {
			this.resourceType = Optional.of(requireNonNull(type, "type"));
			this.resourceID = Optional.of(requireNonNull(id, "id"));
			return this;
		}
		
		/** Build the {@link GetGroupsParams}.
		 * @return the params.
		 */
		public GetGroupsParams build() {
			return new GetGroupsParams(sortAscending, excludeUpTo, role, resourceType, resourceID);
		}
	}
}
