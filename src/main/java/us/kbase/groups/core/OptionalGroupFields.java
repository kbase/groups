package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;

/** Optional fields associated with a {@link Group}. 
 * @author gaprice@lbl.gov
 *
 */
public class OptionalGroupFields {
	
	private final Optional<Boolean> isPrivate;
	private final Optional<Boolean> privateMemberList;
	private final Map<NumberedCustomField, OptionalString> customFields;

	private OptionalGroupFields(
			final Optional<Boolean> isPrivate,
			final Optional<Boolean> privateMemberList,
			final Map<NumberedCustomField, OptionalString> customFields) {
		this.isPrivate = isPrivate;
		this.privateMemberList = privateMemberList;
		this.customFields = Collections.unmodifiableMap(customFields);
	}

	/** Returns true if at least one field require an update.
	 * @return if a field requires an update.
	 */
	public boolean hasUpdate() {
		return isPrivate.isPresent() || privateMemberList.isPresent() || !customFields.isEmpty();
	}
	
	/** Get any update to the group privacy field. {@link Optional#empty()} indicates no update
	 * is required.
	 * @return the privacy update.
	 */
	public Optional<Boolean> isPrivate() {
		return isPrivate;
	}
	
	/** Get any update to the group member list privacy field. {@link Optional#empty()}
	 * indicates no update is required.
	 * @return the member list privacy update.
	 */
	public Optional<Boolean> isPrivateMemberList() {
		return privateMemberList;
	}
	
	/** Get any custom fields included in the fields.
	 * @return the custom fields.
	 */
	public Set<NumberedCustomField> getCustomFields() {
		return customFields.keySet();
	}
	
	/** Get the value for a custom field.
	 * @param field the field.
	 * @return the value.
	 */
	public OptionalString getCustomValue(final NumberedCustomField field) {
		if (!customFields.containsKey(requireNonNull(field, "field"))) {
			throw new IllegalArgumentException("No such field " + field.getField());
		}
		return customFields.get(field);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((isPrivate == null) ? 0 : isPrivate.hashCode());
		result = prime * result + ((privateMemberList == null) ? 0 : privateMemberList.hashCode());
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
		OptionalGroupFields other = (OptionalGroupFields) obj;
		if (customFields == null) {
			if (other.customFields != null) {
				return false;
			}
		} else if (!customFields.equals(other.customFields)) {
			return false;
		}
		if (isPrivate == null) {
			if (other.isPrivate != null) {
				return false;
			}
		} else if (!isPrivate.equals(other.isPrivate)) {
			return false;
		}
		if (privateMemberList == null) {
			if (other.privateMemberList != null) {
				return false;
			}
		} else if (!privateMemberList.equals(other.privateMemberList)) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link OptionalGroupFields}.
	 * @return the builder.
	 */
	public static Builder getBuilder() {
		return new Builder();
	}
	
	/** Get the default set of {@link OptionalGroupFields}, which contains no field updates.
	 * @return the fields.
	 */
	public static OptionalGroupFields getDefault() {
		return getBuilder().build();
	}
	
	/** A builder for a {@link OptionalGroupFields}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {

		private Optional<Boolean> isPrivate = Optional.empty();
		private Optional<Boolean> privateMemberList = Optional.empty();
		private final Map<NumberedCustomField, OptionalString> customFields = new HashMap<>();
		
		private Builder() {}
		
		/** Set the privacy state of the group. A null value indicates no change should be made
		 * to the current (or default) value.
		 * @param isPrivate true to make the group private, false for public, null for no change /
		 * default.
		 * @return this builder.
		 */
		public Builder withNullableIsPrivate(final Boolean isPrivate) {
			this.isPrivate = Optional.ofNullable(isPrivate);
			return this;
		}

		/** Set the privacy state of the group member list. A null value indicates no change
		 * should be made to the current (or default) value.
		 * @param privateMemberList true to make the group member listprivate, false for public, null for
		 * no change / default.
		 * @return this builder.
		 */
		public Builder withNullablePrivateMemberList(final Boolean privateMemberList) {
			this.privateMemberList = Optional.ofNullable(privateMemberList);
			return this;
		}
		
		/** Add a custom field to the set of fields.
		 * @param field the field.
		 * @param value the value of the field. An {@link OptionalString#empty()} value indicates
		 * the field should be removed.
		 * @return this builder.
		 */
		public Builder withCustomField(
				final NumberedCustomField field,
				final OptionalString value) {
			customFields.put(requireNonNull(field, "field"), requireNonNull(value, "value"));
			return this;
		}
		
		/** Build the {@link OptionalGroupFields}.
		 * @return the fields.
		 */
		public OptionalGroupFields build() {
			return new OptionalGroupFields(isPrivate, privateMemberList, customFields);
		}
	}
	
}
