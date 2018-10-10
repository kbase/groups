package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import com.google.common.base.Optional;

public class Group {
	
	// TODO JAVADOC
	// TODO TESTS
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final UserName owner;
	private final GroupType type;
	private final Optional<String> description;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final GroupType type,
			final Optional<String> description) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.type = type;
		this.description = description;
	}

	public GroupID getGroupID() {
		return groupID;
	}

	public GroupName getGroupName() {
		return groupName;
	}

	public UserName getOwner() {
		return owner;
	}

	public GroupType getType() {
		return type;
	}

	public Optional<String> getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		Group other = (Group) obj;
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
		return true;
	}

	public Builder getBuilder(final GroupID id, final GroupName name, final UserName owner) {
		return new Builder(id, name, owner);
	}
	
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private final UserName owner;
		private GroupType type = GroupType.organisation;
		private Optional<String> description = Optional.absent();
		
		public Builder(final GroupID id, final GroupName name, final UserName owner) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			checkNotNull(owner, "owner");
			this.groupID = id;
			this.groupName = name;
			this.owner = owner;
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
		
		public Group build() {
			return new Group(groupID, groupName, owner, type, description);
		}
	}
	
}
