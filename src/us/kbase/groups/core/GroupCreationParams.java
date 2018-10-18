package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import com.google.common.base.Optional;

public class GroupCreationParams {
	
	// TODO JAVADOC
	// TODO TESTS
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final GroupType type;
	private final Optional<String> description;
	
	private GroupCreationParams(
			final GroupID groupID,
			final GroupName groupName,
			final GroupType type,
			final Optional<String> description) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.type = type;
		this.description = description;
	}

	public GroupID getGroupID() {
		return groupID;
	}

	public GroupName getGroupName() {
		return groupName;
	}

	public GroupType getType() {
		return type;
	}

	public Optional<String> getDescription() {
		return description;
	}
	
	public Group toGroup(final UserName owner, final CreateAndModTimes times) {
		return Group.getBuilder(groupID, groupName, owner, times)
				.withType(type)
				.withDescription(description.orNull())
				.build();
	}

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("GroupCreationParams [groupID=");
		builder2.append(groupID);
		builder2.append(", groupName=");
		builder2.append(groupName);
		builder2.append(", type=");
		builder2.append(type);
		builder2.append(", description=");
		builder2.append(description);
		builder2.append("]");
		return builder2.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		GroupCreationParams other = (GroupCreationParams) obj;
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
		if (type != other.type) {
			return false;
		}
		return true;
	}

	// is there a way to make Group inherit from this class & builder to DRY things up?
	public static Builder getBuilder(
			final GroupID id,
			final GroupName name) {
		return new Builder(id, name);
	}
	
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private GroupType type = GroupType.organization;
		private Optional<String> description = Optional.absent();
		
		public Builder(final GroupID id, final GroupName name) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			this.groupID = id;
			this.groupName = name;
		}
		
		public Builder withType(final GroupType type) {
			checkNotNull(type, "type");
			this.type = type;
			return this;
		}
		
		// null or whitespace only == remove description
		public Builder withDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = Optional.absent();
			} else {
				this.description = Optional.of(description);
			}
			return this;
		}
		
		public GroupCreationParams build() {
			return new GroupCreationParams(groupID, groupName, type, description);
		}
	}
	
}
