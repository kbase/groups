package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/** Contains the group ID, whether the user that is associated with this object is a member of
 * the group, and the group's privacy status. May include the group name if it was added by the
 * creator of this object and the user is a group member or the group is not private.
 * @author gaprice@lbl.gov
 *
 */
public class GroupIDNameMembership {
	
	private final GroupID groupID;
	private final Optional<GroupName> groupName;
	private final boolean isMember;
	private final boolean isPrivate;
	
	private GroupIDNameMembership(
			final GroupID groupID,
			final Optional<GroupName> groupName,
			final boolean isMember,
			final boolean isPrivate) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.isMember = isMember;
		this.isPrivate = isPrivate;
	}
	
	/** Get the group ID.
	 * @return  the group ID.
	 */
	public GroupID getID() {
		return groupID;
	}

	/** Get the group name. Will be {@link Optional#empty()} if it was not provided during
	 * object build, or if the user is not a member of the group and the group is private.
	 * @return the group name.
	 */
	public Optional<GroupName> getName() {
		return groupName;
	}

	/** Get whether the user associated with this object is a member of the group.
	 * @return true if the user is a group member.
	 */
	public boolean isMember() {
		return isMember;
	}

	/** Get whether the group is private.
	 * @return true if the group is private.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isMember ? 1231 : 1237);
		result = prime * result + (isPrivate ? 1231 : 1237);
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
		GroupIDNameMembership other = (GroupIDNameMembership) obj;
		if (groupID == null) {
			if (other.groupID != null) {
				return false;
			}
		} else if (!groupID.equals(other.groupID)) {
			return false;
		}
		if (groupName == null) {
			if (other.groupName != null) {
				return false;
			}
		} else if (!groupName.equals(other.groupName)) {
			return false;
		}
		if (isMember != other.isMember) {
			return false;
		}
		if (isPrivate != other.isPrivate) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link GroupIDNameMembership}.
	 * @param groupID the id of the group.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final GroupID groupID) {
		return new Builder(groupID);
	}
	
	/** A builder for a {@link GroupIDNameMembership}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {

		private final GroupID groupID;
		private Optional<GroupName> groupName = Optional.empty();
		private boolean isMember = false;
		private boolean isPrivate = true;
		
		private Builder(GroupID groupID) {
			this.groupID = requireNonNull(groupID, "groupID");
		}
		
		/** Add a group name to the builder. The name will be set to {@link Optional#empty()}
		 * on build if the group is private and the user associated with this object is not
		 * a member of the group.
		 * @param groupName the group name.
		 * @return this builder.
		 */
		public Builder withGroupName(final GroupName groupName) {
			this.groupName = Optional.of(requireNonNull(groupName, "groupName"));
			return this;
		}
		
		/** Set whether the user associated with this object is a group member.
		 * @param isMember true to set the user as a group member.
		 * @return this builder.
		 */
		public Builder withIsMember(final boolean isMember) {
			this.isMember = isMember;
			return this;
		}
		
		/** Set whether the group is private.
		 * @param isPrivate true to set the group as private.
		 * @return this builder.
		 */
		public Builder withIsPrivate(final boolean isPrivate) {
			this.isPrivate = isPrivate;
			return this;
		}
		
		/** Build the {@link GroupIDNameMembership}.
		 * @return the built object.
		 */
		public GroupIDNameMembership build() {
			Optional<GroupName> name = groupName;
			if (!isMember && isPrivate) {
				name = Optional.empty();
			}
			return new GroupIDNameMembership(groupID, name, isMember, isPrivate);
		}
	}
}
