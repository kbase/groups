package us.kbase.groups.core.workspace;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

public interface WorkspaceHandler {

	//TODO NOW JAVADOC

	boolean isAdministrator(WorkspaceID wsid, UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException;

	
}
