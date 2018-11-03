package us.kbase.groups.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** Optional fields associated with a {@link Group}. 
 * @author gaprice@lbl.gov
 *
 */
public class OptionalGroupFields {
	
	private final FieldItem<String> description;

	private OptionalGroupFields(FieldItem<String> description) {
		this.description = description;
	}

	/** Get the description of the group.
	 * @return the description.
	 */
	public FieldItem<String> getDescription() {
		return description;
	}
	
	/** True if for any of the items {@link FieldItem#hasItem()} or {@link FieldItem#isRemove()}
	 * is true, false otherwise.
	 * @return if a field requires an update.
	 */
	public boolean hasUpdate() {
		return description.hasAction();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		
		/** Build the {@link OptionalGroupFields}.
		 * @return the fields.
		 */
		public OptionalGroupFields build() {
			return new OptionalGroupFields(description);
		}
	}
	
}
