package us.kbase.groups.storage;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

public interface GroupsStorage {
	
	//TODO JAVADOC

	void createGroup(final Group group) throws GroupExistsException, GroupsStorageException;

	Group getGroup(GroupID groupID) throws GroupsStorageException, NoSuchGroupException;

	
}
