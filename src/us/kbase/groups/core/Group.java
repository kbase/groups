package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;

import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;

/** Represents a group consisting primarily of a set of users and set of workspaces associated
 * with those users.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class Group {
	
	public static final int MAX_DESCRIPTION_CODE_POINTS = 5000;
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final UserName owner;
	private final Set<UserName> members;
	private final Set<UserName> admins;
	private final GroupType type;
	private final WorkspaceIDSet workspaceIDs;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Optional<String> description;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final Set<UserName> members,
			final Set<UserName> admins,
			final GroupType type,
			final Set<Integer> workspaceIDs,
			final CreateAndModTimes times,
			final Optional<String> description) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.members = Collections.unmodifiableSet(members);
		this.admins = Collections.unmodifiableSet(admins);
		this.type = type;
		this.workspaceIDs = WorkspaceIDSet.fromInts(workspaceIDs);
		this.creationDate = times.getCreationTime();
		this.modificationDate = times.getModificationTime();
		this.description = description;
	}

	/** The ID of the group.
	 * @return the ID.
	 */
	public GroupID getGroupID() {
		return groupID;
	}

	/** The name of the group.
	 * @return the name.
	 */
	public GroupName getGroupName() {
		return groupName;
	}

	/** Get the user that owns the group.
	 * @return the owner.
	 */
	public UserName getOwner() {
		return owner;
	}
	
	/** Get the members of the group.
	 * @return the members.
	 */
	public Set<UserName> getMembers() {
		return members;
	}
	
	/** Get the administrators of the group. The owner is not included in this list, although
	 * the owner is included as an administrator in {@link #isAdministrator(UserName)}.
	 * @return the administrators.
	 */
	public Set<UserName> getAdministrators() {
		return admins;
	}

	/** Get the type of the group.
	 * @return the group type.
	 */
	public GroupType getType() {
		return type;
	}
	
	/** Get the workspace IDs associated with the group.
	 * @return the IDs.
	 */
	public WorkspaceIDSet getWorkspaceIDs() {
		return workspaceIDs;
	}

	/** Get the date the group was created.
	 * @return the creation date.
	 */
	public Instant getCreationDate() {
		return creationDate;
	}

	/** Get the date the group was modified.
	 * @return the modification date.
	 */
	public Instant getModificationDate() {
		return modificationDate;
	}

	/** Get the description of the group, if any.
	 * @return the description or {@link Optional#absent()}.
	 */
	public Optional<String> getDescription() {
		return description;
	}
	
	/** Check if a user is a group administrator.
	 * @param user the user to check.
	 * @return true if the user is a group administrator, false otherwise.
	 */
	public boolean isAdministrator(final UserName user) {
		checkNotNull(user, "user");
		return owner.equals(user) || admins.contains(user);
	}
	
	/** Get the administrators, including the owner, of the group.
	 * @return the administrators.
	 */
	public Set<UserName> getAdministratorsAndOwner() {
		final HashSet<UserName> admins = new HashSet<>(this.admins);
		admins.add(owner);
		return admins;
	}
	
	/** Check if a user is a member of the group. 'Member' includes the group owner and
	 * administrators.
	 * @param user the user to check. Pass null for anonymous users.
	 * @return true if the user is a group member, false otherwise.
	 */
	public boolean isMember(final UserName user) {
		return owner.equals(user) || members.contains(user) || admins.contains(user);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((admins == null) ? 0 : admins.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((workspaceIDs == null) ? 0 : workspaceIDs.hashCode());
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
		Group other = (Group) obj;
		if (admins == null) {
			if (other.admins != null) {
				return false;
			}
		} else if (!admins.equals(other.admins)) {
			return false;
		}
		if (creationDate == null) {
			if (other.creationDate != null) {
				return false;
			}
		} else if (!creationDate.equals(other.creationDate)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
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
		if (members == null) {
			if (other.members != null) {
				return false;
			}
		} else if (!members.equals(other.members)) {
			return false;
		}
		if (modificationDate == null) {
			if (other.modificationDate != null) {
				return false;
			}
		} else if (!modificationDate.equals(other.modificationDate)) {
			return false;
		}
		if (owner == null) {
			if (other.owner != null) {
				return false;
			}
		} else if (!owner.equals(other.owner)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		if (workspaceIDs == null) {
			if (other.workspaceIDs != null) {
				return false;
			}
		} else if (!workspaceIDs.equals(other.workspaceIDs)) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link Group}.
	 * @param id the group ID.
	 * @param name the group name.
	 * @param owner the owner of the group.
	 * @param times the creation and modification times for the group.
	 * @return the new builder.
	 */
	public static Builder getBuilder(
			final GroupID id,
			final GroupName name,
			final UserName owner,
			final CreateAndModTimes times) {
		return new Builder(id, name, owner, times);
	}
	
	/** A builder for a {@link Group}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private final UserName owner;
		private final CreateAndModTimes times;
		private final Set<UserName> members = new HashSet<>();
		private final Set<UserName> admins = new HashSet<>();
		private GroupType type = GroupType.ORGANIZATION;
		private final Set<Integer> workspaceIDs = new HashSet<>();
		private Optional<String> description = Optional.absent();
		
		private Builder(
				final GroupID id,
				final GroupName name,
				final UserName owner,
				final CreateAndModTimes times) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			checkNotNull(owner, "owner");
			checkNotNull(times, "times");
			this.groupID = id;
			this.groupName = name;
			this.owner = owner;
			this.times = times;
		}
		
		/** Change the type of the group. The default type is {@link GroupType#ORGANIZATION}.
		 * @param type the new type.
		 * @return this builder.
		 */
		public Builder withType(final GroupType type) {
			checkNotNull(type, "type");
			this.type = type;
			return this;
		}
		
		/** Add a group description. The maximum description size is
		 * {@link Group#MAX_DESCRIPTION_CODE_POINTS} Unicode code points.
		 * @param description the new description. If null or whitespace only, the description
		 * is set to {@link Optional#absent()}. The description will be {@link String#trim()}ed.
		 * @return this builder.
		 */
		public Builder withDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = Optional.absent();
			} else {
				if (description.codePointCount(0, description.length()) >
						MAX_DESCRIPTION_CODE_POINTS) {
					throw new IllegalArgumentException(
							"description must be <= 5000 Unicode code points");
				}
				this.description = Optional.of(description.trim());
			}
			return this;
		}
		
		/** Add a member to the builder.
		 * @param member the member.
		 * @return this builder.
		 * @throws IllegalArgumentException if the member is already the owner of the group
		 * or an administrator.
		 */
		public Builder withMember(final UserName member) {
			checkNotNull(member, "member");
			if (owner.equals(member) || admins.contains(member)) {
				throw new IllegalArgumentException(
						"Group already contains member as owner or admin");
			}
			this.members.add(member);
			return this;
		}
		
		/** Add an admin to the builder.
		 * @param admin the administrator.
		 * @return this builder.
		 * @throws IllegalArgumentException if the admin is already the owner of the group
		 * or an member.
		 */
		public Builder withAdministrator(final UserName admin) {
			checkNotNull(admin, "admin");
			if (owner.equals(admin) || members.contains(admin)) {
				throw new IllegalArgumentException(
						"Group already contains member as owner or member");
			}
			this.admins.add(admin);
			return this;
		}
		
		public Builder withWorkspace(final WorkspaceID wsid) {
			checkNotNull(wsid, "wsid");
			workspaceIDs.add(wsid.getID());
			return this;
		}
		
		/** Build the {@link Group}.
		 * @return the new group.
		 */
		public Group build() {
			return new Group(groupID, groupName, owner, members, admins, type, workspaceIDs, times,
					description);
		}
	}
	
}
