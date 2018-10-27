package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

public interface WorkspaceHandler {

	//TODO NOW JAVADOC

	boolean isAdministrator(WorkspaceID wsid, UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException;

	
}
