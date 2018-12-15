package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;

/** A view of a {@link Group}. A view consists of a subset of the full information in a
 * {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupView {
	
	//TODO CODE there could be a lot of optimizations to avoid fetching data that we just discard in the view.
	
	// group fields
	private final GroupID groupID; // all views
	private final Optional<GroupName> groupName; // all views except private
	private final Optional<UserName> owner; // all views except private
	// contents depend on view
	private final Map<NumberedCustomField, String> customFields;
	private final Optional<Instant> creationDate; // all views except private
	private final Optional<Instant> modificationDate; // all views except private
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard except private
	
	// standard, but contents depend on view.
	// private - nothing
	// minimal - owner only
	// not member - owner & admins
	// standard - all
	private final Map<UserName, GroupUser> userInfo = new HashMap<>();

	// additional fields. standard - contents should change based on user
	private final Map<ResourceType, ResourceInformationSet> resourceInfo;
	
	// not part of the view, just describes the view
	private final boolean isStandardView;
	private final boolean isMember;
	private final boolean isPrivate;
	
	private GroupView(
			final Group group,
			final boolean standardView,
			final boolean isMember,
			final Map<ResourceType, ResourceInformationSet> resourceInfo,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField,
			final Function<NumberedCustomField, Boolean> isUserPublicField) {
		this.isStandardView = standardView;
		this.isMember = isMember;
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
		} else {
			this.resourceInfo = Collections.unmodifiableMap(resourceInfo);
			
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
				if (!isMember) {
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
			if ((isPublic || isMember) && (isMinimal || isStandardView)) {
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
	
	/** Get whether the user is a member of the group.
	 * @return true if the user is a member.
	 */
	public boolean isMember() {
		return isMember;
	}

	/** Get whether the group is private.
	 * @return true if the group is private.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}
	
	/** Get whether this is a private view of the group, where only the group ID is visible.
	 * The equivalent of {@link #isPrivate} && !{@link #isMember()}.
	 * @return true if this is a private view of the group.
	 */
	public boolean isPrivateView() {
		return isPrivate && !isMember;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((admins == null) ? 0 : admins.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isMember ? 1231 : 1237);
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result + (isStandardView ? 1231 : 1237);
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((resourceInfo == null) ? 0 : resourceInfo.hashCode());
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
		if (isMember != other.isMember) {
			return false;
		}
		if (isPrivate != other.isPrivate) {
			return false;
		}
		if (isStandardView != other.isStandardView) {
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
		if (resourceInfo == null) {
			if (other.resourceInfo != null) {
				return false;
			}
		} else if (!resourceInfo.equals(other.resourceInfo)) {
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
		
		/** Add resource information to the view.
		 * Calling this method will overwrite any previous information for the type.
		 * Any nonexistent resources in the information set will be discarded.
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
			resourceInfo.put(type, info.withoutNonexistentResources());
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
			return new GroupView(group, isStandardView, group.isMember(user.orElse(null)),
					resourceInfo, isPublicField, isMinimalViewField, isUserPublicField);
		}
		
	}
}
