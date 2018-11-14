package us.kbase.groups.core;

import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Optional;

/** Parameters for getting a list of groups.
 * @author gaprice@lbl.gov
 *
 */
public class GetGroupsParams {
	
	private final boolean sortAscending;
	private final Optional<String> excludeUpTo;
	
	private GetGroupsParams(final boolean sortAscending, final Optional<String> excludeUpTo) {
		this.sortAscending = sortAscending;
		this.excludeUpTo = excludeUpTo;
	}

	/** Get whether the list should be sorted in ascending or descending order.
	 * @return true if the sort should be ascending, false if descending.
	 */
	public boolean isSortAscending() {
		return sortAscending;
	}

	/** Get a string that determines where a list of requests should begin. If the sort is
	 * ascending, the request list should begin at a string strictly after this string, and
	 * vice versa for descending sorts.
	 * @return the exclusion string.
	 */
	public Optional<String> getExcludeUpTo() {
		return excludeUpTo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((excludeUpTo == null) ? 0 : excludeUpTo.hashCode());
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
		GetGroupsParams other = (GetGroupsParams) obj;
		if (excludeUpTo == null) {
			if (other.excludeUpTo != null) {
				return false;
			}
		} else if (!excludeUpTo.equals(other.excludeUpTo)) {
			return false;
		}
		if (sortAscending != other.sortAscending) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link GetGroupsParams}.
	 * @return the builder.
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/** A builder for a {@link GetGroupsParams}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private boolean sortAscending = true;
		private Optional<String> excludeUpTo = Optional.empty();
		
		private Builder() {}
		
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
		
		/** Set a string that determines where a list of requests should begin. If the sort is
		 * ascending, the request list should begin at a string strictly after this string,
		 * and vice versa for descending sorts.
		 * If null or whitespace only, no string is set.
		 * The string is {@link String#trim()}ed.
		 * @param excludeUpTo the exclusion string.
		 * @return this builder.
		 */
		public Builder withNullableExcludeUpTo(final String excludeUpTo) {
			if (isNullOrEmpty(excludeUpTo)) {
				this.excludeUpTo = Optional.empty();
			} else {
				this.excludeUpTo = Optional.of(excludeUpTo.trim());
			}
			return this;
		}
		
		/** Build the {@link GetGroupsParams}.
		 * @return the params.
		 */
		public GetGroupsParams build() {
			return new GetGroupsParams(sortAscending, excludeUpTo);
		}
	}
}
