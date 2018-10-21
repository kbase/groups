package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import com.google.common.base.Optional;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A set of parameters for creating a {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupCreationParams {
	
	// is there a way to reduce the duplicate code w {@link Group}?
	
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

	/** Get the type of the group.
	 * @return the group type.
	 */
	public GroupType getType() {
		return type;
	}

	/** Get the description of the group, if any.
	 * @return the description or {@link Optional#absent()}.
	 */
	public Optional<String> getDescription() {
		return description;
	}
	
	/** Create a {@link Group} from these parameters.
	 * @param owner the owner of the group.
	 * @param times the creation and modification times of the group.
	 * @return the new group.
	 */
	public Group toGroup(final UserName owner, final CreateAndModTimes times) {
		return Group.getBuilder(groupID, groupName, owner, times)
				.withType(type)
				.withDescription(description.orNull())
				.build();
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
	/** Get a builder for a {@link GroupCreationParams}.
	 * @param id the group ID.
	 * @param name the group name.
	 * @return the new builder.
	 */
	public static Builder getBuilder(
			final GroupID id,
			final GroupName name) {
		return new Builder(id, name);
	}
	
	/** A builder for a {@link GroupCreationParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private GroupType type = GroupType.ORGANIZATION;
		private Optional<String> description = Optional.absent();
		
		private Builder(final GroupID id, final GroupName name) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			this.groupID = id;
			this.groupName = name;
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
		 * @throws IllegalParameterException if the description is too long.
		 */
		public Builder withDescription(final String description) throws IllegalParameterException {
			if (isNullOrEmpty(description)) {
				this.description = Optional.absent();
			} else {
				try {
					checkString(description, "description", Group.MAX_DESCRIPTION_CODE_POINTS);
				} catch (MissingParameterException e) {
					throw new RuntimeException("This should be impossible");
				}
				this.description = Optional.of(description.trim());
			}
			return this;
		}
		
		/** Build the new {@link GroupCreationParams}.
		 * @return the parameters.
		 */
		public GroupCreationParams build() {
			return new GroupCreationParams(groupID, groupName, type, description);
		}
	}
	
}
