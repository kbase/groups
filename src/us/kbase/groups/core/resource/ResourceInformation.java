package us.kbase.groups.core.resource;

import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Information about an a resource.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceInformation {
	
	private final ResourceID resourceID;
	private final ResourceType resourceType;
	private final Map<String, Object> resourceFields;
	
	private ResourceInformation(
			final ResourceID resourceID,
			final ResourceType resourceType,
			final Map<String, Object> resourceFields) {
		this.resourceID = resourceID;
		this.resourceType = resourceType;
		this.resourceFields = Collections.unmodifiableMap(resourceFields);
	}

	/** The ID of the resource.
	 * @return the resource ID.
	 */
	public ResourceID getResourceID() {
		return resourceID;
	}

	/** The type of the resource.
	 * @return the resource type.
	 */
	public ResourceType getResourceType() {
		return resourceType;
	}

	/** Fields associated with the resource.
	 * The keys are guaranteed to be non-null and non-whitespace only.
	 * The values are unconstrained.
	 * @return the fields.
	 */
	public Map<String, Object> getResourceFields() {
		return resourceFields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resourceFields == null) ? 0 : resourceFields.hashCode());
		result = prime * result + ((resourceID == null) ? 0 : resourceID.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
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
		ResourceInformation other = (ResourceInformation) obj;
		if (resourceFields == null) {
			if (other.resourceFields != null) {
				return false;
			}
		} else if (!resourceFields.equals(other.resourceFields)) {
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
		return true;
	}
	
	/** Get a builder for a {@link ResourceInformation}.
	 * @param type the type of the resource.
	 * @param id the resource ID.
	 * @return the new builder.
	 */
	public static Builder getBuilder(final ResourceType type, final ResourceID id) {
		return new Builder(type, id);
	}
	
	/** A builder for a {@link ResourceInformation}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final ResourceID resourceID;
		private final ResourceType resourceType;
		private final Map<String, Object> resourceFields = new HashMap<>();

		private Builder(final ResourceType type, final ResourceID id) {
			this.resourceType = requireNonNull(type, "type");
			this.resourceID = requireNonNull(id, "id");
		}
		
		/** Add a field to the builder.
		 * @param field the field name. Cannot be null or whitespace-only.
		 * @param value the value of the field.
		 * @return this builder.
		 */
		public Builder withField(final String field, final Object value) {
			exceptOnEmpty(field, "field");
			resourceFields.put(field, value);
			return this;
		}
		
		/** Build the new {@link ResourceInformation}.
		 * @return the information.
		 */
		public ResourceInformation build() {
			return new ResourceInformation(resourceID, resourceType, resourceFields);
		}
	}

}
