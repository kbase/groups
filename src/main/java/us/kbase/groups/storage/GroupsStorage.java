package us.kbase.groups.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupIDAndName;
import us.kbase.groups.core.GroupIDNameMembership;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupUser;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.OptionalString;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.request.RequestType;
import us.kbase.groups.core.resource.ResourceAdministrativeID;
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** A storage interface for the {@link Groups} application.
 * 
 * Warning: for the methods that allow specifing the group modification time, no checking is done
 * to ensure the modification time is after the group creation time. If the modification time
 * is set incorrectly, this may result in groups that cannot be retrieved from the database
 * because they violate the group constraint that mod time > creation time.
 * @author gaprice@lbl.gov
 *
 */
public interface GroupsStorage {
	
	/** Create a new group.
	 * @param group the group.
	 * @throws GroupExistsException if the group already exists.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void createGroup(Group group) throws GroupExistsException, GroupsStorageException;
	
	/** Update a group's fields.
	 * @param update the update to apply.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void updateGroup(GroupUpdateParams update, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException;

	/** Get a group.
	 * @param groupID the ID of the group.
	 * @return the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;
	
	/** Get multiple groups.
	 * @param groupIDs the IDs of the groups.
	 * @return the groups.
	 * @throws NoSuchGroupException if one or more of the groups does not exist.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	Set<Group> getGroups(Collection<GroupID> groupIDs)
			throws NoSuchGroupException, GroupsStorageException;
	
	/** Get the name of one or more groups.
	 * @param user an optional user to determine whether the user is a member of the group.
	 * If no user is provided, group membership is considered to be false.
	 * @param groupID the IDs of the groups.
	 * @return the group names as well as the IDs.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchGroupException if there is no group with one of the given IDs.
	 */
	List<GroupIDNameMembership> getGroupNames(UserName user, Collection<GroupID> groupID)
			throws GroupsStorageException, NoSuchGroupException;
	
