package us.kbase.groups.core.workspace;

import us.kbase.groups.core.exceptions.IllegalParameterException;

/** The ID of a KBase workspace.
 *
 * @author gaprice@lbl.gov
 *
 */
public class WorkspaceID {
	
	/* Workspace IDs are represented as longs in the workspace service, but the chance we'll 
	 * ever get to 2 billion workspaces is zero unless someone starts abusing the system.
	 */
	
	//TODO TEST
	
	private final int id;
	
	/** Construct the ID.
	 * @param id the id.
	 * @throws IllegalParameterException if the ID is < 1.
	 */
	public WorkspaceID(final int id) throws IllegalParameterException {
		if (id < 1) {
			throw new IllegalParameterException("Workspace IDs are > 0");
		}
		this.id = id;
	}

	/** Get the ID.
	 * @return the ID.
	 */
	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WorkspaceID other = (WorkspaceID) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
	
	

}
