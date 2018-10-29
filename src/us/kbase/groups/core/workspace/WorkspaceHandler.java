package us.kbase.groups.core.workspace;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

public interface WorkspaceHandler {

	//TODO NOW JAVADOC

	boolean isAdministrator(WorkspaceID wsid, UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException;

	// always returns public workspaces regardless of bool
	// pass null for user if anon user. In this case only public workspaces will be returned
	WorkspaceInfoSet getWorkspaceInformation(
			UserName user,
			WorkspaceIDSet ids,
			boolean administratedWorkspacesOnly)
			throws WorkspaceHandlerException;

}
