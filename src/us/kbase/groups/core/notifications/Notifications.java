package us.kbase.groups.core.notifications;

import java.util.Collection;
import java.util.Set;

import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

public interface Notifications {

	// TODO JAVADOC
	
	void notify(Collection<UserName> targets, GroupRequest request);

	void cancel(RequestID requestID);

	void deny(Collection<UserName> targets, GroupRequest request);

	void accept(Collection<UserName> targets, GroupRequest request);

	void addResource(
			UserName user,
			Set<UserName> targets,
			GroupID groupID,
			ResourceType type,
			ResourceID resource);
	
}
