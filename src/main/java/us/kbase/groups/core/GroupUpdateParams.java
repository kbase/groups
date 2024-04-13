package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

/** A set of parameters for updating a {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupUpdateParams {
	
	// is there a way to reduce the duplicate code w {@link GroupCreationParams}?
	// builder inheritance is kind of gross
	
	private final GroupID groupID;
	private final Optional<GroupName> groupName;
	private final OptionalGroupFields opfields;
	
	private GroupUpdateParams(
			final GroupID groupID,
			final Optional<GroupName> groupName,
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

	/** The new name of the group, or {@link Optional#empty()} if the name should not be changed.
	 * @return the name.
	 */
	public Optional<GroupName> getGroupName() {
		return groupName;
	}

	/** Get any optional fields associated with the group.
	 * @return the fields.
	 */
	public OptionalGroupFields getOptionalFields() {
		return opfields;
	}

	/** True if the name or type is present or if {@link OptionalGroupFields#hasUpdate()} is true.
	 * @return whether the update params contains update data.
	 */
	public boolean hasUpdate() {
		return groupName.isPresent() || opfields.hasUpdate();
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
		GroupUpdateParams other = (GroupUpdateParams) obj;
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

	/** Get a builder for a {@link GroupUpdateParams}.
	 * @param id the group ID.
	 * @return the new builder.
	 */
	public static Builder getBuilder(final GroupID id) {
		return new Builder(id);
	}
	
	/** A builder for a {@link GroupUpdateParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final GroupID groupID;
		private Optional<GroupName> groupName = Optional.empty();
		private OptionalGroupFields opfields = OptionalGroupFields.getBuilder().build();
		
		private Builder(final GroupID id) {
			checkNotNull(id, "id");
			this.groupID = id;
		}
		
		/** Change the name of the group.
		 * @param name the new name.
		 * @return this builder.
		 */
		public Builder withName(final GroupName name) {
			checkNotNull(name, "name");
			this.groupName = Optional.of(name);
			return this;
		}
		
		/** Change the name of the group.
		 * @param name the new name, or null to not change the name.
		 * @return this builder.
		 */
		public Builder withNullableName(final GroupName name) {
			this.groupName = Optional.ofNullable(name);
			return this;
		}
		
		/** Add optional fields to the update parameters.
		 * @param fields the optional fields.
		 * @return this builder.
		 */
		public Builder withOptionalFields(final OptionalGroupFields fields) {
			checkNotNull(fields, "fields");
			this.opfields = fields;
			return this;
		}
		
		/** Build the new {@link GroupUpdateParams}.
		 * @return the parameters.
		 */
		public GroupUpdateParams build() {
			return new GroupUpdateParams(groupID, groupName, opfields);
		}
	}
	
}
