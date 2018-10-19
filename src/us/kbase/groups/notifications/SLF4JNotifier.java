package us.kbase.groups.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.Notifications;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;

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
				userNamesToStrings(targets),
				request.getID().getID(),
				request.getType().toString(),
				request.getGroupID().getName(),
				group.getGroupName().getName(),
				request.getTarget().isPresent() ? request.getTarget().get().getName() : null,
				request.getRequester().getName()));
	}

	private List<String> userNamesToStrings(final Collection<UserName> targets) {
		return targets.stream().map(t -> t.getName()).collect(Collectors.toList());
	}

	@Override
	public void cancel(final RequestID requestID) {
		LoggerFactory.getLogger(getClass()).info(String.format(
				"Canceled request %s", requestID.getID()));
	}
	
	@Override
	public void deny(
			final Set<UserName> targets,
			final GroupRequest request,
			final UserName deniedBy) {
		LoggerFactory.getLogger(getClass()).info(String.format(
				"User %s denied request %s, targets: %s",
				deniedBy.getName(), request.getID().getID(), userNamesToStrings(targets)));
	}

	@Override
	public void accept(
			final Set<UserName> targets,
			final GroupRequest request,
			final UserName acceptedBy) {
		LoggerFactory.getLogger(getClass()).info(String.format(
				"User %s accepted request %s, targets: %s",
				acceptedBy.getName(), request.getID().getID(), userNamesToStrings(targets)));
	}

}
