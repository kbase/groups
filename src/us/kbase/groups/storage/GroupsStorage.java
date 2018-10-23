package us.kbase.groups.storage;

import java.time.Instant;
import java.util.List;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestStatus;
import us.kbase.groups.core.request.GroupRequestStatusType;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

public interface GroupsStorage {
	
	//TODO JAVADOC

	void createGroup(Group group) throws GroupExistsException, GroupsStorageException;

	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;
	
	// assumes not that many groups. If it turns out we make a lot of groups (probably > ~100k)
	// something will have to change.
	// ordered by group ID
	List<Group> getGroups() throws GroupsStorageException;
	
	void addMember(GroupID groupID, UserName member)
			throws NoSuchGroupException, GroupsStorageException, UserIsMemberException;
	
	void removeMember(GroupID groupID, UserName member)
			throws NoSuchGroupException, GroupsStorageException, NoSuchUserException;

	void storeRequest(GroupRequest request)
			throws RequestExistsException, GroupsStorageException;
	
	GroupRequest getRequest(RequestID requestID)
			throws NoSuchRequestException, GroupsStorageException;
	
	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	// pass null for any status
	List<GroupRequest> getRequestsByRequester(
			UserName requester,
			GroupRequestStatusType status) throws GroupsStorageException;
	
	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	// pass null for any status
	List<GroupRequest> getRequestsByTarget(
			UserName target,
			GroupRequestStatusType status) throws GroupsStorageException;

	//TODO NOW need date range, limit & sort by date up /down if there's a lot
	// maybe also specify optional target user? YAGNI for now
	// only returns requests for that group specifically, e.g. no target user
	// pass null for any status
	List<GroupRequest> getRequestsByGroupID(GroupID groupID, GroupRequestStatusType status)
			throws GroupsStorageException;
	
	void closeRequest(
			RequestID requestID,
			GroupRequestStatus status,
			Instant modificationTime)
			throws NoSuchRequestException, GroupsStorageException;
}
