package us.kbase.groups.core;

import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** A set of workspaceIDs.
 * @author gaprice@lbl.gov
 *
 */
public class WorkspaceIDSet {
	
	/* Workspace IDs are represented as longs in the workspace service, but the chance we'll 
	 * ever get to 2 billion workspaces is zero unless someone starts abusing the system.
	 */
	
	//TODO TEST
	//TODO TEST immutable
	
	// could probably save a bunch of memory by making this an array. Don't worry about it for
	// now.
	private final Set<Integer> ids;
	
	private WorkspaceIDSet(final Collection<Integer> ids) {
		this.ids = Collections.unmodifiableSet(new HashSet<>(ids));
	}
	
	/** Create a set from {@link WorkspaceID}s.
	 * @param ids the IDs.
	 * @return the new set.
	 */
	public static WorkspaceIDSet fromIDs(final Collection<WorkspaceID> ids) {
		checkNoNullsInCollection(ids, "ids");
		return new WorkspaceIDSet(ids.stream().map(i -> i.getID())
				.collect(Collectors.toSet()));
	}
	
	/** Create a set from ints.
	 * @param ids the IDs.
	 * @return the new set.
	 */
	public static WorkspaceIDSet fromInts(final Collection<Integer> ids) {
		checkNoNullsInCollection(ids, "ids");
		for (final long id: ids) {
			if (id < 1) {
				throw new IllegalArgumentException(String.format("ID %s must be > 1", id));
			}
		}
		return new WorkspaceIDSet(ids);
	}

	/** Get the workspace IDs as longs.
	 * @return the IDs.
	 */
	public Set<Integer> getIds() {
		return ids;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ids == null) ? 0 : ids.hashCode());
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
		WorkspaceIDSet other = (WorkspaceIDSet) obj;
		if (ids == null) {
			if (other.ids != null) {
				return false;
			}
		} else if (!ids.equals(other.ids)) {
			return false;
		}
		return true;
	}
}
