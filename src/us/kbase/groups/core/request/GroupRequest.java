package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Optional;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.catalog.CatalogMethod;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.Group;

/** Represents a request to modify a {@link Group} in some way.
 * @author gaprice@lbl.gov
 *
 */
public class GroupRequest {
	
	private final RequestID id;
	private final GroupID groupID;
	private final Optional<UserName> target;
	private final Optional<WorkspaceID> wsTarget;
	private final Optional<CatalogMethod> catTarget;
	private final UserName requester;
	private final GroupRequestType type;
	private final GroupRequestStatusType statusType;
	private final Optional<UserName> closedBy;
	private final Optional<String> closedReason;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Instant expirationDate;
	
	private GroupRequest(
			final RequestID id,
			final GroupID groupID,
			final Optional<UserName> target,
			final Optional<WorkspaceID> wsTarget,
			final Optional<CatalogMethod> catTarget,
			final UserName requester,
			final GroupRequestType type,
			final GroupRequestStatusType status,
			final Optional<UserName> closedBy,
			final Optional<String> closedReason,
			final CreateModAndExpireTimes times) {
		this.id = id;
		this.groupID = groupID;
		this.target = target;
		this.wsTarget = wsTarget;
		this.catTarget = catTarget;
		this.requester = requester;
		this.type = type;
		this.statusType = status;
		this.closedBy = closedBy;
		this.closedReason = closedReason;
		this.creationDate = times.getCreationTime();
		this.modificationDate = times.getModificationTime();
		this.expirationDate = times.getExpirationTime();
	}

	/** Get the ID of the request.
	 * @return the ID.
	 */
	public RequestID getID() {
		return id;
	}

	/** Get the ID of the group the request will affect if accepted.
	 * @return the group ID.
	 */
	public GroupID getGroupID() {
		return groupID;
	}

	/** Get the user targeted by the request, if any. The target is present when the request
	 * type is {@link GroupRequestType#INVITE_TO_GROUP} or
	 * {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP}. In this case the user is added to the
	 * group if the request is accepted.
	 * @return the target user.
	 */
	public Optional<UserName> getTarget() {
		return target;
	}
	
	/** Get the workspace targeted by the request, if any. The target is present when the
	 * request type is {@link GroupRequestType#INVITE_WORKSPACE} or
	 * {@link GroupRequestType#REQUEST_ADD_WORKSPACE}. In this case the workspace is added to the
	 * group if the request is accepted.
	 * @return the target workspace.
	 */
	public Optional<WorkspaceID> getWorkspaceTarget() {
		return wsTarget;
	}
	
	/** Get the catalog targeted by the request, if any. The target is present when the
	 * request type is {@link GroupRequestType#INVITE_CATALOG_METHOD} or
	 * {@link GroupRequestType#REQUEST_ADD_CATALOG_METHOD}. In this case the catalog method is
	 * added to the group if the request is accepted.
	 * @return the target workspace.
	 */
	public Optional<CatalogMethod> getCatalogMethodTarget() {
		return catTarget;
	}

	/** Get the user that created the request.
	 * @return the user that made the request.
	 */
	public UserName getRequester() {
		return requester;
	}

	/** Get the type of the request.
	 * @return the request type.
	 */
	public GroupRequestType getType() {
		return type;
	}
	
	/** Returns true if the request is an invitation, or false if it is a request.
	 * Invitations are {@link GroupRequestType#INVITE_TO_GROUP},
	 * {@link GroupRequestType#INVITE_WORKSPACE}, and
	 * {@link GroupRequestType#INVITE_CATALOG_METHOD}.
	 * Requests are {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP},
	 * {@link GroupRequestType#REQUEST_ADD_WORKSPACE}, and
	 * {@link GroupRequestType#REQUEST_ADD_CATALOG_METHOD}.
	 * @return true if the request is an invitation.
	 */
	public boolean isInvite() {
		return GroupRequestType.INVITE_TO_GROUP.equals(type) ||
				GroupRequestType.INVITE_WORKSPACE.equals(type) ||
				GroupRequestType.INVITE_CATALOG_METHOD.equals(type);
	}

