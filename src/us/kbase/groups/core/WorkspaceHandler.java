package us.kbase.groups.core;

import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

public interface WorkspaceHandler {

	//TODO NOW JAVADOC

	boolean isAdmin(WorkspaceID wsid, UserName user) throws WorkspaceHandlerException;

	
}
