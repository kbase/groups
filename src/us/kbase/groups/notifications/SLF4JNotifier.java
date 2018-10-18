package us.kbase.groups.notifications;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.Notifications;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.request.GroupRequest;

/** A notification implementation that sends a message to SLF4J but otherwise does nothing
 * useful. 
 * @author gaprice@lbl.gov
 *
 */
public class SLF4JNotifier implements Notifications {

	// TODO JAVADOC
	// TODO TEST
	
	@Override
	public void notify(
			final Set<UserName> targets,
			final Group group,
			final GroupRequest request) {
		LoggerFactory.getLogger(getClass()).info(String.format(
				"Notifying %s of request %s %s for group %s (%s) with target %s requested by %s",
				targets.stream().map(t -> t.getName()).collect(Collectors.toList()),
				request.getID().toString(),
				request.getType().toString(),
				request.getGroupID().getName(),
				group.getGroupName().getName(),
				request.getTarget().isPresent() ? request.getTarget().get().getName() : null,
				request.getRequester().getName()));
	}

	@Override
	public void cancel(final UUID requestID) {
		LoggerFactory.getLogger(getClass()).info(String.format(
				"Canceled request %s", requestID.toString()));
	}

}
