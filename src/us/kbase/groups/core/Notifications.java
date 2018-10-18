package us.kbase.groups.core;

import java.util.Set;
import java.util.UUID;

import us.kbase.groups.core.request.GroupRequest;

public interface Notifications {

	// TODO JAVADOC
	
	void notify(Set<UserName> targets, Group group, GroupRequest request);

	void cancel(UUID requestID);

	void deny(Set<UserName> targets, GroupRequest request, UserName deniedBy);

	void accept(Set<UserName> targets, GroupRequest request, UserName acceptedBy);
	
}
