package us.kbase.groups.core;

import java.time.Instant;
import java.util.Optional;

public class GetRequestsParams {

	// TODO JAVADOC
	// TODO TEST
	
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

	public boolean isIncludeClosed() {
		return includeClosed;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

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
	
	public static Builder getBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private boolean includeClosed = false;
		private boolean sortAscending = true;
		private Optional<Instant> excludeUpTo = Optional.empty();
		
		private Builder() {}
		
		public Builder withNullableIncludeClosed(final Boolean includeClosed) {
			if (includeClosed == null) {
				this.includeClosed = false;
			} else {
				this.includeClosed = includeClosed;
			}
			return this;
		}
		
		public Builder withNullableSortAscending(final Boolean sortAscending) {
			if (sortAscending == null) {
				this.sortAscending = false;
			} else {
				this.sortAscending = sortAscending;
			}
			return this;
		}
		
		public Builder withNullableExcludeUpTo(final Instant excludeUpTo) {
			this.excludeUpTo = Optional.ofNullable(excludeUpTo);
			return this;
		}
		
		public GetRequestsParams build() {
			return new GetRequestsParams(includeClosed, sortAscending, excludeUpTo);
		}
	}
}
