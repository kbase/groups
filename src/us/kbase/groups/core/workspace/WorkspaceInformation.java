package us.kbase.groups.core.workspace;

import static us.kbase.groups.util.Util.isNullOrEmpty;
import static us.kbase.groups.util.Util.exceptOnEmpty;

import com.google.common.base.Optional;

public class WorkspaceInformation {
	
	// TODO JAVADOC
	// TODO TEST
	
	// add more fields when actually asked for them
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

	public int getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Optional<String> getNarrativeName() {
		return narrativeName;
	}
	
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
	
	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("WorkspaceInformation [id=");
		builder2.append(id);
		builder2.append(", name=");
		builder2.append(name);
		builder2.append(", narrativeName=");
		builder2.append(narrativeName);
		builder2.append(", isPublic=");
		builder2.append(isPublic);
		builder2.append("]");
		return builder2.toString();
	}

	public static Builder getBuilder(final int wsid, final String name) {
		return new Builder(wsid, name);
	}
	
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
		
		public Builder withNullableNarrativeName(final String narrativeName) {
			if (isNullOrEmpty(narrativeName)) {
				this.narrativeName = Optional.absent();
			} else {
				this.narrativeName = Optional.of(narrativeName);
			}
			return this;
		}
		
		public Builder withIsPublic(final boolean isPublic) {
			this.isPublic = isPublic;
			return this;
		}
		
		public WorkspaceInformation build() {
			return new WorkspaceInformation(id, name, narrativeName, isPublic);
		}
	}

}
