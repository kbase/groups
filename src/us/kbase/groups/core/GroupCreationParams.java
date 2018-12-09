package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

/** A set of parameters for creating a {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupCreationParams {
	
	// is there a way to reduce the duplicate code w {@link Group}? Tried this in auth
	// with the admin configuration and it's kind of gross
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final OptionalGroupFields opfields;
	
	private GroupCreationParams(
			final GroupID groupID,
			final GroupName groupName,
			final OptionalGroupFields opfields) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.opfields = opfields;
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

	/** Get any optional fields associated with the group.
	 * @return the fields.
	 */
	public OptionalGroupFields getOptionalFields() {
		return opfields;
	}

	/** Create a {@link Group} from these parameters.
	 * @param owner the owner of the group.
	 * @param times the creation and modification times of the group.
	 * @return the new group.
	 */
	public Group toGroup(final UserName owner, final CreateAndModTimes times) {
		final us.kbase.groups.core.Group.Builder b = Group.getBuilder(
				groupID, groupName, GroupUser.getBuilder(owner, times.getCreationTime()).build(),
				times);
		opfields.getCustomFields().stream().filter(f -> opfields.getCustomValue(f).isPresent())
				.forEach(f -> b.withCustomField(f, opfields.getCustomValue(f).get()));
		return b.build();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((opfields == null) ? 0 : opfields.hashCode());
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
		if (opfields == null) {
			if (other.opfields != null) {
				return false;
			}
		} else if (!opfields.equals(other.opfields)) {
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
	public static Builder getBuilder(final GroupID id, final GroupName name) {
		return new Builder(id, name);
	}
	
	/** A builder for a {@link GroupCreationParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final GroupID groupID;
		private final GroupName groupName;
		private OptionalGroupFields opfields = OptionalGroupFields.getBuilder().build();
		
		private Builder(final GroupID id, final GroupName name) {
			checkNotNull(id, "id");
			checkNotNull(name, "name");
			this.groupID = id;
			this.groupName = name;
		}
		
		/** Add optional fields to the creation parameters. In the context of the creation
		 * parameters, {@link FieldItem#isRemove()} is treated as {@link FieldItem#isNoAction()}.
		 * The default set of optional fields are as {@link OptionalGroupFields#getDefault()}.
		 * @param fields the optional fields.
		 * @return this builder.
		 */
		public Builder withOptionalFields(final OptionalGroupFields fields) {
			checkNotNull(fields, "fields");
			this.opfields = fields;
			return this;
		}
		
		/** Build the new {@link GroupCreationParams}.
		 * @return the parameters.
		 */
		public GroupCreationParams build() {
			return new GroupCreationParams(groupID, groupName, opfields);
		}
	}
	
}
