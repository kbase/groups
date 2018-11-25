package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceType;

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
	private final Map<ResourceType, Set<ResourceDescriptor>> resources;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Optional<String> description;
	private final Map<NumberedCustomField, String> customFields;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final Set<UserName> members,
			final Set<UserName> admins,
			final GroupType type,
			final Map<ResourceType, Set<ResourceDescriptor>> resources,
			final CreateAndModTimes times,
			final Optional<String> description,
			final Map<NumberedCustomField, String> customFields) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.members = Collections.unmodifiableSet(members);
		this.admins = Collections.unmodifiableSet(admins);
		this.type = type;
		this.resources = Collections.unmodifiableMap(resources);
		this.creationDate = times.getCreationTime();
		this.modificationDate = times.getModificationTime();
		this.description = description;
		this.customFields = Collections.unmodifiableMap(customFields);
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
	
	/** Get the types of resources associated with this group.
	 * @return the types.
	 */
	public Set<ResourceType> getResourceTypes() {
		return resources.keySet();
	}
	
	/** Get the resources associated with the group for a given type.
	 * @param type the type of resources to get.
	 * @return the resources.
	 */
	public Set<ResourceDescriptor> getResources(final ResourceType type) {
		if (!resources.containsKey(type)) {
			throw new IllegalArgumentException("Type not found in group: " + type.getName());
		}
		return Collections.unmodifiableSet(resources.get(type));
	}
	
	/** Determine whether this group contains a resource.
	 * @param type the type of the resource.
	 * @param descriptor the resource descriptor.
	 * @return true if the group contains the resource, false otherwise.
	 */
	public boolean containsResource(final ResourceType type, final ResourceDescriptor descriptor) {
		return resources.containsKey(type) && resources.get(type).contains(descriptor);
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
	 * @return the description or {@link Optional#empty()}.
	 */
	public Optional<String> getDescription() {
		return description;
	}
	
	/** Get any custom fields associated with the group.
	 * @return the custom fields.
	 */
	public Map<NumberedCustomField, String> getCustomFields() {
		return customFields;
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
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((resources == null) ? 0 : resources.hashCode());
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
		if (customFields == null) {
			if (other.customFields != null) {
				return false;
			}
		} else if (!customFields.equals(other.customFields)) {
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
		if (resources == null) {
			if (other.resources != null) {
				return false;
			}
		} else if (!resources.equals(other.resources)) {
			return false;
		}
		if (type != other.type) {
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
		private final Map<ResourceType, Set<ResourceDescriptor>> resources = new HashMap<>();
		private Optional<String> description = Optional.empty();
		private final Map<NumberedCustomField, String> customFields = new HashMap<>();
		
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
		 * is set to {@link Optional#empty()}. The description will be {@link String#trim()}ed.
		 * @return this builder.
		 */
		public Builder withDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = Optional.empty();
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
		
		/** Add a resource to the group.
		 * @param type the type of the resource.
		 * @param descriptor the resource descriptor.
		 * @return this builder.
		 */
		public Builder withResource(final ResourceType type, final ResourceDescriptor descriptor) {
			checkNotNull(type, "type");
			checkNotNull(descriptor, "descriptor");
			if (!resources.containsKey(type)) {
				resources.put(type, new HashSet<>());
			}
			resources.get(type).add(descriptor);
			return this;
		}
		
		/** Add a custom field to the group.
		 * @param field the field name.
		 * @param value the field value.
		 * @return this builder.
		 */
		public Builder withCustomField(final NumberedCustomField field, final String value) {
			checkNotNull(field, "field");
			exceptOnEmpty(value, "value");
			// TODO CODE limit on value size?
			customFields.put(field, value);
			return this;
		}
		
		/** Build the {@link Group}.
		 * @return the new group.
		 */
		public Group build() {
			return new Group(groupID, groupName, owner, members, admins, type, resources,
					times, description, customFields);
		}
	}
	
}
