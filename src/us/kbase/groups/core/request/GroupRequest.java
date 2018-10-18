package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.UUID;

import com.google.common.base.Optional;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;

public class GroupRequest {
	
	// TODO JAVADOC
	// TODO TEST
	// TODO NOW expire requests in DB - need a thread running
	
	// TODO NOW make a real ID class that wraps UUID
	private final UUID id;
	private final GroupID groupID;
	private final Optional<UserName> target;
	private final UserName requester;
	private final GroupRequestType type;
	private final GroupRequestStatus status;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Instant expirationDate;
	
	private GroupRequest(
			final UUID id,
			final GroupID groupID,
			final Optional<UserName> target,
			final UserName requester,
			final GroupRequestType type,
			final GroupRequestStatus status,
			final CreateModAndExpireTimes times) {
		this.id = id;
		this.groupID = groupID;
		this.target = target;
		this.requester = requester;
		this.type = type;
		this.status = status;
		this.creationDate = times.getCreationTime();
		this.modificationDate = times.getModificationTime();
		this.expirationDate = times.getExpirationTime();
	}

	public UUID getID() {
		return id;
	}

	public GroupID getGroupID() {
		return groupID;
	}

	public Optional<UserName> getTarget() {
		return target;
	}

	public UserName getRequester() {
		return requester;
	}

	public GroupRequestType getType() {
		return type;
	}

	public GroupRequestStatus getStatus() {
		return status;
	}

	public Instant getCreationDate() {
		return creationDate;
	}

	public Instant getModificationDate() {
		return modificationDate;
	}

	public Instant getExpirationDate() {
		return expirationDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((requester == null) ? 0 : requester.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		GroupRequest other = (GroupRequest) obj;
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
		if (status != other.status) {
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
		return true;
	}

	/** Get a builder for a {@link GroupRequest}. By default, the type is
	 * {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP} and the target user is
	 * {@link Optional#absent()}.
	 * @param id the request ID.
	 * @param groupID the ID of the group at which the request is targeted.
	 * @param requester the user making the request.
	 * @param times the creation, modification, and expiration times for the request.
	 * @return
	 */
	public static Builder getBuilder(
			final UUID id,
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
		
		private final UUID id;
		private final GroupID groupID;
		private final UserName requester;
		private final CreateModAndExpireTimes times;
		private Optional<UserName> target = Optional.absent();
		private GroupRequestType type = GroupRequestType.REQUEST_GROUP_MEMBERSHIP;
		private GroupRequestStatus status = GroupRequestStatus.OPEN;

		private Builder(
				final UUID id,
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
		}
		
		/** The equivalent of {@link #withType(GroupRequestType, UserName)}
		 * with {@link GroupRequestType#REQUEST_GROUP_MEMBERSHIP} as the type and a null user.
		 * @return this builder.
		 */
		public Builder withRequestGroupMembership() {
			this.target = Optional.absent();
			this.type = GroupRequestType.REQUEST_GROUP_MEMBERSHIP;
			return this;
		}
		
		/** The equivalent of {@link #withType(GroupRequestType, UserName)}
		 * with {@link GroupRequestType#INVITE_TO_GROUP} as the type and a non-null user.
		 * @param target the user to invite to the group.
		 * @return this builder.
		 */
		public Builder withInviteToGroup(final UserName target) {
			checkNotNull(target, "target");
			this.target = Optional.of(target);
			this.type = GroupRequestType.INVITE_TO_GROUP;
			return this;
		}
		
		// this'll need a workspace ID in the future
		/** Set the type of this request. A user is required for
		 * {@link GroupRequestType#INVITE_TO_GROUP} requests.
		 * @param type the type of the request.
		 * @param nullableUser an optional user.
		 * @return this builder.
		 */
		public Builder withType(final GroupRequestType type, final UserName nullableUser) {
			checkNotNull(type, "type");
			if (type.equals(GroupRequestType.INVITE_TO_GROUP)) {
				if (nullableUser == null) {
					throw new IllegalArgumentException("Group invites must have a target user");
				}
				return withInviteToGroup(nullableUser);
			} else if (type.equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
				return withRequestGroupMembership();
			} else {
				// since type is an enum, this is impossible to test unless we're missing a case
				throw new RuntimeException("Unknown type: " + type);
			}
		}
		
		/** Set the status of this request.
		 * @param status the status.
		 * @return this builder.
		 */
		public Builder withStatus(final GroupRequestStatus status) {
			checkNotNull(status, "status");
			this.status = status;
			return this;
		}
		
		/** Builder the {@link GroupRequest}.
		 * @return the new instance.
		 */
		public GroupRequest build() {
			return new GroupRequest(id, groupID, target, requester, type, status, times);
		}
	}
}
