package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkNoNullsInCollection;

import java.util.Set;

public class GroupRequestWithActions {

	// TODO JAVADOC
	// TODO TEST
	
	private final GroupRequest request;
	private final Set<GroupRequestUserAction> actions;
	
	public GroupRequestWithActions(
			final GroupRequest request,
			final Set<GroupRequestUserAction> actions) {
		checkNotNull(request, "request");
		checkNoNullsInCollection(actions, "actions");
		this.request = request;
		this.actions = actions;
	}

	public GroupRequest getRequest() {
		return request;
	}

	public Set<GroupRequestUserAction> getActions() {
		return actions;
	}
}