	/** Check whether a group exists.
	 * @param groupID the ID of the group.
	 * @return true if the group exists, false otherwise.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	boolean getGroupExists(GroupID groupID) throws GroupsStorageException;

	/** Get the groups the member belongs to, sorted by ID.
	 * @param user the user for whom groups will be returned.
	 * @return the user's groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupIDAndName> getMemberGroups(UserName user) throws GroupsStorageException;
	
	/** Get the IDs of the set of groups the user administrates, including the owner.
	 * @param user the administrator.
	 * @return the group IDs.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	Set<GroupID> getAdministratedGroups(UserName user) throws GroupsStorageException;
	
	/** Get groups in the system, sorted by the group ID.
	 * At most 100 groups are returned.
	 * If the user is null and the user's role is not None no groups are returned.
	 * If the user is null, a resource is present in the parameters, and that resource is private,
	 * no groups are returned.
	 * @param params the parameters for getting the groups.
	 * @param resourceIsPublic true if the resource in the params is public, false otherwise.
	 * Ignored if no resource is present.
	 * @param user an optional user. If no user is provided, only public groups are returned.
	 * @return the groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<Group> getGroups(GetGroupsParams params, boolean resourceIsPublic, UserName user)
			throws GroupsStorageException;
	
	/** Add a member to a group.
	 * @param groupID the ID of the group.
	 * @param member the new member.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws UserIsMemberException if the user is already a member of the group, including
	 * the owner and administrators.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void addMember(GroupID groupID, GroupUser member, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException;
	
	/** Remove a member from a group.
	 * @param groupID the ID of the group.
	 * @param member the member to remove.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws NoSuchUserException if the group does not include the member, not counting
	 * the owner or administrators.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void removeMember(GroupID groupID, UserName member, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException;

	/** Add an administrator to a group. This will remove the user from the member list if present.
	 * @param groupID the ID of the group.
	 * @param admin the new admin.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws UserIsMemberException if the user is the owner or already an administrator
	 * of the group.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchUserException if the user is not already a group member.
	 */
	void addAdmin(GroupID groupID, UserName admin, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException,
				NoSuchUserException;
	
	/** Demote an admin to a member of a group.
	 * @param groupID the ID of the group.
	 * @param member the admin to demote.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws NoSuchUserException if the user is not an admin.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void demoteAdmin(GroupID groupID, UserName member, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException;
	
	/** Update user properties.
	 * @param groupID the ID of the group to update.
	 * @param member the user to update.
	 * @param fields the fields to update. An {@link OptionalString#empty()} value indicates
	 * the field should be removed.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws NoSuchUserException if the user is not a member of the group.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void updateUser(
			GroupID groupID,
			UserName member,
			Map<NumberedCustomField, OptionalString> fields,
			Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException;
	
	/** Update a group member's last visited date for the group. This does not update the
	 * group's modification date.
	 * @param groupID the ID of the group the member visited.
	 * @param member the member's user name.
	 * @param lastVisited the date the user visited.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws NoSuchUserException if the user is not a member of the group.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void updateUser(GroupID groupID, UserName member, Instant lastVisited)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException;
	
	/** Add a resource to a group.
	 * @param groupID the group ID.
	 * @param type the resource type.
	 * @param resource the resource descriptor.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws ResourceExistsException if the resource already exists in the group.
	 */
	void addResource(
			GroupID groupID,
			ResourceType type,
			ResourceDescriptor resource,
			Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, ResourceExistsException;
	
	/** Remove a resource from a group.
	 * @param groupID the group ID.
	 * @param type the resource type.
	 * @param resource the resource ID.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchResourceException if the group does not contain the resource.
	 */
	void removeResource(
			GroupID groupID,
			ResourceType type,
			ResourceID resource,
			Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, NoSuchResourceException;
	
	/** Store a new request. The request ID must not already be present in the system.
	 * @param request the new request.
	 * @throws IllegalArgumentException if the request ID already exists.
	 * @throws RequestExistsException if an effectively identical request (the same requester,
	 * group, target, and type) already exists in an {@link GroupRequestStatusType#OPEN} state
	 * in the system.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void storeRequest(GroupRequest request)
			throws RequestExistsException, GroupsStorageException;
	
	/** Get a request.
	 * @param requestID the ID of the request.
	 * @return the request.
	 * @throws NoSuchRequestException if there is no request with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	GroupRequest getRequest(RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException;
	
	/** Get the open requests created by a user, sorted by the modification time of the request.
	 * At most 100 requests are returned.
	 * @param requester the user that created the requests.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByRequester(UserName requester, GetRequestsParams params)
			throws GroupsStorageException;
	
	/** Get the open requests that target a user or the resources a user administrates,
	 * sorted by the modification time of the request.
	 * At most 100 requests are returned.
	 * @param target the targeted user.
	 * @param resources the resources that user administrates.
	 * @param params the parameters for getting the requests. A particular resource may not
	 * be specified in the parameters.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByTarget(
			UserName target,
			Map<ResourceType, Set<ResourceAdministrativeID>> resources,
			GetRequestsParams params)
			throws GroupsStorageException;
	
	/** Get the open requests that target a specific resource (e.g. the request type is always
	 * {@link RequestType#INVITE}) sorted by the modification time of the request.
	 * At most 100 requests are returned.
	 * @param params the parameters for getting the requests. A particular resource must
	 * be specified in the parameters.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByTarget(GetRequestsParams params)
			throws GroupsStorageException;

	/** Get the open requests that target a group, sorted by the modification time of the request.
	 * At most 100 requests are returned.
	 * Requests that target a group are of type {@link RequestType#REQUEST}.
	 * @param groupID the targeted group.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByGroup(GroupID groupID, GetRequestsParams params)
			throws GroupsStorageException;
	
	/** Get the open requests that target a set of groups, sorted by the modification time of the
	 * request.
	 * At most 100 requests are returned.
	 * Requests that target a group are of type {@link RequestType#REQUEST}.
	 * @param groupIDs the targeted groups.
	 * @param params the parameters for getting the requests.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByGroups(Set<GroupID> groupIDs, GetRequestsParams params)
			throws GroupsStorageException;
	
	/** Check if a group has at least one open incoming
	 * (e.g. are {@link RequestType#REQUEST}s) request later than a specified date.
	 * @param groupID the ID of the group to check.
	 * @param laterThan if all open requests were created on a date earlier or equal to  this,
	 * false is returned. If null, returns true if any open requests exist.
	 * @return true if at least one open request exists, false otherwise.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	boolean groupHasRequest(GroupID groupID, Instant laterThan) throws GroupsStorageException;
	
	/** Close a request. WARNING: this function will allow setting the modification time to
	 * an earlier date than the creation time of the request, which will cause indeterminate
	 * behavior. Don't do this.
	 * @param requestID the ID of the request to close.
	 * @param status the status to apply to the request. Must not be
	 * {@link GroupRequestStatus#open()}.
	 * @param modificationTime the modification time of the request.
	 * @throws NoSuchRequestException if there is no open request with the given ID
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void closeRequest(
			RequestID requestID,
			GroupRequestStatus status,
			Instant modificationTime)
			throws NoSuchRequestException, GroupsStorageException;
}
