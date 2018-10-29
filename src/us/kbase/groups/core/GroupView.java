package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

import us.kbase.groups.core.workspace.WorkspaceInfoSet;
import us.kbase.groups.core.workspace.WorkspaceInformation;

public class GroupView {

	// TODO JAVADOC
	// TODO TEST
	
	public static enum ViewType {
		MINIMAL,
		NON_MEMBER,
		MEMBER;
	}
	
	// group fields
	private final GroupID groupID; // all views
	private final GroupName groupName; // all views
	private final UserName owner; // all views
	private final Set<UserName> members; // member
	private final Set<UserName> admins; // standard
	private final GroupType type; // all views
	private final Optional<Instant> creationDate; // standard
	private final Optional<Instant> modificationDate; // standard
	private final Optional<String> description; // standard

	// additional fields. standard - contents should change based on user
	private final Map<WorkspaceInformation, Boolean> workspaceSet;
	
	// not part of the view, just describes the view
	private final ViewType viewType;
	
	public GroupView(
			final Group group,
			final WorkspaceInfoSet workspaceSet,
			final ViewType viewType) {
		checkNotNull(group, "group");
		checkNotNull(workspaceSet, "workspaceSet");
		checkNotNull(viewType, "viewType");
		this.viewType = viewType;
		final Map<WorkspaceInformation, Boolean> wsSet = new HashMap<>();
		for (final WorkspaceInformation wsi: workspaceSet.getWorkspaceInformation()) {
			wsSet.put(wsi, workspaceSet.isAdministrator(wsi));
		}
		this.workspaceSet = Collections.unmodifiableMap(wsSet);
		
		// group properties
		this.groupID = group.getGroupID();
		this.groupName = group.getGroupName();
		this.owner = group.getOwner();
		this.type = group.getType();
		if (viewType.equals(ViewType.MINIMAL)) {
			members = getEmptyImmutableSet();
			admins = getEmptyImmutableSet();
			creationDate = Optional.absent();
			modificationDate = Optional.absent();
			description = Optional.absent();
		} else {
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

	public ViewType getViewType() {
		return viewType;
	}

	public GroupID getGroupID() {
		return groupID;
	}

	public GroupName getGroupName() {
		return groupName;
	}

	public UserName getOwner() {
		return owner;
	}

	public Set<UserName> getMembers() {
		return members;
	}

	public Set<UserName> getAdministrators() {
		return admins;
	}

	public GroupType getType() {
		return type;
	}

	public Optional<Instant> getCreationDate() {
		return creationDate;
	}

	public Optional<Instant> getModificationDate() {
		return modificationDate;
	}

	public Optional<String> getDescription() {
		return description;
	}

	public Set<WorkspaceInformation> getWorkspaceInformation() {
		return workspaceSet.keySet();
	}
	
	public boolean isAdministrator(final WorkspaceInformation wsInfo) {
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
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
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
