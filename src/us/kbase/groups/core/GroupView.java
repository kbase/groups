package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;

/** A view of a {@link Group}. A view consists of a subset of the full information in a
 * {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupView {
	
	public enum Role {
		/** Not a member of the group. */
		none,
		
		/** A member of the group. */
		member,
		
		/** An administrator of the group. */
		admin,
		
		/** The owner of the group. */
		owner;
	}
	
	//TODO CODE there could be a lot of optimizations to avoid fetching data that we just discard in the view.
	
	// group fields
	private final GroupID groupID; // all views
	private final Role role; // all views except private
	private final Optional<GroupName> groupName; // all views except private
	private final Optional<UserName> owner; // all views except private
	// contents depend on view
	private final Map<NumberedCustomField, String> customFields;
	private final Optional<Instant> creationDate; // all views except private
	private final Optional<Instant> modificationDate; // all views except private
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard except private
	private final Optional<Integer> memberCount; // all views except private
	private final Map<ResourceType, Integer> resourceCount; // empty for non-members
	
	// standard, but contents depend on view.
	// minimal - nothing
	// not member - owner & admins
	// standard - all
	private final Map<UserName, GroupUser> userInfo = new HashMap<>();

	// additional fields. standard - contents should change based on user
	private final Map<ResourceType, ResourceInformationSet> resourceInfo;
	
	// not part of the view, just describes the view
	private final boolean isStandardView;
	private final boolean isPrivate;
	
	// this class is starting to get a little hairy. Getting close to rethink/refactor time
	private GroupView(
			final Group group,
			final boolean standardView,
			final Role role,
			final Map<ResourceType, ResourceInformationSet> resourceInfo,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField,
			final Function<NumberedCustomField, Boolean> isUserPublicField) {
		this.isStandardView = standardView;
		this.role = role;
		this.isPrivate = group.isPrivate();
		this.groupID = group.getGroupID();
		if (isPrivateView()) {
			this.resourceInfo = Collections.emptyMap();
			this.groupName = Optional.empty();
			this.owner = Optional.empty();
			this.creationDate = Optional.empty();
			this.modificationDate = Optional.empty();
			this.members = Collections.emptySet();
			this.admins = Collections.emptySet();
			this.customFields = Collections.emptyMap();
			this.memberCount = Optional.empty();
			this.resourceCount = Collections.emptyMap();
		} else {
			this.memberCount = Optional.of(group.getAllMembers().size());
			this.resourceInfo = Collections.unmodifiableMap(resourceInfo);
			if (role.equals(Role.none)) {
				this.resourceCount = Collections.emptyMap();
			} else {
				// since the user is a member, we know the view isn't private
				this.resourceCount = Collections.unmodifiableMap(group.getResourceTypes().stream()
						.collect(Collectors.toMap(t -> t, t -> group.getResources(t).size())));
			}
			
			// group properties
			this.groupName = Optional.of(group.getGroupName());
			this.owner = Optional.of(group.getOwner());
			this.customFields = getCustomFields(
					group.getCustomFields(), isPublicField, isMinimalViewField);
			this.creationDate = Optional.of(group.getCreationDate());
			this.modificationDate = Optional.of(group.getModificationDate());
			final Function<NumberedCustomField, Boolean> upub = isUserPublicField;
			if (!standardView) {
				members = Collections.emptySet();
				admins = Collections.emptySet();
			} else {
				admins = group.getAdministrators();
				if (role.equals(Role.none)) {
					group.getAdministratorsAndOwner().stream().forEach(u -> userInfo.put(
							u, filterUserFields(group.getMember(u), upub)));
					members = Collections.emptySet();
				} else {
					members = group.getMembers();
					group.getAllMembers().stream().forEach(u -> userInfo.put(
							u, filterUserFields(group.getMember(u), upub)));
				}
			}
		}
	}
	
	// user fields are only visible in standard views.
	private GroupUser filterUserFields(
			final GroupUser member,
			final Function<NumberedCustomField, Boolean> isUserPublicField) {
		final GroupUser.Builder b = GroupUser.getBuilder(member.getName(), member.getJoinDate());
		getCustomFields(member.getCustomFields(), isUserPublicField, f -> false)
				.entrySet().stream().forEach(e -> b.withCustomField(e.getKey(), e.getValue()));
		return b.build();
	}

	private Map<NumberedCustomField, String> getCustomFields(
			final Map<NumberedCustomField, String> customFields,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField) {
		final Map<NumberedCustomField, String> ret = new HashMap<>();
		for (final NumberedCustomField f: customFields.keySet()) {
			final boolean isPublic = isPublicField.apply(f);
			final boolean isMinimal = isMinimalViewField.apply(f);
			if ((isPublic || !role.equals(Role.none)) && (isMinimal || isStandardView)) {
				ret.put(f, customFields.get(f));
			}
		}
		return Collections.unmodifiableMap(ret);
	}

	/** Get the type of the view.
	 * @return true if the view is a standard view, false if the view is minimal.
	 */
	public boolean isStandardView() {
		return isStandardView;
	}
	
	/** Get the user's role within the group.
	 * @return true if the user is a member.
	 */
	public Role getRole() {
		return role;
	}

	/** Get whether the group is private.
	 * @return true if the group is private.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}
	
	/** Get whether this is a private view of the group, where only the group ID is visible.
	 * The equivalent of {@link #isPrivate} && {@link #getRole()} equals
	 * {@link GroupView#Role#none}.
	 * @return true if this is a private view of the group.
	 */
	public boolean isPrivateView() {
		return isPrivate && role.equals(Role.none);
	}
	
	/** Get the group ID.
	 * @return the ID.
	 */
	public GroupID getGroupID() {
		return groupID;
	}

	/** Get the group name. Empty if the view is a private view.
	 * @return the name.
	 */
	public Optional<GroupName> getGroupName() {
		return groupName;
	}

	/** Get any custom fields associated with the group.
	 * @return the custom fields.
	 */
	public Map<NumberedCustomField, String> getCustomFields() {
		return customFields;
	}
	
	/** Get the owner of the group. Empty if the view is a private view.
	 * @return the owner.
	 */
	public Optional<UserName> getOwner() {
		return owner;
	}

	/** Get the members of the group. Empty for minimal and non-member views.
	 * @return the group members.
	 */
	public Set<UserName> getMembers() {
		return members;
	}
	
	/** Get the number of members in the group. Empty for private views.
	 * @return the number group members.
	 */
	public Optional<Integer> getMemberCount() {
		return memberCount;
	}

	/** Get the administrators of the group. Empty for minimal views.
	 * @return the group administrators.
	 */
	public Set<UserName> getAdministrators() {
		return admins;
	}
	
	/** Get the creation date of the group. Empty if the view is a private view.
	 * @return the creation date.
	 */
	public Optional<Instant> getCreationDate() {
		return creationDate;
	}

	/** Get the modification date of the group. Empty if the view is a private view.
	 * @return the modification date.
	 */
	public Optional<Instant> getModificationDate() {
		return modificationDate;
	}

	/** Get the types of the resources included in this view.
	 * @return the resource types.
	 */
	public Set<ResourceType> getResourceTypes() {
		return resourceInfo.keySet();
	}
	
	/** Get information about a particular resource type.
	 * @param type the resource type.
	 * @return information about the resources for that type included in this view.
	 */
	public ResourceInformationSet getResourceInformation(final ResourceType type) {
		if (!resourceInfo.containsKey(type)) {
			throw new IllegalArgumentException("No such resource type " + type.getName());
		}
		return resourceInfo.get(type);
	}
	
	/** Get the count of each resource type contained in the group. Empty for non-members.
	 * @return the resource counts per type.
	 */
	public Map<ResourceType, Integer> getResourceCounts() {
		return resourceCount;
	}
	
	/** Get a member's detailed information. Only available in a standard view. In a minimal
	 * view, this method will throw an illegal argument exception.
	 * @param user the member.
	 * @return the member's info.
	 */
	public GroupUser getMember(final UserName user) {
		if (!userInfo.containsKey(user)) {
			throw new IllegalArgumentException("No such member");
		}
		return userInfo.get(user);
	}

	// there's a lot of fields that are essentially redundant, but taking them out makes
	// EqualsVerifier complain and I can't be arsed to fix it. Besides, the performance hit
	// is likely negligible.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((admins == null) ? 0 : admins.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result + (isStandardView ? 1231 : 1237);
		result = prime * result + ((memberCount == null) ? 0 : memberCount.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((resourceCount == null) ? 0 : resourceCount.hashCode());
		result = prime * result + ((resourceInfo == null) ? 0 : resourceInfo.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((userInfo == null) ? 0 : userInfo.hashCode());
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
		GroupView other = (GroupView) obj;
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
		if (isStandardView != other.isStandardView) {
			return false;
		}
		if (memberCount == null) {
			if (other.memberCount != null) {
				return false;
			}
		} else if (!memberCount.equals(other.memberCount)) {
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
		if (resourceCount == null) {
			if (other.resourceCount != null) {
				return false;
			}
		} else if (!resourceCount.equals(other.resourceCount)) {
			return false;
		}
		if (resourceInfo == null) {
			if (other.resourceInfo != null) {
				return false;
			}
		} else if (!resourceInfo.equals(other.resourceInfo)) {
			return false;
		}
		if (role != other.role) {
			return false;
		}
		if (userInfo == null) {
			if (other.userInfo != null) {
				return false;
			}
		} else if (!userInfo.equals(other.userInfo)) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link GroupView}.
	 * @param group the group for the view.
	 * @param user the user for whom the view is being constructed. May be null. Any
	 * {@link ResourceInformationSet}s added to the builder via
	 * {@link Builder#withResource(ResourceType, ResourceInformationSet)} must have the same
	 * user.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final Group group, final UserName user) {
		return new Builder(group, user);
	}
	
	/** A builder for {@link GroupView}s.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final Group group;
		private final Optional<UserName> user;
		private boolean isStandardView = false;
		private final Map<ResourceType, ResourceInformationSet> resourceInfo = new HashMap<>();
		private Function<NumberedCustomField, Boolean> isPublicField = f -> false;
		private Function<NumberedCustomField, Boolean> isMinimalViewField = f -> false;
		private Function<NumberedCustomField, Boolean> isUserPublicField = f -> false;
		
		private Builder(final Group group, final UserName user) {
			this.group = requireNonNull(group, "group");
			this.user = Optional.ofNullable(user);
		}
		
		/** Set the type of the view - false for a minimal view (the default), true for a
		 * standard view.
		 * @param isStandardView the view type.
		 * @return this builder.
		 */
		public Builder withStandardView(final boolean isStandardView) {
			this.isStandardView = isStandardView;
			return this;
		}
		
		/** Add resource information to the view. The resource type and the resource IDs for
		 * that type must exist in the group, and the information set may not include
		 * nonexistent resources.
		 * Calling this method will overwrite any previous information for the type.
		 * @param type the type of the resource.
		 * @param info the information for the resource.
		 * @return this builder.
		 */
		public Builder withResource(final ResourceType type, final ResourceInformationSet info) {
			requireNonNull(type, "type");
			requireNonNull(info, "info");
			if (!info.getUser().equals(user)) {
				throw new IllegalArgumentException("User in info does not match user in builder");
			}
			if (!info.getNonexistentResources().isEmpty()) {
				throw new IllegalArgumentException(
						"Nonexistent resources are not allowed in the information set");
			}
			if (!group.getResourceTypes().contains(type)) {
				throw new IllegalArgumentException("Resource type does not exist in group: " +
						type.getName());
			}
			for (final ResourceID rid: info.getResources()) {
				if (!group.containsResource(type, rid)) {
					throw new IllegalArgumentException(String.format(
							"Resource %s of type %s does not exist in group",
							rid.getName(), type.getName()));
				}
			}
			
			resourceInfo.put(type, info);
			return this;
		}
		
		/** Add an empty {@link ResourceInformationSet} to the builder. This is useful to
		 * maintain a consistent set of types available in a view, regardless of the types
		 * available in the group.
		 * Calling this method will overwrite any previous information for the type.
		 * @param type the type of the resource.
		 * @return this builder.
		 */
		public Builder withResourceType(final ResourceType type) {
			requireNonNull(type, "type");
			resourceInfo.put(type, ResourceInformationSet.getBuilder(user.orElse(null)).build());
			return this;
		}
		
		/** Add a function that will be used to determine which fields are public fields and
		 * therefore viewable by all users, not just group members.
		 * By default, no fields are considered to be public fields.
		 * The function must not return null.
		 * @param isPublic a function that determines whether a custom field is public (true)
		 * or not (false).
		 * @return this builder.
		 */
		public Builder withPublicFieldDeterminer(
				final Function<NumberedCustomField, Boolean> isPublic) {
			this.isPublicField = requireNonNull(isPublic, "isPublic");
			return this;
		}
		
		/** Add a function that will be used to determine which fields are viewable in a
		 * non-standard (e.g. minimal) view.
		 * By default, no fields are viewable in a minimal view.
		 * The function must not return null.
		 * @param isMinimalView a function that determines whether a custom field is viewable
		 * in a minimal view (true) or not (false).
		 * @return this builder.
		 */
		public Builder withMinimalViewFieldDeterminer(
				final Function<NumberedCustomField, Boolean> isMinimalView) {
			this.isMinimalViewField = requireNonNull(isMinimalView, "isMinimalView");
			return this;
		}
		
		/** Add a function that will be used to determine which user fields are public fields and
		 * therefore viewable by all users, not just group members.
		 * By default, no fields are considered to be public fields.
		 * The function must not return null.
		 * @param isPublic a function that determines whether a custom user field is public (true)
		 * or not (false).
		 * @return this builder.
		 */
		public Builder withPublicUserFieldDeterminer(
				final Function<NumberedCustomField, Boolean> isPublic) {
			this.isUserPublicField = requireNonNull(isPublic, "isPublic");
			return this;
		}
		
		/** Build a new {@link GroupView}.
		 * @return the view.
		 */
		public GroupView build() {
			return new GroupView(group, isStandardView, getRole(group, user),
					resourceInfo, isPublicField, isMinimalViewField, isUserPublicField);
		}
		
		public Role getRole(final Group group, final Optional<UserName> user) {
			Role r = Role.none;
			if (user.isPresent()) {
				final UserName u = user.get();
				if (group.isMember(u)) {
					r = Role.member;
				}
				if (group.isAdministrator(u)) {
					r = Role.admin;
				}
				if (group.getOwner().equals(u)) {
					r = Role.owner;
				}
			}
			return r;
		}
		
	}
}
