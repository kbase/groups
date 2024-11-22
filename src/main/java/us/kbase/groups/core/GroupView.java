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

import us.kbase.groups.core.Group.Role;
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
	
	// seems stupid to have this so similar to GroupUser...
	/** A view of a member of a group.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class GroupUserView {
		
		private final UserName name;
		private final Optional<Instant> joinDate;
		private final Optional<Instant> lastVisit;
		private final Map<NumberedCustomField, String> customFields;
		
		private GroupUserView(
				final UserName name,
				final Instant joinDate,
				final Instant lastVisit,
				final Map<NumberedCustomField, String> customFields) {
			this.name = name;
			this.joinDate = Optional.ofNullable(joinDate);
			this.lastVisit = Optional.ofNullable(lastVisit);
			this.customFields = Collections.unmodifiableMap(customFields);
		}

		/** The user's name.
		 * @return the user's name.
		 */
		public UserName getName() {
			return name;
		}

		/** The date the user joined the group, if visible to the querying user.
		 * Only members can see other members' join dates.
		 * @return the join date.
		 */
		public Optional<Instant> getJoinDate() {
			return joinDate;
		}
		
		/** Get the date the user last visited the group, or {@link Optional#empty()} if the user
		 * has never visited the group or the querying user may not see the date.
		 * @return the date the user last visited the group.
		 */
		public Optional<Instant> getLastVisit() {
			return lastVisit;
		}

		/** Get any visible custom fields associated with the user.
		 * @return the custom fields.
		 */
		public Map<NumberedCustomField, String> getCustomFields() {
			return customFields;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
			result = prime * result + ((joinDate == null) ? 0 : joinDate.hashCode());
			result = prime * result + ((lastVisit == null) ? 0 : lastVisit.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			GroupUserView other = (GroupUserView) obj;
			if (customFields == null) {
				if (other.customFields != null) {
					return false;
				}
			} else if (!customFields.equals(other.customFields)) {
				return false;
			}
			if (joinDate == null) {
				if (other.joinDate != null) {
					return false;
				}
			} else if (!joinDate.equals(other.joinDate)) {
				return false;
			}
			if (lastVisit == null) {
				if (other.lastVisit != null) {
					return false;
				}
			} else if (!lastVisit.equals(other.lastVisit)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
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
	private final Optional<Instant> lastVisit; // all views except private
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard except private
	private final Optional<Integer> memberCount; // all views except private
	private final Map<ResourceType, Integer> resourceCount; // all views except private
	
	// standard, but contents depend on view.
	// minimal - nothing
	// not member - owner & admins
	// standard - all
	private final Map<UserName, GroupUserView> userInfo = new HashMap<>();

	// additional fields. standard - contents should change based on user
	// also, it might make sense to make another class that combines these two info sources
	// Since it's hidden behind the JSON api, don't worry about it for now. Refactor later
	// This class is going to need a pretty serious refactor anyway
	private final Map<ResourceType, ResourceInformationSet> resourceInfo;
	private final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate;
	
	// not part of the view, just describes the view
	private final boolean isStandardView;
	private final boolean isPrivate;
	private final boolean isOverridePrivateView;
	private final Optional<Boolean> isPrivateMemberList;
	
	// this class is starting to get a little hairy. Getting close to rethink/refactor time
	private GroupView(
			final Group group,
			final boolean standardView,
			final boolean isOverridePrivateView,
			final Optional<UserName> user,
			final Map<ResourceType, ResourceInformationSet> resourceInfo,
			final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField,
			final Function<NumberedCustomField, Boolean> isUserPublicField) {
		this.isStandardView = standardView;
		this.isOverridePrivateView = isOverridePrivateView;
		this.role = user.map(u -> group.getRole(u)).orElse(Role.NONE);
		this.isPrivate = group.isPrivate();
		this.groupID = group.getGroupID();
		if (isPrivateView()) {
			// resInfo is expected to only contain admin'd resources for non-members
			this.resourceInfo = Collections.unmodifiableMap(resourceInfo);
			this.resourceJoinDate = Collections.emptyMap();
			this.groupName = Optional.empty();
			this.owner = Optional.empty();
			this.creationDate = Optional.empty();
			this.modificationDate = Optional.empty();
			this.members = Collections.emptySet();
			this.admins = Collections.emptySet();
			this.customFields = Collections.emptyMap();
			this.memberCount = Optional.empty();
			this.resourceCount = Collections.emptyMap();
			this.isPrivateMemberList = Optional.empty();
			this.lastVisit = Optional.empty();
		} else {
			this.memberCount = Optional.of(group.getAllMembers().size());
			this.resourceCount = Collections.unmodifiableMap(group.getResourceTypes().stream()
					.collect(Collectors.toMap(t -> t, t -> group.getResources(t).size())));
			this.resourceInfo = Collections.unmodifiableMap(resourceInfo);
			if (role.equals(Role.NONE)) {
				this.resourceJoinDate = Collections.emptyMap();
				this.lastVisit = Optional.empty();
			} else {
				// since the user is a member, we know the view isn't private
				this.resourceJoinDate = Collections.unmodifiableMap(resourceJoinDate);
				this.lastVisit = group.getMember(user.get()).getLastVisit();
			}
			
			// group properties
			this.groupName = Optional.of(group.getGroupName());
			this.owner = Optional.of(group.getOwner());
			this.customFields = getCustomFields(
					group.getCustomFields(), isPublicField, isMinimalViewField);
			this.creationDate = Optional.of(group.getCreationDate());
			this.modificationDate = Optional.of(group.getModificationDate());
			if (!standardView) {
				isPrivateMemberList = Optional.empty();
				members = Collections.emptySet();
				admins = Collections.emptySet();
			} else {
				final Function<NumberedCustomField, Boolean> upub = isUserPublicField;
				isPrivateMemberList = Optional.of(group.isPrivateMemberList());
				admins = group.getAdministrators();
				if (role.equals(Role.NONE) && group.isPrivateMemberList()) {
					group.getAdministratorsAndOwner().stream().forEach(u -> userInfo.put(
							u, filterUserFields(group.getMember(u), upub, group, user)));
					members = Collections.emptySet();
				} else {
					members = group.getMembers();
					group.getAllMembers().stream().forEach(u -> userInfo.put(
							u, filterUserFields(group.getMember(u), upub, group, user)));
				}
			}
		}
	}
	
	// user fields are only visible in standard views.
	private GroupUserView filterUserFields(
			final GroupUser member,
			final Function<NumberedCustomField, Boolean> isUserPublicField,
			final Group g,
			final Optional<UserName> user) {
		final Map<NumberedCustomField, String> fields =
				getCustomFields(member.getCustomFields(), isUserPublicField, f -> false);
		if (role.equals(Role.NONE)) {
			return new GroupUserView(member.getName(), null, null, fields);
		} else if (g.isAdministrator(user.get())) {
			return new GroupUserView(member.getName(), member.getJoinDate(),
					member.getLastVisit().orElse(null), fields);
		} else {
			return new GroupUserView(member.getName(), member.getJoinDate(), null, fields);
		}
	}

	private Map<NumberedCustomField, String> getCustomFields(
			final Map<NumberedCustomField, String> customFields,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField) {
		final Map<NumberedCustomField, String> ret = new HashMap<>();
		for (final NumberedCustomField f: customFields.keySet()) {
			final boolean isPublic = isPublicField.apply(f);
			final boolean isMinimal = isMinimalViewField.apply(f);
			if ((isPublic || !role.equals(Role.NONE)) && (isMinimal || isStandardView)) {
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
	
	/** Get whether the members list is private. Only available in the standard view.
	 * @return true if the members list is private.
	 */
	public Optional<Boolean> isPrivateMembersList() {
		return isPrivateMemberList;
	}
	
	/** Get whether the group's privacy setting has been overridden in this view, allowing
	 * for a public view. The user's role within the group is still taken into account.
	 * @return whether the group's privacy setting has been overridden.
	 */
	public boolean isOverridePrivateView() {
		return isOverridePrivateView;
	}
	
	/** Get whether this is a private view of the group, where only the group ID is visible.
	 * The equivalent of {@link #isPrivate()} && !{@link #isOverridePrivateView()} &&
	 * {@link #getRole()} equals {@link Role#NONE}.
	 * @return true if this is a private view of the group.
	 */
	public boolean isPrivateView() {
		return isPrivate && !isOverridePrivateView && role.equals(Role.NONE);
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

	/** Get the members of the group. Empty for minimal views and non-member views where the member
	 * list is private.
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
	
	/** Get the date of the last visit to the group of the user that was passed in to
	 * {@link #getBuilder(Group, UserName)}.
	 * @return the last visit date.
	 */
	public Optional<Instant> getLastVisit() {
		return lastVisit;
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
	
	/** Get the date a resource was added to the group. May be {@link Optional#empty()} for
	 * resources added before version 0.1.3 of the software. Calling this method for views of
	 * a group where the user is not a member will throw an error.
	 * @param type the type of the resource.
	 * @param resourceID the ID of the resource.
	 * @return the date the resource was added to the group.
	 */
	public Optional<Instant> getResourceAddDate(
			final ResourceType type,
			final ResourceID resourceID) {
		requireNonNull(type, "type");
		requireNonNull(resourceID, "resourceID");
		// could check group membership here and throw a nicer error but yagni for now.
		if (!resourceJoinDate.containsKey(type) ||
				!resourceJoinDate.get(type).containsKey(resourceID)) {
			throw new IllegalArgumentException(String.format("No such resource %s %s",
					type.getName(), resourceID.getName()));
		}
		return resourceJoinDate.get(type).get(resourceID);
	}

	/** Get the count of each resource type contained in the group. Empty for non-members.
	 * @return the resource counts per type.
	 */
	public Map<ResourceType, Integer> getResourceCounts() {
		return resourceCount;
	}
	
	/** Get a member's detailed information. Only available in a standard view. In a minimal
	 * view, this method will throw an illegal argument exception.
	 * {@link GroupUser#getLastVisit()} will return {@link Optional#empty()} if the user
	 * passed into {@link #getBuilder(Group, UserName)} is not a group administrator.
	 * @param user the member.
	 * @return the member's info.
	 */
	public GroupUserView getMember(final UserName user) {
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
		result = prime * result + (isOverridePrivateView ? 1231 : 1237);
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result + ((isPrivateMemberList == null) ? 0 : isPrivateMemberList.hashCode());
		result = prime * result + (isStandardView ? 1231 : 1237);
		result = prime * result + ((lastVisit == null) ? 0 : lastVisit.hashCode());
		result = prime * result + ((memberCount == null) ? 0 : memberCount.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((resourceCount == null) ? 0 : resourceCount.hashCode());
		result = prime * result + ((resourceInfo == null) ? 0 : resourceInfo.hashCode());
		result = prime * result + ((resourceJoinDate == null) ? 0 : resourceJoinDate.hashCode());
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
		if (isOverridePrivateView != other.isOverridePrivateView) {
			return false;
		}
		if (isPrivate != other.isPrivate) {
			return false;
		}
		if (isPrivateMemberList == null) {
			if (other.isPrivateMemberList != null) {
				return false;
			}
		} else if (!isPrivateMemberList.equals(other.isPrivateMemberList)) {
			return false;
		}
		if (isStandardView != other.isStandardView) {
			return false;
		}
		if (lastVisit == null) {
			if (other.lastVisit != null) {
				return false;
			}
		} else if (!lastVisit.equals(other.lastVisit)) {
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
		if (resourceJoinDate == null) {
			if (other.resourceJoinDate != null) {
				return false;
			}
		} else if (!resourceJoinDate.equals(other.resourceJoinDate)) {
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
		private boolean isOverridePrivateView = false;
		private final Map<ResourceType, ResourceInformationSet> resourceInfo = new HashMap<>();
		private final Map<ResourceType, Map<ResourceID, Optional<Instant>>> resourceJoinDate =
				new HashMap<>();
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
		 * The resource information will be visible to any user able to access this object.
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
			resourceJoinDate.put(type, new HashMap<>());
			info.getResources().stream().forEach(
					r -> resourceJoinDate.get(type).put(r, group.getResourceAddDate(type, r)));
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
			resourceJoinDate.put(type, new HashMap<>());
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
		
		/** Override the private property of a group and allow a public view of the group.
		 * The user's role within the group is still taken into account when determining the view.
		 * @param overridePrivateView true to override the group's private property.
		 * @return this builder.
		 */
		public Builder withOverridePrivateView(final boolean overridePrivateView) {
			this.isOverridePrivateView = overridePrivateView;
			return this;
		}
		
		/** Build a new {@link GroupView}.
		 * @return the view.
		 */
		public GroupView build() {
			return new GroupView(group, isStandardView, isOverridePrivateView, user,
					resourceInfo, resourceJoinDate,
					isPublicField, isMinimalViewField, isUserPublicField);
		}
	}
}
