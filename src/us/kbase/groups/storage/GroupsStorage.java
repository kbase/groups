package us.kbase.groups.storage;

import java.time.Instant;
import java.util.List;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.exceptions.WorkspaceExistsException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.workspace.WorkspaceID;
import us.kbase.groups.core.workspace.WorkspaceIDSet;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

/** A storage interface for the {@link Groups} application.
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

	/** Get a group.
	 * @param groupID the ID of the group.
	 * @return the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;
	
	// assumes not that many groups. If it turns out we make a lot of groups (probably > ~100k)
	// something will have to change.
	// ordered by group ID
	/** Get all the groups in the system, sorted by the group ID.
	 * @return the groups.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<Group> getGroups() throws GroupsStorageException;
	
	/** Add a member to a group.
	 * @param groupID the ID of the group.
	 * @param member the new member.
	 * @param modDate the modification date to apply to the group.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws UserIsMemberException if the user is already a member of the group, including
	 * the owner and administrators.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void addMember(GroupID groupID, UserName member, Instant modDate)
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
	 */
	void addAdmin(GroupID groupID, UserName admin, Instant modDate)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException;
	
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
	
	/** Add a workspace to a group.
	 * @param groupID the group ID.
	 * @param wsid the workspace ID.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws WorkspaceExistsException if the workspace already exists in the group.
	 */
	void addWorkspace(GroupID groupID, WorkspaceID wsid)
			throws NoSuchGroupException, GroupsStorageException, WorkspaceExistsException;
	
	/** Remove a workspace from a group.
	 * @param groupID the group ID.
	 * @param wsid the workspace ID.
	 * @throws NoSuchGroupException if there is no group with the given ID.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 * @throws NoSuchWorkspaceException if the group does not contain the workspace.
	 */
	void removeWorkspace(GroupID groupID, WorkspaceID wsid)
			throws NoSuchGroupException, GroupsStorageException, NoSuchWorkspaceException;
	
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
	
	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	//TODO NOW allow getting closed requests
	/** Get the open requests created by a user, sorted by the modification time of the request.
	 * @param requester the user that created the requests.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByRequester(
			UserName requester) throws GroupsStorageException;
	
	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	//TODO NOW allow getting closed requests
	/** Get the open requests that target a user or the workspaces a user administrates,
	 * sorted by the modification time of the request.
	 * @param target the targeted user.
	 * @param wsids the targeted workspaces for that user.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByTarget(
			UserName target, WorkspaceIDSet wsids) throws GroupsStorageException;

	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	// only returns requests for that group specifically, e.g. no target user
	//TODO NOW allow getting closed requests
	/** Get the open requests that target a group, sorted by the modification time of the request.
	 * @param groupID the targeted group.
	 * @return the requests.
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	List<GroupRequest> getRequestsByGroup(GroupID groupID)
			throws GroupsStorageException;
	
	/** Close a request. WARNING: this function will allow setting the modification time to
	 * an earlier date than the creation time of the request, which will cause indeterminate
	 * behavior. Don't do this.
	 * @param requestID the ID of the request to close.
	 * @param status the status to apply to the request. Must not be
	 * {@link GroupRequestStatus#open()}.
	 * @param modificationTime the modfication time of the request.
	 * @throws NoSuchRequestException if there is no open request with the given ID
	 * @throws GroupsStorageException if an error occurs contacting the storage system.
	 */
	void closeRequest(
			RequestID requestID,
			GroupRequestStatus status,
			Instant modificationTime)
			throws NoSuchRequestException, GroupsStorageException;
}
