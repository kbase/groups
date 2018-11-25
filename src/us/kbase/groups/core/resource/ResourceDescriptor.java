package us.kbase.groups.core.resource;

import static com.google.common.base.Preconditions.checkNotNull;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The resource ID and corresponding administrative ID for a resource.
 * @author gaprice@lbl.gov
 *
 */
public class ResourceDescriptor {

	private final ResourceAdministrativeID administrativeID;
	private final ResourceID resourceID;
	
	/** Create the descriptor.
	 * @param administrativeID the administrative ID of a resource.
	 * @param resourceID the ID of a resource.
	 */
	public ResourceDescriptor(
			final ResourceAdministrativeID administrativeID,
			final ResourceID resourceID) {
		checkNotNull(administrativeID, "administrativeID");
		checkNotNull(resourceID, "resourceID");
		this.administrativeID = administrativeID;
		this.resourceID = resourceID;
	}
	
	/** Create a descriptor with identical administrative and resource IDs.
	 * @param resourceID the resource ID. The administrative ID for this resource is identical.
	 */
	public ResourceDescriptor(final ResourceID resourceID) {
		checkNotNull(resourceID, "resourceID");
		try {
			this.administrativeID = new ResourceAdministrativeID(resourceID.getName());
		} catch (MissingParameterException | IllegalParameterException e) {
			// since resID and resadminID have the same requirements
			throw new RuntimeException("This should be impossible", e);
		}
		this.resourceID = resourceID;
	}

	/** Get the administrative ID of the resource.
	 * @return the administrative ID.
	 */
	public ResourceAdministrativeID getAdministrativeID() {
		return administrativeID;
	}

	/** Get the resource ID.
	 * @return the ID.
	 */
	public ResourceID getResourceID() {
		return resourceID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((administrativeID == null) ? 0 : administrativeID.hashCode());
		result = prime * result + ((resourceID == null) ? 0 : resourceID.hashCode());
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
		ResourceDescriptor other = (ResourceDescriptor) obj;
		if (administrativeID == null) {
			if (other.administrativeID != null) {
				return false;
			}
		} else if (!administrativeID.equals(other.administrativeID)) {
			return false;
		}
		if (resourceID == null) {
			if (other.resourceID != null) {
				return false;
			}
		} else if (!resourceID.equals(other.resourceID)) {
			return false;
		}
		return true;
	}
}
