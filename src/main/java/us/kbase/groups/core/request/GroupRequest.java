package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Optional;

import us.kbase.groups.core.CreateModAndExpireTimes;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceType;

/** Represents a request to modify a {@link Group} in some way.
 * @author gaprice@lbl.gov
 *
 */
/**
 * @author gaprice@lbl.gov
 *
 */
public class GroupRequest {
	
	public static final ResourceType USER_TYPE;
	static {
		try {
			USER_TYPE = new ResourceType("user");
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("This should be impossible", e);
		}
	}
	
	private final RequestID id;
	private final GroupID groupID;
	private final RequestType type;
	private final ResourceType resourceType;
	private final ResourceDescriptor resource;
	private final UserName requester;
	private final GroupRequestStatusType statusType;
	private final Optional<UserName> closedBy;
	private final Optional<String> closedReason;
	private final Instant creationDate;
	private final Instant modificationDate;
	private final Instant expirationDate;
	
	private GroupRequest(
			final RequestID id,
			final GroupID groupID,
			final RequestType type,
			final ResourceType resourceType,
			final ResourceDescriptor resource,
			final UserName requester,
			final GroupRequestStatusType status,
			final Optional<UserName> closedBy,
			final Optional<String> closedReason,
			final CreateModAndExpireTimes times) {
		this.id = id;
		this.groupID = groupID;
		this.type = type;
		this.resourceType = resourceType;
		this.resource = resource;
		this.requester = requester;
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

	/** Get the type of the request.
	 * @return the type.
	 */
	public RequestType getType() {
		return type;
	}
	
	/** Get the type of the resource that is the target of this request.
	 * @return the resource type.
	 */
	public ResourceType getResourceType() {
		return resourceType;
	}
	
	/** Get the resource that is the target of this request.
	 * @return the resource.
	 */
	public ResourceDescriptor getResource() {
		return resource;
	}

	/** Get the user that created the request.
	 * @return the user that made the request.
	 */
	public UserName getRequester() {
		return requester;
	}

	/** Returns true if the request is an invitation, or false if it is a request.
	 * @return true if the request is an invitation.
	 */
	public boolean isInvite() {
		return type.equals(RequestType.INVITE);
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
		result = prime * result + ((closedBy == null) ? 0 : closedBy.hashCode());
		result = prime * result + ((closedReason == null) ? 0 : closedReason.hashCode());
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modificationDate == null) ? 0 : modificationDate.hashCode());
		result = prime * result + ((requester == null) ? 0 : requester.hashCode());
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + ((statusType == null) ? 0 : statusType.hashCode());
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
		if (resource == null) {
			if (other.resource != null) {
				return false;
			}
		} else if (!resource.equals(other.resource)) {
			return false;
		}
		if (resourceType == null) {
			if (other.resourceType != null) {
				return false;
			}
		} else if (!resourceType.equals(other.resourceType)) {
			return false;
		}
		if (statusType != other.statusType) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link GroupRequest}. By default, the type is
	 * {@link RequestType#REQUEST}, the resource type is "user" and the resource is the user name
	 * of the requester.
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
		private RequestType type = RequestType.REQUEST;
		private ResourceType resourceType = USER_TYPE;
		private ResourceDescriptor resource;
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
			this.resource = ResourceDescriptor.from(requester);
		}
		
		/** Set the type of the request.
		 * @param type the type.
		 * @return this builder.
		 */
		public Builder withType(final RequestType type) {
			checkNotNull(type, "type");
			this.type = type;
			return this;
		}
		
		/** Set the resource that is the target of this request.
		 * @param type the type of the resource.
		 * @param resource the resource.
		 * @return this builder.
		 */
		public Builder withResource(final ResourceType type, final ResourceDescriptor resource) {
			this.resourceType = requireNonNull(type, "type");
			this.resource = requireNonNull(resource, "resource");
			return this;
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
			return new GroupRequest(id, groupID, type, resourceType, resource, requester, status,
					closedBy, closedReason, times);
		}
	}
}
