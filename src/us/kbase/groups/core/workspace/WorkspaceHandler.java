package us.kbase.groups.core.workspace;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

public interface WorkspaceHandler {

	//TODO NOW JAVADOC

	boolean isAdministrator(WorkspaceID wsid, UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException;

	// missing or deleted ws ids are not included in the returned set.
	WorkspaceInfoSet getWorkspaceInformation(WorkspaceIDSet ids, UserName user)
			throws WorkspaceHandlerException;

	// missing or deleted ws ids are not included in the returned set.
	// always returns public workspaces regardless of bool
	WorkspaceInfoSet getWorkspaceInformation(
			WorkspaceIDSet ids,
			UserName user,
			boolean administratedWorkspacesOnly)
			throws WorkspaceHandlerException;

}