	/** Get the type of the status of the request.
	 * @return the request status type.
	 */
	public GroupRequestStatusType getStatusType() {
		return statusType;
	}
	
	/** Get whether this request is open or not.
	 * @return true if the request is open, false otherwise.
	 */
	public boolean isOpen() {
		return GroupRequestStatusType.OPEN.equals(statusType);
	}

	/** Get the user that closed the request. Only present when the status type is
	 * {@link GroupRequestStatusType#ACCEPTED} or {@link GroupRequestStatusType#DENIED}.
	 * @return the closing user.
	 */
	public Optional<UserName> getClosedBy() {
		return closedBy;
	}

	/** Get the reason the request was closed. Only present when the status type is
	 * {@link GroupRequestStatusType#DENIED}, and even then may be omitted.
	 * @return the close reason.
	 */
	public Optional<String> getClosedReason() {
		return closedReason;
	}

	/** Get the date the request was created.
	 * @return the creation date.
	 */
	public Instant getCreationDate() {
		return creationDate;
	}

	/** Get the date the request was modified.
	 * @return the modification date.
	 */
	public Instant getModificationDate() {
		return modificationDate;
	}

	/** Get the date the request expires.
	 * @return the expiration date.
	 */
	public Instant getExpirationDate() {
		return expirationDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((catTarget == null) ? 0 : catTarget.hashCode());
		result = prime * result + ((closedBy == null) ? 0 : closedBy.hashCode());
		result = prime * result + ((closedReason == null) ? 0 : closedReason.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((requester == null) ? 0 : requester.hashCode());
		result = prime * result + ((statusType == null) ? 0 : statusType.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((wsTarget == null) ? 0 : wsTarget.hashCode());
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
		GroupRequest other = (GroupRequest) obj;
		if (catTarget == null) {
			if (other.catTarget != null) {
				return false;
			}
		} else if (!catTarget.equals(other.catTarget)) {
			return false;
		}
		if (closedBy == null) {
			if (other.closedBy != null) {
				return false;
			}
		} else if (!closedBy.equals(other.closedBy)) {
			return false;
		}
		if (closedReason == null) {
			if (other.closedReason != null) {
				return false;
			}
		} else if (!closedReason.equals(other.closedReason)) {
			return false;
		}
		if (creationDate == null) {
			if (other.creationDate != null) {
				return false;
			}
		} else if (!creationDate.equals(other.creationDate)) {
			return false;
		}
		if (expirationDate == null) {
			if (other.expirationDate != null) {
				return false;
			}
		} else if (!expirationDate.equals(other.expirationDate)) {
			return false;
		}
		if (groupID == null) {
			if (other.groupID != null) {
				return false;
			}
		} else if (!groupID.equals(other.groupID)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (modificationDate == null) {
			if (other.modificationDate != null) {
				return false;
			}
		} else if (!modificationDate.equals(other.modificationDate)) {
			return false;
		}
		if (requester == null) {
			if (other.requester != null) {
				return false;
			}
		} else if (!requester.equals(other.requester)) {
			return false;
		}
		if (statusType != other.statusType) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		if (wsTarget == null) {
			if (other.wsTarget != null) {
				return false;
			}
		} else if (!wsTarget.equals(other.wsTarget)) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link GroupRequest}. By default, the type is
	 * {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP} and the target user is the requester.
	 * @param id the request ID.
	 * @param groupID the ID of the group at which the request is targeted.
	 * @param requester the user making the request.
	 * @param times the creation, modification, and expiration times for the request.
	 * @return the builder.
	 */
	public static Builder getBuilder(
			final RequestID id,
			final GroupID groupID,
			final UserName requester,
			final CreateModAndExpireTimes times) {
		return new Builder(id, groupID, requester, times);
	}
	
	/** A {@link GroupRequest} builder.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final RequestID id;
		private final GroupID groupID;
		private final UserName requester;
		private final CreateModAndExpireTimes times;
		private Optional<UserName> target;
		private Optional<WorkspaceID> wsTarget = Optional.empty();
		private Optional<CatalogMethod> catTarget = Optional.empty();
		private GroupRequestType type = GroupRequestType.REQUEST_GROUP_MEMBERSHIP;
		private GroupRequestStatusType status = GroupRequestStatusType.OPEN;
		private Optional<UserName> closedBy = Optional.empty();
		private Optional<String> closedReason = Optional.empty();

		private Builder(
				final RequestID id,
				final GroupID groupID,
				final UserName requester,
				final CreateModAndExpireTimes times) {
			checkNotNull(id, "id");
			checkNotNull(groupID, "groupID");
			checkNotNull(requester, "requester");
			checkNotNull(times, "times");
			this.id = id;
			this.groupID = groupID;
			this.requester = requester;
			this.times = times;
			this.target = Optional.ofNullable(requester);
		}
		
		/** Request membership to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP} as the type and null
		 *  user, method, and workspace ID.
		 * @return this builder.
		 */
		public Builder withRequestGroupMembership() {
			this.target = Optional.of(requester);
			this.wsTarget = Optional.empty();
			this.catTarget = Optional.empty();
			this.type = GroupRequestType.REQUEST_GROUP_MEMBERSHIP;
			return this;
		}
		
		/** Invite a user to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#INVITE_TO_GROUP} as the type and a non-null user and
		 * null workspace ID and method.
		 * @param target the user to invite to the group.
		 * @return this builder.
		 */
		public Builder withInviteToGroup(final UserName target) {
			checkNotNull(target, "target");
			this.target = Optional.of(target);
			this.wsTarget = Optional.empty();
			this.catTarget = Optional.empty();
			this.type = GroupRequestType.INVITE_TO_GROUP;
			return this;
		}
		
		/** Request that a workspace is added to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#REQUEST_ADD_WORKSPACE} as the type and a non-null
		 * workspace ID and a null user and method.
		 * @param wsid the workspace ID that is the subject of the request.
		 * @return this builder.
		 */
		public Builder withRequestAddWorkspace(final WorkspaceID wsid) {
			return setWorkspaceType(wsid, GroupRequestType.REQUEST_ADD_WORKSPACE);
		}

		private Builder setWorkspaceType(final WorkspaceID wsid, final GroupRequestType type) {
			checkNotNull(wsid, "wsid");
			this.target = Optional.empty();
			this.wsTarget = Optional.of(wsid);
			this.catTarget = Optional.empty();
			this.type = type;
			return this;
		}
		
		/** Invite a workspace to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#INVITE_WORKSPACE} as the type and a non-null
		 * workspace ID and a null user and method.
		 * @param wsid the workspace ID that is to be invited to the group.
		 * @return this builder.
		 */
		public Builder withInviteWorkspace(final WorkspaceID wsid) {
			return setWorkspaceType(wsid, GroupRequestType.INVITE_WORKSPACE);
		}
		
		/** Request that a catalog method is added to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#REQUEST_ADD_CATALOG_METHOD} as the type and a non-null
		 * method and a null user and workspace ID.
		 * @param method the catalog method that is the subject of the request.
		 * @return this builder.
		 */
		public Builder withRequestAddCatalogMethod(final CatalogMethod method) {
			return setCatalogType(method, GroupRequestType.REQUEST_ADD_CATALOG_METHOD);
		}

		private Builder setCatalogType(final CatalogMethod method, final GroupRequestType type) {
			checkNotNull(method, "method");
			this.target = Optional.empty();
			this.wsTarget = Optional.empty();
			this.catTarget = Optional.of(method);
			this.type = type;
			return this;
		}
		
		/** Invite a catalog method to a group.
		 * The equivalent of
		 * {@link #withType(GroupRequestType, UserName, WorkspaceID, CatalogMethod)}
		 * with {@link GroupRequestType#INVITE_CATALOG_METHOD} as the type and a non-null
		 * method and a null user and workspace ID.
		 * @param method the catalog method that is the subject of the request.
		 * @return this builder.
		 */
		public Builder withInviteCatalogMethod(final CatalogMethod method) {
			return setCatalogType(method, GroupRequestType.INVITE_CATALOG_METHOD);
		}
		
		/** Set the type of this request.
		 * A user is required for {@link GroupRequestType#INVITE_TO_GROUP} requests.
		 * A workspace ID is required for {@link GroupRequestType#REQUEST_ADD_WORKSPACE} and
		 * {@link GroupRequestType#INVITE_WORKSPACE} requests.
		 * A catalog method is required for {@link GroupRequestType#REQUEST_ADD_CATALOG_METHOD} and
		 * {@link GroupRequestType#INVITE_CATALOG_METHOD} requests.
		 * @param type the type of the request.
		 * @param nullableUser an optional user.
		 * @param nullableWorkspaceID an optional workspace ID.
		 * @param nullableCatalogMethod an optional catalog method.
		 * @return this builder.
		 */
		public Builder withType(
				final GroupRequestType type,
				final UserName nullableUser,
				final WorkspaceID nullableWorkspaceID,
				final CatalogMethod nullableCatalogMethod) {
			checkNotNull(type, "type");
			if (type.equals(GroupRequestType.INVITE_TO_GROUP)) {
				if (nullableUser == null) {
					throw new IllegalArgumentException("Group invites must have a target user");
				}
				return withInviteToGroup(nullableUser);
			} else if (type.equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
				return withRequestGroupMembership();
			} else if (type.equals(GroupRequestType.REQUEST_ADD_WORKSPACE)) {
				checkWSID(nullableWorkspaceID);
				return withRequestAddWorkspace(nullableWorkspaceID);
			} else if (type.equals(GroupRequestType.INVITE_WORKSPACE)) {
				checkWSID(nullableWorkspaceID);
				return withInviteWorkspace(nullableWorkspaceID);
			} else if (type.equals(GroupRequestType.REQUEST_ADD_CATALOG_METHOD)) {
				checkMethod(nullableCatalogMethod);
				return withRequestAddCatalogMethod(nullableCatalogMethod);
			} else if (type.equals(GroupRequestType.INVITE_CATALOG_METHOD)) {
				checkMethod(nullableCatalogMethod);
				return withInviteCatalogMethod(nullableCatalogMethod);
			} else {
				// since type is an enum, this is impossible to test unless we're missing a case
				throw new RuntimeException("Unknown type: " + type);
			}
		}
		
		private void checkWSID(final WorkspaceID wsid) {
			if (wsid == null) {
				throw new IllegalArgumentException(
						"Workspace requests and invites must have a target workspace");
			}
		}
		
		private void checkMethod(final CatalogMethod method) {
			if (method == null) {
				throw new IllegalArgumentException(
						"Catalog method requests and invites must have a target method");
			}
		}

		/** Set the status of the request. The default status is {@link GroupRequestStatus#open()}.
		 * @param status the status.
		 * @return this builder.
		 */
		public Builder withStatus(final GroupRequestStatus status) {
			checkNotNull(status, "status");
			this.status = status.getStatusType();
			this.closedBy = status.getClosedBy();
			this.closedReason = status.getClosedReason();
			return this;
		}
		
		/** Builder the {@link GroupRequest}.
		 * @return the new instance.
		 */
		public GroupRequest build() {
			return new GroupRequest(id, groupID, target, wsTarget, catTarget, requester, type,
					status, closedBy, closedReason, times);
		}
	}
}
