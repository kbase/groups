package us.kbase.groups.core;

import static java.util.Objects.requireNonNull;

/** A class consisting of the ID and name of a group with no other fields.
 * @author gaprice@lbl.gov
 *
 */
public class GroupIDAndName {

	private final GroupID id;
	private final GroupName name;
	
	private GroupIDAndName(final GroupID id, final GroupName name) {
		this.id = id;
		this.name = name;
	}
	
	/** Get the group ID.
	 * @return the ID.
	 */
	public GroupID getID() {
		return id;
	}
	
	/** Get the group name.
	 * @return the name.
	 */
	public GroupName getName() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		GroupIDAndName other = (GroupIDAndName) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
	
	/** Create the ID and name container.
	 * @param id the group ID.
	 * @param name the group name.
	 */
	public static GroupIDAndName of(final GroupID id, final GroupName name) {
		return new GroupIDAndName(requireNonNull(id, "id"), requireNonNull(name, "name"));
	}
	
	
}
