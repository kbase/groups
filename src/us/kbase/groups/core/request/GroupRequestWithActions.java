package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.util.Set;

/** Represents a {@link GroupRequest} along with specific {@link GroupRequestUserAction}s a user
 * is permitted to take on the request.
 * @author gaprice@lbl.gov
 *
 */
public class GroupRequestWithActions {

	private final GroupRequest request;
	private final Set<GroupRequestUserAction> actions;
	
	/** Construct the request.
	 * @param request the request.
	 * @param actions the actions that are allowable for the request and some user.
	 */
	public GroupRequestWithActions(
			final GroupRequest request,
			final Set<GroupRequestUserAction> actions) {
		checkNotNull(request, "request");
		checkNoNullsInCollection(actions, "actions");
		this.request = request;
		this.actions = actions;
	}

	/** Get the request.
	 * @return the request.
	 */
	public GroupRequest getRequest() {
		return request;
	}

	/** Get the actions that are permitted for the request.
	 * @return the actions.
	 */
	public Set<GroupRequestUserAction> getActions() {
		return actions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actions == null) ? 0 : actions.hashCode());
		result = prime * result + ((request == null) ? 0 : request.hashCode());
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
		GroupRequestWithActions other = (GroupRequestWithActions) obj;
		if (actions == null) {
			if (other.actions != null) {
				return false;
			}
		} else if (!actions.equals(other.actions)) {
			return false;
		}
		if (request == null) {
			if (other.request != null) {
				return false;
			}
		} else if (!request.equals(other.request)) {
			return false;
		}
		return true;
	}
}
