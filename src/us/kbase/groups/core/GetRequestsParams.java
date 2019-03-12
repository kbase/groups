package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Optional;

import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

/** A set of parameters for use when listing {@link GroupRequest}s.
 * @author gaprice@lbl.gov
 *
 */
public class GetRequestsParams {

	private final boolean includeClosed;
	private final boolean sortAscending;
	private final Optional<Instant> excludeUpTo;
	private final Optional<ResourceType> resourceType;
	private final Optional<ResourceID> resourceID;
	
	private GetRequestsParams(
			final boolean includeClosed,
			final boolean sortAscending,
			final Optional<Instant> excludeUpTo,
			final Optional<ResourceType> resourceType,
			final Optional<ResourceID> resourceID) {
		this.includeClosed = includeClosed;
		this.sortAscending = sortAscending;
		this.excludeUpTo = excludeUpTo;
		this.resourceType = resourceType;
		this.resourceID = resourceID;
	}

	/** Get whether closed requests should be included in the list. Any request with a status type
	 * other than {@link GroupRequestStatusType#OPEN} is closed.
	 * @return whether closed requests should be included.
	 */
	public boolean isIncludeClosed() {
		return includeClosed;
	}

	/** Get whether the list should be sorted in ascending or descending order.
	 * @return true if the sort should be ascending, false if descending.
	 */
	public boolean isSortAscending() {
		return sortAscending;
	}

	/** Get a date that determines where a list of requests should begin. If the sort is
	 * ascending, the request list should begin at a date strictly later than this date, and
	 * vice versa for descending sorts.
	 * @return the exclusion date.
	 */
	public Optional<Instant> getExcludeUpTo() {
		return excludeUpTo;
	}
	
	/** Get the resource type that must limit the list of requests. If the type is present,
	 * {@link #getResourceID()} will always return a resource ID. The combination of the two
	 * must limit the list of requests.
	 * @return the resource type that all the requests must possess.
	 */
	public Optional<ResourceType> getResourceType() {
		return resourceType;
	}
	
	/** Get the resource ID that must limit the list of requests. If the ID is present,
	 * {@link #getResourceType()} will always return a resource type. The combination of the two
	 * must limit the list of requests.
	 * @return the resource ID that all the requests must possess.
	 */
	public Optional<ResourceID> getResourceID() {
		return resourceID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((excludeUpTo == null) ? 0 : excludeUpTo.hashCode());
		result = prime * result + (includeClosed ? 1231 : 1237);
		result = prime * result + ((resourceID == null) ? 0 : resourceID.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + (sortAscending ? 1231 : 1237);
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
		GetRequestsParams other = (GetRequestsParams) obj;
		if (excludeUpTo == null) {
			if (other.excludeUpTo != null) {
				return false;
			}
		} else if (!excludeUpTo.equals(other.excludeUpTo)) {
			return false;
		}
		if (includeClosed != other.includeClosed) {
			return false;
		}
		if (resourceID == null) {
			if (other.resourceID != null) {
				return false;
			}
		} else if (!resourceID.equals(other.resourceID)) {
			return false;
		}
		if (resourceType == null) {
			if (other.resourceType != null) {
				return false;
			}
		} else if (!resourceType.equals(other.resourceType)) {
			return false;
		}
		if (sortAscending != other.sortAscending) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link GetRequestsParams}.
	 * @return the builder.
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/** A builder for a {@link GetRequestsParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private boolean includeClosed = false;
		private boolean sortAscending = true;
		private Optional<Instant> excludeUpTo = Optional.empty();
		private Optional<ResourceType> resourceType = Optional.empty();
		private Optional<ResourceID> resourceID = Optional.empty();
		
		private Builder() {}
		
		/** Set whether closed requests should be included in the returned list.
		 * Any request with a status type other than {@link GroupRequestStatusType#OPEN} is closed.
		 * If null, the default of false is used.
		 * @param includeClosed true to include closed requests, false otherwise.
		 * @return this builder.
		 */
		public Builder withNullableIncludeClosed(final Boolean includeClosed) {
			if (includeClosed == null) {
				this.includeClosed = false;
			} else {
				this.includeClosed = includeClosed;
			}
			return this;
		}
		
		/** Set whether the list should be sorted in ascending or descending order.
		 * If null, the default of true is used.
		 * @param sortAscending true sort ascending, false for descending.
		 * @return this builder.
		 */
		public Builder withNullableSortAscending(final Boolean sortAscending) {
			if (sortAscending == null) {
				this.sortAscending = true;
			} else {
				this.sortAscending = sortAscending;
			}
			return this;
		}
		
		/** Set a date that determines where a list of requests should begin. If the sort is
		 * ascending, the request list should begin at a date strictly later than this date, and
		 * vice versa for descending sorts.
		 * If null, no date is set.
		 * @param excludeUpTo the exclusion date.
		 * @return this builder.
		 */
		public Builder withNullableExcludeUpTo(final Instant excludeUpTo) {
			this.excludeUpTo = Optional.ofNullable(excludeUpTo);
			return this;
		}
		
		/** Set a resource ID that limits the list of requests to include only requests
		 * that contain that resource ID.
		 * @param type the type of the resource.
		 * @param id the resource ID.
		 * @return this builder.
		 */
		public Builder withResource(final ResourceType type, final ResourceID id) {
			this.resourceType = Optional.of(requireNonNull(type, "type"));
			this.resourceID = Optional.of(requireNonNull(id, "id"));
			return this;
		}
		
		/** Build the {@link GetRequestsParams}.
		 * @return the parameters.
		 */
		public GetRequestsParams build() {
			return new GetRequestsParams(includeClosed, sortAscending, excludeUpTo,
					resourceType, resourceID);
		}
	}
}
