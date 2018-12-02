package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

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

	// group fields
	private final GroupID groupID; // all views
	private final GroupName groupName; // all views
	private final UserName owner; // all views
	private final Map<NumberedCustomField, String> customFields; // all views
	private final Instant creationDate; // all views
	private final Instant modificationDate; // all views
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard
	private final Optional<String> description; // standard

	// additional fields. standard - contents should change based on user
	private final Map<ResourceType, ResourceInformationSet> resourceInfo;
	
	// not part of the view, just describes the view
	private final boolean isStandardView;
	private final boolean isMember;
	
	private GroupView(
			final Group group,
			final boolean standardView,
			final boolean isMember,
			final Map<ResourceType, ResourceInformationSet> resourceInfo,
			final Function<NumberedCustomField, Boolean> isPublicField,
			final Function<NumberedCustomField, Boolean> isMinimalViewField) {
		this.isStandardView = standardView;
		this.isMember = isMember;
		this.resourceInfo = Collections.unmodifiableMap(resourceInfo);
		
		// group properties
		this.groupID = group.getGroupID();
		this.groupName = group.getGroupName();
		this.owner = group.getOwner();
		this.customFields = getCustomFields(
				group.getCustomFields(), isPublicField, isMinimalViewField);
		this.creationDate = group.getCreationDate();
		this.modificationDate = group.getModificationDate();
		if (!standardView) {
			members = getEmptyImmutableSet();
			admins = getEmptyImmutableSet();
			description = Optional.empty();
		} else {
			admins = group.getAdministrators();
			description = group.getDescription();
			if (!isMember) {
				members = getEmptyImmutableSet();
			} else {
				members = group.getMembers();
			}
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
			if ((isPublic || isMember) && (isMinimal || isStandardView)) {
				ret.put(f, customFields.get(f));
			}
		}
		return Collections.unmodifiableMap(ret);
	}

	private <T> Set<T> getEmptyImmutableSet() {
		return Collections.unmodifiableSet(Collections.emptySet());
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

	/** Get the group ID.
	 * @return the ID.
	 */
	public GroupID getGroupID() {
		return groupID;
	}

	/** Get the group name.
	 * @return the name.
	 */
	public GroupName getGroupName() {
		return groupName;
	}

	/** Get any custom fields associated with the group.
	 * @return the custom fields.
	 */
	public Map<NumberedCustomField, String> getCustomFields() {
		return customFields;
	}
	
	/** Get the owner of the group.
	 * @return the owner.
	 */
	public UserName getOwner() {
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
	
	/** Get the creation date of the group.
	 * @return the creation date.
	 */
	public Instant getCreationDate() {
		return creationDate;
	}

	/** Get the modification date of the group.
	 * @return the modification date.
	 */
	public Instant getModificationDate() {
		return modificationDate;
	}

	/** Get the optional description of the group. {@link Optional#empty()} if not provided
	 * or the view is minimal
	 * @return the description.
	 */
	public Optional<String> getDescription() {
		return description;
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
		result = prime * result + (isMember ? 1231 : 1237);
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((resourceInfo == null) ? 0 : resourceInfo.hashCode());
		result = prime * result + (isStandardView ? 1231 : 1237);
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
		if (isMember != other.isMember) {
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
		if (isStandardView != other.isStandardView) {
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
		
		private Builder(final Group group, final UserName user) {
			checkNotNull(group, "group");
			this.group = group;
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
			checkNotNull(type, "type");
			checkNotNull(info, "info");
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
			checkNotNull(type, "type");
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
			checkNotNull(isPublic, "isPublic");
			this.isPublicField = isPublic;
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
			checkNotNull(isMinimalView, "isMinimalView");
			this.isMinimalViewField = isMinimalView;
			return this;
		}
		
		/** Build a new {@link GroupView}.
		 * @return the view.
		 */
		public GroupView build() {
			return new GroupView(group, isStandardView, group.isMember(user.orElse(null)),
					resourceInfo, isPublicField, isMinimalViewField);
		}
		
	}
}
