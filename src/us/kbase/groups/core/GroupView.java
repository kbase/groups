package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;
import us.kbase.groups.core.workspace.WorkspacePermission;

/** A view of a {@link Group}. A view consists of a subset of the full information in a
 * {@link Group}.
 * @author gaprice@lbl.gov
 *
 */
public class GroupView {

	/** The type of the view.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static enum ViewType {
		/** A minimal view, consisting of the group ID, name, owner, and type. */
		MINIMAL,
		/** A view for a user that is not a member of the group. Includes all fields except
		 * for the members list, which is empty.
		 */
		NON_MEMBER,
		/** A full view for a member of the group. */
		MEMBER;
	}
	
	// group fields
	private final GroupID groupID; // all views
	private final GroupName groupName; // all views
	private final UserName owner; // all views
	private final Map<NumberedCustomField, String> customFields; // all views
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard
	private final Set<CatalogMethod> methods; // standard
	private final GroupType type; // all views
	private final Optional<Instant> creationDate; // standard
	private final Optional<Instant> modificationDate; // standard
	private final Optional<String> description; // standard

	// additional fields. standard - contents should change based on user
	private final Map<WorkspaceInformation, WorkspacePermission> workspaceSet;
	
	// not part of the view, just describes the view
	private final ViewType viewType;
	
	/** Create a view.
	 * @param group the group upon which the view will be based.
	 * @param workspaceSet the workspaces that the user that requested the view can view.
	 * @param viewType the type of the view.
	 */
	public GroupView(
			final Group group,
			final WorkspaceInfoSet workspaceSet,
			final ViewType viewType) {
		checkNotNull(group, "group");
		checkNotNull(workspaceSet, "workspaceSet");
		checkNotNull(viewType, "viewType");
		this.viewType = viewType;
		final Map<WorkspaceInformation, WorkspacePermission> wsSet = new HashMap<>();
		for (final WorkspaceInformation wsi: workspaceSet.getWorkspaceInformation()) {
			wsSet.put(wsi, workspaceSet.getPermission(wsi));
		}
		this.workspaceSet = Collections.unmodifiableMap(wsSet);
		
		// group properties
		this.groupID = group.getGroupID();
		this.groupName = group.getGroupName();
		this.owner = group.getOwner();
		this.type = group.getType();
		this.customFields = group.getCustomFields();
		if (viewType.equals(ViewType.MINIMAL)) {
			members = getEmptyImmutableSet();
			admins = getEmptyImmutableSet();
			methods = getEmptyImmutableSet();
			creationDate = Optional.empty();
			modificationDate = Optional.empty();
			description = Optional.empty();
		} else {
			methods = group.getCatalogMethods();
			admins = group.getAdministrators();
			creationDate = Optional.of(group.getCreationDate());
			modificationDate = Optional.of(group.getModificationDate());
			description = group.getDescription();
			if (viewType.equals(ViewType.NON_MEMBER)) {
				members = getEmptyImmutableSet();
			} else {
				members = group.getMembers();
			}
		}
	}
	
	private <T> Set<T> getEmptyImmutableSet() {
		return Collections.unmodifiableSet(Collections.emptySet());
	}

	/** Get the type of the view.
	 * @return the view type.
	 */
	public ViewType getViewType() {
		return viewType;
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

	/** Get the members of the group. Empty for all views except {@link ViewType#MEMBER}.
	 * @return the group members.
	 */
	public Set<UserName> getMembers() {
		return members;
	}

	/** Get the administrators of the group. Empty for {@link ViewType#MINIMAL} views.
	 * @return the group administrators.
	 */
	public Set<UserName> getAdministrators() {
		return admins;
	}
	
	/** Get the Catalog methods associated with the group. Empty for {@link ViewType#MINIMAL}
	 * views.
	 * @return the methods.
	 */
	public Set<CatalogMethod> getCatalogMethods() {
		return methods;
	}

	/** Get the type of the group.
	 * @return the group type.
	 */
	public GroupType getType() {
		return type;
	}

	/** Get the creation date of the group. {@link Optional#empty()} for {@link ViewType#MINIMAL}
	 * views.
	 * @return the creation date.
	 */
	public Optional<Instant> getCreationDate() {
		return creationDate;
	}

	/** Get the modification date of the group. {@link Optional#empty()} for
	 * {@link ViewType#MINIMAL} views.
	 * @return the modification date.
	 */
	public Optional<Instant> getModificationDate() {
		return modificationDate;
	}

	/** Get the optional description of the group. {@link Optional#empty()} if not provided
	 * or the view is {@link ViewType#MINIMAL}.
	 * @return the description.
	 */
	public Optional<String> getDescription() {
		return description;
	}

	/** Get any workspaces included in the view.
	 * @return the workspace information for the workspaces.
	 */
	public Set<WorkspaceInformation> getWorkspaceInformation() {
		return workspaceSet.keySet();
	}
	
	/** Get the permission of the user for whom this view was created to a
	 * workspace included in the view.
	 * @param wsInfo the workspace.
	 * @return the user's permission.
	 */
	public WorkspacePermission getPermission(final WorkspaceInformation wsInfo) {
		checkNotNull(wsInfo, "wsInfo");
		if (!workspaceSet.containsKey(wsInfo)) {
			throw new IllegalArgumentException("Provided workspace info not included in view");
		} else {
			return workspaceSet.get(wsInfo);
		}
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
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((viewType == null) ? 0 : viewType.hashCode());
		result = prime * result + ((workspaceSet == null) ? 0 : workspaceSet.hashCode());
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
		if (members == null) {
			if (other.members != null) {
				return false;
			}
		} else if (!members.equals(other.members)) {
			return false;
		}
		if (methods == null) {
			if (other.methods != null) {
				return false;
			}
		} else if (!methods.equals(other.methods)) {
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
		if (viewType != other.viewType) {
			return false;
		}
		if (workspaceSet == null) {
			if (other.workspaceSet != null) {
				return false;
			}
		} else if (!workspaceSet.equals(other.workspaceSet)) {
			return false;
		}
		return true;
	}
}
