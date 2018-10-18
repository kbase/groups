package us.kbase.groups.storage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

public interface GroupsStorage {
	
	//TODO JAVADOC

	void createGroup(final Group group) throws GroupExistsException, GroupsStorageException;

	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;
	
	// assumes not that many groups. If it turns out we make a lot of groups (probably > ~100k)
	// something will have to change.
	// ordered by group ID
	List<Group> getGroups() throws GroupsStorageException;

	void storeRequest(GroupRequest request)
			throws RequestExistsException, GroupsStorageException;
	
	GroupRequest getRequest(UUID requestID)
			throws NoSuchRequestException, GroupsStorageException;
	
	// pass null for any status
	Set<GroupRequest> getRequestsByRequester(
			final UserName requester,
			final GroupRequestStatus status) throws GroupsStorageException;
	
	// pass null for any status
	Set<GroupRequest> getRequestsByTarget(
			final UserName target,
			final GroupRequestStatus status) throws GroupsStorageException;
}
