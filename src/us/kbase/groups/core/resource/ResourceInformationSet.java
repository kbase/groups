package us.kbase.groups.core.resource;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import us.kbase.groups.core.UserName;

/** A set of information about various resources. Resource from separate sources should not
 * be mixed in one set.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceInformationSet {
	
	private final Optional<UserName> user;
	// might need to think about sorting here. YAGNI for now.
	//TODO NNOW consider just resourceID. Is RD necessary?
	private final Map<ResourceDescriptor, Map<String, Object>> resources;
	private final Set<ResourceDescriptor> nonexistent;
	
	private ResourceInformationSet(
			final Optional<UserName> user,
			final Map<ResourceDescriptor, Map<String, Object>> perms,
			final Set<ResourceDescriptor> nonexistent) {
		this.user = user;
		this.resources = Collections.unmodifiableMap(perms);
		this.nonexistent = Collections.unmodifiableSet(nonexistent);
	}
	
	/** Get the user associated with the set. Any user-specific fields returned by
	 * {@link #getFields(ResourceDescriptor)} refers to this user.
	 * @return the user, or {@link Optional#empty()} if the user is anonymous.
	 */
	public Optional<UserName> getUser() {
		return user;
	}
	
	/** Get the resources in this set.
	 * @return the workspace information.
	 */
	public Set<ResourceDescriptor> getResources() {
		return resources.keySet();
	}
	
	/** Get any fields associated with a resource. If any of the fields are user-specific, they
	 * are for the user from {@link #getUser()}.
	 * @param resource the resource to query.
	 * @return the fields.
	 */
	public Map<String, Object> getFields(final ResourceDescriptor resource) {
		checkNotNull(resource, "resource");
		if (!resources.containsKey(resource)) {
			throw new IllegalArgumentException("Provided resource not included in set");
		} else {
			return Collections.unmodifiableMap(resources.get(resource));
		}
	}
	
	/** Get any resources that were found to be deleted or missing altogether when building
	 * this set.
	 * @return the resources.
	 */
	public Set<ResourceDescriptor> getNonexistentResources() {
		return nonexistent;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resources == null) ? 0 : resources.hashCode());
		result = prime * result + ((nonexistent == null) ? 0 : nonexistent.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		ResourceInformationSet other = (ResourceInformationSet) obj;
		if (resources == null) {
			if (other.resources != null) {
				return false;
			}
		} else if (!resources.equals(other.resources)) {
			return false;
		}
		if (nonexistent == null) {
			if (other.nonexistent != null) {
				return false;
			}
		} else if (!nonexistent.equals(other.nonexistent)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}

	/** Get a builder for a {@link ResourceInformationSet}.
	 * @param user the user associated with this set. The field information
	 * passed to {@link Builder#withResourceField(ResourceDescriptor, String, Object)} must
	 * be based on this user. Pass null for an anonymous user.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final UserName user) {
		return new Builder(user);
	}
	
	/** A builder for {@link ResourceInformationSet}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {

		private final Optional<UserName> user;
		// might need to think about sorting here. YAGNI for now.
		private final Map<ResourceDescriptor, Map<String, Object>> resources = new HashMap<>();
		private final Set<ResourceDescriptor> nonexistent = new HashSet<>();
		
		private Builder(final UserName user) {
			this.user = Optional.ofNullable(user);
		}
		
		/** Add a resource to the builder without any fields.
		 * @param resource the resource.
		 * @return this builder.
		 */
		public Builder withResourceDescriptor(final ResourceDescriptor resource) {
			checkNotNull(resource, "resource");
			if (!resources.containsKey(resource)) {
				resources.put(resource, new HashMap<>());
			}
			return this;
		}
		
		/** Add a field to a resource. If the resource does not already exist in the builder,
		 * it will be added. Fields may not be null or whitespace only.
		 * @param resource the resource.
		 * @param field the field.
		 * @param value the field's value.
		 * @return this builder.
		 */
		public Builder withResourceField(
				final ResourceDescriptor resource,
				final String field,
				final Object value) {
			checkNotNull(resource, "resource");
			exceptOnEmpty(field, "field");
			withResourceDescriptor(resource);
			resources.get(resource).put(field, value);
			return this;
		}
		
		/** Add a resource that was found to be missing or deleted altogether when building
		 * this set.
		 * @param resource the deleted or missing resource.
		 * @return this builder.
		 */
		public Builder withNonexistentResource(final ResourceDescriptor resource) {
			checkNotNull(resource, "resource");
			nonexistent.add(resource);
			return this;
		}
		
		/** Build the {@link ResourceInformationSet}.
		 * @return the new set.
		 */
		public ResourceInformationSet build() {
			return new ResourceInformationSet(user, resources, nonexistent);
		}
	}
}
