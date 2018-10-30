package us.kbase.groups.core.workspace;

import static us.kbase.groups.util.Util.isNullOrEmpty;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import com.google.common.base.Optional;

/** Information about a workspace. Includes a subset of the data returned from the 
 * get_workspace_info or list_workspace_info methods.
 * 
 * Assumes that the client creating this class is pulling the input data from the workspace, and
 * so only does basic checking of the inputs - e.g. the workspace name is not checked to ensure
 * it is a valid workspace name.
 * @author gaprice@lbl.gov
 *
 */
public class WorkspaceInformation {
	
	// add more fields when actually asked for them. YAGNI baby
	private final int id;
	private final String name;
	private final Optional<String> narrativeName;
	private final boolean isPublic;
	
	private WorkspaceInformation(
			final int id,
			final String name,
			final Optional<String> narrativeName,
			final boolean isPublic) {
		this.id = id;
		this.name = name;
		this.narrativeName = narrativeName;
		this.isPublic = isPublic;
	}

	/** Get the workspace ID.
	 * @return the ID.
	 */
	public int getID() {
		return id;
	}

	/** Get the workspace name.
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	/** Get the name of the narrative in the workspace, if any.
	 * @return the narrative name, or {@link Optional#absent()}.
	 */
	public Optional<String> getNarrativeName() {
		return narrativeName;
	}
	
	/** Get whether the workspace is publicly readable or not.
	 * @return true if the workspace is public.
	 */
	public boolean isPublic() {
		return isPublic;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (isPublic ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((narrativeName == null) ? 0 : narrativeName.hashCode());
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
		WorkspaceInformation other = (WorkspaceInformation) obj;
		if (id != other.id) {
			return false;
		}
		if (isPublic != other.isPublic) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (narrativeName == null) {
			if (other.narrativeName != null) {
				return false;
			}
		} else if (!narrativeName.equals(other.narrativeName)) {
			return false;
		}
		return true;
	}
	
	/** Get a builder for a {@link WorkspaceInformation}.
	 * @param wsid the ID of the workspace.
	 * @param name the name of the workspace.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final int wsid, final String name) {
		return new Builder(wsid, name);
	}
	
	/** A builder for a {@link WorkspaceInformation}.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private final int id;
		private final String name;
		private Optional<String> narrativeName = Optional.absent();
		private boolean isPublic = false;

		private Builder(final int wsid, final String name) {
			if (wsid < 1) {
				throw new IllegalArgumentException("workspace IDs must be > 0");
			}
			exceptOnEmpty(name, "name");
			this.id = wsid;
			this.name = name;
		}
		
		/** Add a narrative name to the workspace information.
		 * @param narrativeName the narrative name. If null or whitespace only, the narrative
		 * name will be removed.
		 * @return this builder.
		 */
		public Builder withNullableNarrativeName(final String narrativeName) {
			if (isNullOrEmpty(narrativeName)) {
				this.narrativeName = Optional.absent();
			} else {
				this.narrativeName = Optional.of(narrativeName);
			}
			return this;
		}
		
		/** Set whether the workspace is public.
		 * @param isPublic true to set as public, false for private.
		 * @return this builder.
		 */
		public Builder withIsPublic(final boolean isPublic) {
			this.isPublic = isPublic;
			return this;
		}
		
		/** Build the {@link WorkspaceInformation}.
		 * @return the information.
		 */
		public WorkspaceInformation build() {
			return new WorkspaceInformation(id, name, narrativeName, isPublic);
		}
	}

}
