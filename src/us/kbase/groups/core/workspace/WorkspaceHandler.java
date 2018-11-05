package us.kbase.groups.core.workspace;

import java.util.Set;

import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.NoSuchWorkspaceException;
import us.kbase.groups.core.exceptions.WorkspaceHandlerException;

/** Handles requests for information from the KBase Workspace service.
 * @author gaprice@lbl.gov
 *
 */
public interface WorkspaceHandler {

	/** Determine whether a user is an administrator of a workspace.
	 * @param wsid the ID of the workspace.
	 * @param user the user.
	 * @return true if the user is a workspace admin, false otherwise.
	 * @throws WorkspaceHandlerException if an error occurs contacting the workspace.
	 * @throws NoSuchWorkspaceException if the workspace is deleted or there is no workspace
	 * with the given ID.
	 */
	boolean isAdministrator(WorkspaceID wsid, UserName user)
			throws WorkspaceHandlerException, NoSuchWorkspaceException;

	/** Get information about a set of workspaces.
	 * @param user the user requesting information. If null, indicating an anonymous user,
	 * only public workspaces are returned.
	 * @param ids the workspace IDs to get information about.
	 * @param administratedWorkspacesOnly if true, only return public workspaces and workspaces
	 * the user administrates. If false, return all workspaces (unless the user is null).
	 * @return the information about the workspaces.
	 * @throws WorkspaceHandlerException if an error occurs contacting the workspace.
	 */
	WorkspaceInfoSet getWorkspaceInformation(
			UserName user,
			WorkspaceIDSet ids,
			boolean administratedWorkspacesOnly)
			throws WorkspaceHandlerException;

	/** Get the IDs of the workspaces a user administrates.
	 * @param user the user.
	 * @return the IDs.
	 * @throws WorkspaceHandlerException if an error occurs contacting the workspace.
	 */
	WorkspaceIDSet getAdministratedWorkspaces(UserName user)
		throws WorkspaceHandlerException;

	/** Get the set of users that administrate a workspace.
	 * @param wsid the workspace to query.
	 * @return the set of administrators.
	 * @throws WorkspaceHandlerException if an error occurs contacting the workspace.
	 * @throws NoSuchWorkspaceException if the workspace is deleted or there is no workspace
	 * with the given ID.
	 */
	Set<UserName> getAdministrators(WorkspaceID wsid)
			throws NoSuchWorkspaceException, WorkspaceHandlerException;
	
	/** Give a user read permissions if the workspace is not already readable (including
	 * publicly) to them.
	 * @param wsid the workspace to modify.
	 * @param user the user to grant read permissions.
	 * @throws WorkspaceHandlerException if an error occurs contacting the workspace.
	 * @throws NoSuchWorkspaceException if the workspace is deleted or there is no workspace
	 * with the given ID.
	 */
	void setReadPermission(WorkspaceID wsid, UserName user)
			throws NoSuchWorkspaceException, WorkspaceHandlerException;
	
}
