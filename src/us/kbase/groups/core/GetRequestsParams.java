package us.kbase.groups.core;

import java.time.Instant;
import java.util.Optional;

import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatusType;

/** A set of parameters for use when listing {@link GroupRequest}s.
 * @author gaprice@lbl.gov
 *
 */
public class GetRequestsParams {

	private final boolean includeClosed;
	private final boolean sortAscending;
	private final Optional<Instant> excludeUpTo;
	
	private GetRequestsParams(
			final boolean includeClosed,
			final boolean sortAscending,
			final Optional<Instant> excludeUpTo) {
		this.includeClosed = includeClosed;
		this.sortAscending = sortAscending;
		this.excludeUpTo = excludeUpTo;
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
	 * @return the exlusion date.
	 */
	public Optional<Instant> getExcludeUpTo() {
		return excludeUpTo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((excludeUpTo == null) ? 0 : excludeUpTo.hashCode());
		result = prime * result + (includeClosed ? 1231 : 1237);
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
		
		/** Build the {@link GetRequestsParams}.
		 * @return the parameters.
		 */
		public GetRequestsParams build() {
			return new GetRequestsParams(includeClosed, sortAscending, excludeUpTo);
		}
	}
}
