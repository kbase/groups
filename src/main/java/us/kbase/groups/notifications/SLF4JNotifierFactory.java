package us.kbase.groups.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.notifications.NotificationsFactory;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;

/** A notification implementation that sends a message to SLF4J but otherwise does nothing
 * useful. 
 * @author gaprice@lbl.gov
 *
 */
public class SLF4JNotifierFactory implements NotificationsFactory {

	// TODO JAVADOC
	// TODO TEST
	
	@Override
	public Notifications getNotifier(final Map<String, String> configuration)
			throws IllegalParameterException {
		return new SLF4JNotifier();
	}
	
	private static class SLF4JNotifier implements Notifications {
	
		@Override
		public void notify(
				final Collection<UserName> targets,
				final GroupRequest request) {
			LoggerFactory.getLogger(getClass()).info(String.format(
					"Notifying %s of request %s %s for group %s adding %s %s " +
					"requested by %s",
					userNamesToStrings(targets),
					request.getID().getID(),
					request.getType().getRepresentation(),
					request.getGroupID().getName(),
					request.getResourceType().getName(),
					request.getResource().getResourceID().getName(),
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
		public void deny(final Collection<UserName> targets, final GroupRequest request) {
			LoggerFactory.getLogger(getClass()).info(String.format(
					"User %s denied request %s, targets: %s",
					request.getClosedBy().get().getName(), request.getID().getID(),
					userNamesToStrings(targets)));
		}
	
		@Override
		public void accept(final Collection<UserName> targets, final GroupRequest request) {
			LoggerFactory.getLogger(getClass()).info(String.format(
					"User %s accepted request %s, targets: %s",
					request.getClosedBy().get().getName(), request.getID().getID(),
					userNamesToStrings(targets)));
		}

		@Override
		public void addResource(
				final UserName user,
				final Set<UserName> targets,
				final GroupID groupID,
				final ResourceType type,
				final ResourceID resource) {
			LoggerFactory.getLogger(getClass()).info(String.format(
					"User %s added %s %s to group %s, targets: %s",
					user.getName(), type.getName(), resource.getName(), groupID.getName(),
					userNamesToStrings(targets)));
		}
	}

}
