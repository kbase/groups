package us.kbase.groups.core.resource;

import java.util.Set;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;

/** A handler for a resource. Provides information about the resources in the handler's domain
 * and allows granting read permissions to a resource for a user.
 * @author gaprice@lbl.gov
 *
 */
public interface ResourceHandler {
	
	/** Get a full descriptor for a resource. This method is not guaranteed to check that the
	 * resource actually exists.
	 * @param resource the resource.
	 * @return the descriptor.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	ResourceDescriptor getDescriptor(ResourceID resource)
			throws IllegalResourceIDException, ResourceHandlerException, NoSuchResourceException;
	
	/** Check if a user is an administrator for a resource.
	 * @param resource the resource to check.
	 * @param user the user.
	 * @return true if the user is a resource administrator, false otherwise.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	boolean isAdministrator(ResourceID resource, UserName user)
			throws IllegalResourceIDException, ResourceHandlerException, NoSuchResourceException;

	/** Check if a resource is public. The resource handler implementation may or may not
	 * check for the existence of the resource if all resources are public.
	 * @param resource the resource to check.
	 * @return true if the resource is public, false otherwise.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	boolean isPublic(ResourceID resource)
			throws IllegalResourceIDException, ResourceHandlerException, NoSuchResourceException;
	
	/** Get the set of administrators of a resource.
	 * @param resource the resource.
	 * @return the set of administrators of that resource.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	Set<UserName> getAdministrators(ResourceID resource)
			throws IllegalResourceIDException, NoSuchResourceException, ResourceHandlerException;

	/** Get the set of resource administrative IDs for which the user is an administrator.
	 * @param user the user.
	 * @return the IDs of the resources the user administrates.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 */
	Set<ResourceAdministrativeID> getAdministratedResources(UserName user)
		throws ResourceHandlerException;

	/** Get information about a set of resources.
	 * This method is not guaranteed to check if the resources exist.
	 * @param user the user at which any user-specific fields should be targeted. If null,
	 * indicating an anonymous user, only public resources are returned.
	 * @param resources the resources to retrieve.
	 * @param access the level of access to the resources the user possesses. Note that
	 * an anonymous user will, in most cases, administrate no resources.
	 * @return the resource information.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 */
	ResourceInformationSet getResourceInformation(
			UserName user,
			Set<ResourceID> resources,
			ResourceAccess access)
			throws IllegalResourceIDException, ResourceHandlerException;

	/** Grant a user permission to view or read a resource, if the user does not already have
	 * permissions (including public permissions) for the resource.
	 * 
	 * The implementation of this method may be a no-op when resources are all public.
	 * @param resource the resource for which permissions will be added.
	 * @param user the user.
	 * @throws IllegalResourceIDException if the resource ID is not in a legal format.
	 * @throws ResourceHandlerException if an error occurs contacting the resource service.
	 * @throws NoSuchResourceException if there is no such resource.
	 */
	void setReadPermission(ResourceID resource, UserName user)
			throws IllegalResourceIDException, NoSuchResourceException, ResourceHandlerException;
}
