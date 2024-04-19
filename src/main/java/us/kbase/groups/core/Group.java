package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

/** Represents a group consisting primarily of a set of users and set of resources associated
 * with those users.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class Group {
	
	private final GroupID groupID;
	private final GroupName groupName;
	private final UserName owner;
	private final boolean isPrivate;
	private final boolean privateMemberList;
	private final Map<UserName, GroupUser> allMembers;
	private final Set<UserName> admins;
	// maybe combine these into a single map with a class?
	private final Map<ResourceType, Map<ResourceID, ResourceAdministrativeID>> resources;
	private final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Map<NumberedCustomField, String> customFields;
	
	private Group(
			final GroupID groupID,
			final GroupName groupName,
			final UserName owner,
			final boolean isPrivate,
			final boolean privateMemberList,
			final Map<UserName, GroupUser> allMembers,
			final Set<UserName> admins,
			final Map<ResourceType, Map<ResourceID, ResourceAdministrativeID>> resources,
			final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate,
			final Instant creationDate,
			final Instant modificationDate,
			final Map<NumberedCustomField, String> customFields) {
		this.groupID = groupID;
		this.groupName = groupName;
		this.owner = owner;
		this.isPrivate = isPrivate;
		this.privateMemberList = privateMemberList;
		this.allMembers = Collections.unmodifiableMap(allMembers);
		this.admins = Collections.unmodifiableSet(admins);
		this.resources = Collections.unmodifiableMap(resources);
		this.resourceJoinDate = Collections.unmodifiableMap(resourceJoinDate);
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
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
	
	/** Get whether the group is private or not.
	 * @return true if the group is private.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}
	
	/** Get whether the member list is private or not. This does not affect the contents of
	 * this class, but must be respected in external views.
	 * @return true if the member list is private.
	 */
	public boolean isPrivateMemberList() {
		return privateMemberList;
	}
	
	/** Get the members of the group.
	 * This does not include the owner or administrators.
	 * @return the members.
	 */
	public Set<UserName> getMembers() {
		return Collections.unmodifiableSet(
				allMembers.keySet().stream().filter(u -> !owner.equals(u) && !admins.contains(u))
				.collect(Collectors.toSet()));
	}
	
	/** The role of a user within a group.
	 * @author gaprice@lbl.gov
	 *
	 */
	public enum Role {
		/** Not a member of the group. */
		NONE	("None"),
		
		/** A member of the group. */
		MEMBER	("Member"),
		
		/** An administrator of the group. */
		ADMIN	("Admin"),
		
		/** The owner of the group. */
		OWNER	("Owner");
		
		private static final Map<String, Role> REP_MAP = new HashMap<>();
		static {
			for (final Role r: Role.values()) {
				REP_MAP.put(r.getRepresentation(), r);
			}
		}
		
		private final String representation;
		
		private Role(final String representation) {
			this.representation = representation;
		}
		
		/** Get a representation of the enum that should be used for presentation purposes.
		 * @return the representation.
		 */
		public String getRepresentation() {
			return representation;
		}
		
		/** Get a role based on a supplied representation.
		 * @param representation the representation of the role as a string.
		 * @return a role type.
		 * @throws IllegalArgumentException if there is no role matching the representation.
		 */
		public static Role fromRepresentation(final String representation) {
			if (!REP_MAP.containsKey(representation)) {
				throw new IllegalArgumentException("Invalid role: " + representation);
			}
			return REP_MAP.get(representation);
		}
	}
	
	/** Get the role of a user within a group.
	 * @param userName the user.
	 * @return the users role.
	 */
	public Role getRole(final UserName userName) {
		if (!allMembers.containsKey(requireNonNull(userName, "userName"))) {
			return Role.NONE;
		}
		Role r = Role.MEMBER;
		if (admins.contains(userName)) {
			r = Role.ADMIN;
		}
		if (owner.equals(userName)) {
			r = Role.OWNER;
		}
		return r;
	}
	
	/** Get the administrators of the group. The owner is not included in this list, although
	 * the owner is included as an administrator in {@link #isAdministrator(UserName)}.
	 * @return the administrators.
	 */
	public Set<UserName> getAdministrators() {
		return admins;
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
		return Collections.unmodifiableSet(resources.get(type).entrySet().stream()
				.map(e -> new ResourceDescriptor(e.getValue(), e.getKey()))
				.collect(Collectors.toSet()));
	}
	
	/** Determine whether this group contains a resource.
	 * @param type the type of the resource.
	 * @param descriptor the resource descriptor.
	 * @return true if the group contains the resource, false otherwise.
	 */
	public boolean containsResource(final ResourceType type, final ResourceDescriptor descriptor) {
		requireNonNull(type, "type");
		requireNonNull(descriptor, "descriptor");
		return resources.containsKey(type) && descriptor.getAdministrativeID()
				.equals(resources.get(type).get(descriptor.getResourceID()));
	}
	
	/** Determine whether this group contains a resource.
	 * @param type the type of the resource.
	 * @param resourceID the resource ID.
	 * @return true if the group contains the resource, false otherwise.
	 */
	public boolean containsResource(final ResourceType type, final ResourceID resourceID) {
		requireNonNull(type, "type");
		requireNonNull(resourceID, "resourceID");
		return resources.containsKey(type) && resources.get(type).containsKey(resourceID);
	}
	
	/** Get a resource contained in the group.
	 * @param type the type of the resource.
	 * @param resourceID the resource ID.
	 * @return the resource.
	 */
	public ResourceDescriptor getResource(final ResourceType type, final ResourceID resourceID) {
		getResourceCheckArgs(type, resourceID);
		return new ResourceDescriptor(resources.get(type).get(resourceID), resourceID);
	}
	
	/** Get the date a resource was added to the group. May be {@link Optional#empty()} for
	 * resources added before version 0.1.3 of the software.
	 * @param type the type of the resource.
	 * @param resourceID the ID of the resource.
	 * @return the date the resource was added to the group.
	 */
	public Optional<Instant> getResourceAddDate(
			final ResourceType type,
			final ResourceID resourceID) {
		getResourceCheckArgs(type, resourceID);
		return resourceJoinDate.get(type).get(resourceID);
	}

	private void getResourceCheckArgs(final ResourceType type, final ResourceID resourceID) {
		requireNonNull(type, "type");
		requireNonNull(resourceID, "resourceID");
		if (!containsResource(type, resourceID)) {
			throw new IllegalArgumentException(String.format("No such resource %s %s",
					type.getName(), resourceID.getName()));
		}
	}
	
	/** Get a copy of the group without the specified resources.
	 * @param type the type of resources to remove.
	 * @param resources the resources to remove.
	 * @return a copy of the group less the resources.
	 */
	public Group removeResources(final ResourceType type, final Set<ResourceID> resources) {
		requireNonNull(type, "type");
		checkNoNullsInCollection(resources, "resources");
		if (!this.resources.containsKey(type)) {
			throw new IllegalArgumentException(String.format("No such resource type %s",
					type.getName()));
		}
		final Map<ResourceType, Map<ResourceID, ResourceAdministrativeID>> res = this.resources
				.entrySet().stream().collect(Collectors.toMap(
						e -> e.getKey(), e -> new HashMap<>(e.getValue())));
		final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resJoin = this.resourceJoinDate
				.entrySet().stream().collect(Collectors.toMap(
						e -> e.getKey(), e -> new HashMap<>(e.getValue())));
		for (final ResourceID r: resources) {
			if (res.get(type).remove(r) == null) {
				throw new IllegalArgumentException(String.format("No such resource %s %s",
						type.getName(), r.getName()));
			}
			resJoin.get(type).remove(r);
		}
		if (res.get(type).isEmpty()) {
			res.remove(type);
		}
		if (resJoin.get(type).isEmpty()) {
			resJoin.remove(type);
		}
		return new Group(groupID, groupName, owner, isPrivate, privateMemberList, allMembers,
				admins, res, resJoin, creationDate, modificationDate, customFields);
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

	/** Get any custom fields associated with the group.
	 * @return the custom fields.
	 */
	public Map<NumberedCustomField, String> getCustomFields() {
		return customFields;
	}
	
	/** Check if a user is a group administrator, including the owner.
	 * @param user the user to check.
	 * @return true if the user is a group administrator, false otherwise.
	 */
	public boolean isAdministrator(final UserName user) {
		requireNonNull(user, "user");
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
		return allMembers.containsKey(user);
	}
	
	/** Get a member's detailed information.
	 * @param user the member.
	 * @return the member's info.
	 */
	public GroupUser getMember(final UserName user) {
		if (!allMembers.containsKey(user)) {
			throw new IllegalArgumentException("No such member");
		}
		return allMembers.get(user);
	}
	
	/** Get all the members, including the administrators and owner, of the group.
	 * @return the group members.
	 */
	public Set<UserName> getAllMembers() {
		return allMembers.keySet();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((admins == null) ? 0 : admins.hashCode());
		result = prime * result + ((allMembers == null) ? 0 : allMembers.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + (privateMemberList ? 1231 : 1237);
		result = prime * result + ((resourceJoinDate == null) ? 0 : resourceJoinDate.hashCode());
		result = prime * result + ((resources == null) ? 0 : resources.hashCode());
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
		if (allMembers == null) {
			if (other.allMembers != null) {
				return false;
			}
		} else if (!allMembers.equals(other.allMembers)) {
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
		if (isPrivate != other.isPrivate) {
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
		if (privateMemberList != other.privateMemberList) {
			return false;
		}
		if (resourceJoinDate == null) {
			if (other.resourceJoinDate != null) {
				return false;
			}
		} else if (!resourceJoinDate.equals(other.resourceJoinDate)) {
			return false;
		}
		if (resources == null) {
			if (other.resources != null) {
				return false;
			}
		} else if (!resources.equals(other.resources)) {
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
			final GroupUser owner,
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
		private boolean isPrivate = false;
		private boolean privateMemberList = true;
		private final CreateAndModTimes times;
		private final Map<UserName, GroupUser> allMembers = new HashMap<>();
		private final Set<UserName> admins = new HashSet<>();
		private final Map<ResourceType, Map<ResourceID, ResourceAdministrativeID>> resources =
				new HashMap<>();
		private final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate =
				new HashMap<>();
		private final Map<NumberedCustomField, String> customFields = new HashMap<>();
		
		private Builder(
				final GroupID id,
				final GroupName name,
				final GroupUser owner,
				final CreateAndModTimes times) {
			requireNonNull(owner, "owner");
			this.groupID = requireNonNull(id, "id");
			this.groupName = requireNonNull(name, "name");
			this.owner = owner.getName();
			this.allMembers.put(owner.getName(), owner);
			this.times = requireNonNull(times, "times");
		}
		
		/** Add a member to the builder.
		 * @param member the member.
		 * @return this builder.
		 * @throws IllegalArgumentException if the member is already the owner of the group
		 * or an administrator.
		 */
		public Builder withMember(final GroupUser member) {
			requireNonNull(member, "member");
			addMember(member);
			return this;
		}

		private void addMember(final GroupUser member) {
			if (allMembers.containsKey(member.getName())) {
				throw new IllegalArgumentException("Group already contains member " +
						member.getName().getName());
			}
			this.allMembers.put(member.getName(), member);
		}
		
		/** Add an admin to the builder.
		 * @param admin the administrator.
		 * @return this builder.
		 * @throws IllegalArgumentException if the admin is already the owner of the group
		 * or an member.
		 */
		public Builder withAdministrator(final GroupUser admin) {
			requireNonNull(admin, "admin");
			addMember(admin);
			this.admins.add(admin.getName());
			return this;
		}
		
		/** Add a resource to the group. The equivalent of
		 * {@link #withResource(ResourceType, ResourceDescriptor, Instant)} with a null date
		 * argument.
		 * Adding a duplicate resource ID with a different administrative ID will overwrite the
		 * previous resource.
		 * @param type the type of the resource.
		 * @param descriptor the resource descriptor.
		 * @return this builder.
		 */
		public Builder withResource(
				final ResourceType type,
				final ResourceDescriptor descriptor) {
			return withResource(type, descriptor, null);
		}
		
		/** Add a resource to the group.
		 * Adding a duplicate resource ID with a different administrative ID will overwrite the
		 * previous resource.
		 * @param type the type of the resource.
		 * @param descriptor the resource descriptor.
		 * @param added the date the resource was added to the group. Null is allowable if
		 * the date is not known.
		 * @return this builder.
		 */
		public Builder withResource(
				final ResourceType type,
				final ResourceDescriptor descriptor,
				final Instant added) {
			requireNonNull(type, "type");
			requireNonNull(descriptor, "descriptor");
			if (!resources.containsKey(type)) {
				resources.put(type, new HashMap<>());
			}
			if (!resourceJoinDate.containsKey(type)) {
				resourceJoinDate.put(type, new HashMap<>());
			}
			resources.get(type).put(descriptor.getResourceID(), descriptor.getAdministrativeID());
			resourceJoinDate.get(type).put(descriptor.getResourceID(), Optional.ofNullable(added));
			return this;
		}
		
		/** Add a custom field to the group.
		 * @param field the field name.
		 * @param value the field value.
		 * @return this builder.
		 */
		public Builder withCustomField(final NumberedCustomField field, final String value) {
			requireNonNull(field, "field");
			exceptOnEmpty(value, "value");
			// TODO CODE limit on value size?
			customFields.put(field, value);
			return this;
		}
		
		/** Set the group to public or private.
		 * @param isPrivate true to set the group to private, false for public.
		 * @return this builder.
		 */
		public Builder withIsPrivate(final boolean isPrivate) {
			this.isPrivate = isPrivate;
			return this;
		}
		
		/** Set the member list to private or public. This has no effect on the contents of
		 * this class, but must be respected in external views.
		 * @param privateMembers true to make the members list private, false for public.
		 * @return this builder.
		 */
		public Builder withPrivateMemberList(final boolean privateMembers) {
			this.privateMemberList = privateMembers;
			return this;
		}
		
		/** Build the {@link Group}.
		 * @return the new group.
		 */
		public Group build() {
			return new Group(groupID, groupName, owner, isPrivate, privateMemberList, allMembers,
					admins, resources, resourceJoinDate,
					times.getCreationTime(), times.getModificationTime(),
					customFields);
		}
	}
	
}
