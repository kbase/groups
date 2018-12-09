package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;

/** Optional fields associated with a {@link Group}. 
 * @author gaprice@lbl.gov
 *
 */
public class OptionalGroupFields {
	
	private final FieldItem<String> description;
	private final Map<NumberedCustomField, OptionalString> customFields;

	private OptionalGroupFields(
			final FieldItem<String> description,
			final Map<NumberedCustomField, OptionalString> customFields) {
		this.description = description;
		this.customFields = Collections.unmodifiableMap(customFields);
	}

	/** Get the description of the group.
	 * @return the description.
	 */
	public FieldItem<String> getDescription() {
		return description;
	}
	
	/** Returns true if at least one field require an update.
	 * @return if a field requires an update.
	 */
	public boolean hasUpdate() {
		return description.hasAction() || !customFields.isEmpty();
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
		checkNotNull(field, "field");
		if (!customFields.containsKey(field)) {
			throw new IllegalArgumentException("No such field " + field.getField());
		}
		return customFields.get(field);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
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
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
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
	
	/** Get the default set of {@link OptionalGroupFields}, where all of the fields are set to
	 * {@link FieldItem#noAction()}.
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

		private FieldItem<String> description = FieldItem.noAction();
		private final Map<NumberedCustomField, OptionalString> customFields = new HashMap<>();
		
		private Builder() {}
		
		/** Add a group description. The maximum description size is
		 * {@link Group#MAX_DESCRIPTION_CODE_POINTS} Unicode code points.
		 * @param description the new description.
		 * @return this builder.
		 * @throws IllegalParameterException if the description is too long.
		 */
		public Builder withDescription(final StringField description)
				throws IllegalParameterException {
			checkNotNull(description, "description");
			if (description.hasItem()) {
				try {
					checkString(description.get(), "description",
							Group.MAX_DESCRIPTION_CODE_POINTS);
				} catch (MissingParameterException e) {
					throw new RuntimeException("This should be impossible");
				}
			}
			this.description = description;
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
			checkNotNull(field, "field");
			checkNotNull(value, "value");
			customFields.put(field, value);
			return this;
		}
		
		/** Build the {@link OptionalGroupFields}.
		 * @return the fields.
		 */
		public OptionalGroupFields build() {
			return new OptionalGroupFields(description, customFields);
		}
	}
	
}
