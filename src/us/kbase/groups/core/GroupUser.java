package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import us.kbase.groups.core.fieldvalidation.NumberedCustomField;

/** A user of the group system.
 * @author gaprice@lbl.gov
 *
 */
public class GroupUser {

	private final UserName name;
	private final Instant joinDate;
	private final Map<NumberedCustomField, String> customFields;
	
	private GroupUser(
			final UserName name,
			final Instant joinDate,
			final Map<NumberedCustomField, String> customFields) {
		this.name = name;
		this.joinDate = joinDate;
		this.customFields = Collections.unmodifiableMap(customFields);
	}

	/** The user's name.
	 * @return the user's name.
	 */
	public UserName getName() {
		return name;
	}

	/** The date the user joined the group.
	 * @return the join date.
	 */
	public Instant getJoinDate() {
		return joinDate;
	}

	/** Get any custom fields associated with the user.
	 * @return the custom fields.
	 */
	public Map<NumberedCustomField, String> getCustomFields() {
		return customFields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customFields == null) ? 0 : customFields.hashCode());
		result = prime * result + ((joinDate == null) ? 0 : joinDate.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		GroupUser other = (GroupUser) obj;
		if (customFields == null) {
			if (other.customFields != null) {
				return false;
			}
		} else if (!customFields.equals(other.customFields)) {
			return false;
		}
		if (joinDate == null) {
			if (other.joinDate != null) {
				return false;
			}
		} else if (!joinDate.equals(other.joinDate)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link GroupUser}.
	 * @param name the user's name.
	 * @param joinDate the user's join date.
	 * @return the builder.
	 */
	public static Builder getBuilder(final UserName name, final Instant joinDate) {
		return new Builder(name, joinDate);
	}
	
	/** A builder for a {@link GroupUser}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final UserName name;
		private final Instant joinDate;
		private final Map<NumberedCustomField, String> customFields = new HashMap<>();
		
		private Builder(final UserName name, final Instant joinDate) {
			this.name = requireNonNull(name, "name");
			this.joinDate = requireNonNull(joinDate, "joinDate");
		}
		
		/** Add a custom field to the user.
		 * @param field the field key.
		 * @param value the field value.
		 * @return this builder.
		 */
		public Builder withCustomField(final NumberedCustomField field, final String value) {
			requireNonNull(field, "field");
			exceptOnEmpty(value, "value");
			// TODO CODE limit on value size?
			customFields.put(field, value);
			return this;
		}
		
		/** Build the user.
		 * @return the user.
		 */
		public GroupUser build() {
			return new GroupUser(name, joinDate, customFields);
		}
	}
	
}
