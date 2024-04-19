package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.core.request.GroupRequestStatusType.ACCEPTED;
import static us.kbase.groups.core.request.GroupRequestStatusType.CANCELED;
import static us.kbase.groups.core.request.GroupRequestStatusType.DENIED;
import static us.kbase.groups.core.request.GroupRequestStatusType.EXPIRED;
import static us.kbase.groups.core.request.GroupRequestStatusType.OPEN;
import static us.kbase.groups.util.Util.codePoints;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Optional;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;

/** The status of a {@link GroupRequest}. Includes a {@link GroupRequestStatusType},
 * the user that accepted or denied the request for those types, and an optional denial reason
 * for denied requests.
 * @author gaprice@lbl.gov
 *
 */
public class GroupRequestStatus {
	
	/** The maximum size of the reason string for denied requests. */
	public final static int MAX_REASON_SIZE = 500;
	
	private final GroupRequestStatusType statusType;
	private final Optional<UserName> closedBy;
	private final Optional<String> closedReason;
	
	private GroupRequestStatus(
			final GroupRequestStatusType statusType,
			final Optional<UserName> closedBy,
			final Optional<String> closedReason) {
		this.statusType = statusType;
		this.closedBy = closedBy;
		this.closedReason = closedReason;
	}

	/** Get the type of the status.
	 * @return the type.
	 */
	public GroupRequestStatusType getStatusType() {
		return statusType;
	}

	/** Get the user that closed the request for accepted and denied requests.
	 * @return the closing user.
	 */
	public Optional<UserName> getClosedBy() {
		return closedBy;
	}

	/** Get the reason the request was closed, if any. Only present for denied requests.
	 * @return the closure reason.
	 */
	public Optional<String> getClosedReason() {
		return closedReason;
	}

	private static final Optional<UserName> OPTUSER = Optional.empty();
	private static final Optional<String> OPTREASON = Optional.empty();
	
	/** Get an open status. In this case, the closed by and closed reason fields are empty.
	 * @return the new status.
	 */
	public static GroupRequestStatus open() {
		return new GroupRequestStatus(OPEN, OPTUSER, OPTREASON);
	}
	
	/** Get a canceled status. In this case, the closed by and closed reason fields are empty.
	 * @return the new status.
	 */
	public static GroupRequestStatus canceled() {
		return new GroupRequestStatus(CANCELED, OPTUSER, OPTREASON);
	}
	
	/** Get an expired status. In this case, the closed by and closed reason fields are empty.
	 * @return the new status.
	 */
	public static GroupRequestStatus expired() {
		return new GroupRequestStatus(EXPIRED, OPTUSER, OPTREASON);
	}
	
	/** Get an accepted status. In this case, the closed reason field is empty.
	 * @param acceptedBy the user that closed the request.
	 * @return the new status.
	 */
	public static GroupRequestStatus accepted(final UserName acceptedBy) {
		checkNotNull(acceptedBy, "acceptedBy");
		return new GroupRequestStatus(ACCEPTED, Optional.of(acceptedBy), OPTREASON);
	}
	
	/** Get a denied status. If the reason is null or whitespace only, it is ignored.
	 * @param deniedBy the user that closed the request.
	 * @param reason the reason the request was denied.
	 * @return the new status.
	 * @throws IllegalParameterException if the reason is too long.
	 */
	public static GroupRequestStatus denied(final UserName deniedBy, String reason)
			throws IllegalParameterException {
		checkNotNull(deniedBy, "deniedBy");
		if (isNullOrEmpty(reason)) {
			reason = null;
		} else {
			reason = reason.trim();
			if (codePoints(reason) > MAX_REASON_SIZE) {
				throw new IllegalParameterException(
						"reason size greater than limit " + MAX_REASON_SIZE);
			}
		}
		return new GroupRequestStatus(
				DENIED, Optional.of(deniedBy), Optional.ofNullable(reason));
	}
	
	/** Construct a request status freeform. The closed by and reason fields are processed as
	 * if the correct method for the status type was called.
	 * @param statusType the status type of this status.
	 * @param closedBy the user that closed the request. Required for
	 * {@link GroupRequestStatusType#ACCEPTED} and {@link GroupRequestStatusType#DENIED}.
	 * @param reason the reason the request was closed. Ignored if null, whitespace only, or if
	 * the status type is not {@link GroupRequestStatusType#DENIED}.
	 * @return the new status.
	 * @throws IllegalParameterException if the reason is too long.
	 */
	public static GroupRequestStatus from(
			final GroupRequestStatusType statusType,
			final UserName closedBy,
			final String reason)
			throws IllegalParameterException {
		checkNotNull(statusType, "statusType");
		if (OPEN.equals(statusType) || EXPIRED.equals(statusType) || CANCELED.equals(statusType)) {
			return new GroupRequestStatus(statusType, OPTUSER, OPTREASON);
		}
		checkNotNull(closedBy, "closedBy");
		if (ACCEPTED.equals(statusType)) {
			return accepted(closedBy);
		} else {
			return denied(closedBy, reason);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((closedBy == null) ? 0 : closedBy.hashCode());
		result = prime * result + ((closedReason == null) ? 0 : closedReason.hashCode());
		result = prime * result + ((statusType == null) ? 0 : statusType.hashCode());
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
		GroupRequestStatus other = (GroupRequestStatus) obj;
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
		if (statusType != other.statusType) {
			return false;
		}
		return true;
	}
}
