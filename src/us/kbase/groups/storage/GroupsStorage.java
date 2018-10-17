package us.kbase.groups.storage;

import java.util.List;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

public interface GroupsStorage {
	
	//TODO JAVADOC

	void createGroup(final Group group) throws GroupExistsException, GroupsStorageException;

	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;
	
	// assumes not that many groups. If it turns out we make a lot of groups (probably > ~100k)
	// something will have to change.
	// ordered by group ID
	List<Group> getGroups() throws GroupsStorageException;

	
}
