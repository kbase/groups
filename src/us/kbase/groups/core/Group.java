package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;

import com.google.common.base.Optional;

public class Group {
	
	// TODO JAVADOC
	// TODO TESTS
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final UserName owner;
	private final GroupType type;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Optional<String> description;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final GroupType type,
			final Instant creationDate,
			final Instant modificationDate,
			final Optional<String> description) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.type = type;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
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

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getModificationDate() {
		return modificationDate;
	}

	public Optional<String> getDescription() {
		return description;
	}
	
	

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("Group [groupID=");
		builder2.append(groupID);
		builder2.append(", groupName=");
		builder2.append(groupName);
		builder2.append(", owner=");
		builder2.append(owner);
		builder2.append(", type=");
		builder2.append(type);
		builder2.append(", creationDate=");
		builder2.append(creationDate);
		builder2.append(", modificationDate=");
		builder2.append(modificationDate);
		builder2.append(", description=");
		builder2.append(description);
		builder2.append("]");
		return builder2.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
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
		return true;
	}

	public static TimesStep getBuilder(
			final GroupID id,
			final GroupName name,
			final UserName owner) {
		return new Builder(id, name, owner);
	}
	
	public interface TimesStep {
		
		OptionalsStep withTimes(Instant createdDate, Instant modifiedDate);
	}
	
	public interface OptionalsStep {
		
		OptionalsStep withType(final GroupType type);
		
		OptionalsStep withDescription(final String description);
		
		Group build();
	}
	
	public static class Builder implements TimesStep, OptionalsStep{
		
		private final GroupID groupID;
		private final GroupName groupName;
		private final UserName owner;
		private GroupType type = GroupType.organisation;
		private Instant creationDate;
		private Instant modificationDate;
		private Optional<String> description = Optional.absent();
		
		public Builder(final GroupID id, final GroupName name, final UserName owner) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			checkNotNull(owner, "owner");
			this.groupID = id;
			this.groupName = name;
			this.owner = owner;
		}
		
		public OptionalsStep withTimes(
				final Instant creationDate,
				final Instant modificationDate) {
			checkNotNull(creationDate, "creationDate");
			checkNotNull(modificationDate, "modificationDate");
			if (modificationDate.isBefore(creationDate)) {
				throw new IllegalArgumentException(
						"modification date must be after creation date");
			}
			this.creationDate = creationDate;
			this.modificationDate = modificationDate;
			return this;
		}
		
		public OptionalsStep withType(final GroupType type) {
			checkNotNull(type, "type");
			this.type = type;
			return this;
		}
		
		// null or whitespace only == remove description
		public OptionalsStep withDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = Optional.absent();
			} else {
				this.description = Optional.of(description);
			}
			return this;
		}
		
		public Group build() {
			if (creationDate == null || modificationDate == null) {
				throw new NullPointerException("Don't skip the withTimes step");
			}
			return new Group(groupID, groupName, owner, type, creationDate, modificationDate,
					description);
		}
	}
	
}
