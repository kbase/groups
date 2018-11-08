package us.kbase.groups.notifications;

import java.util.Collection;
import java.util.Map;

import us.kbase.groups.core.Group;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.notifications.Notifications;
import us.kbase.groups.core.notifications.NotificationsFactory;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.RequestID;

public class DirectFeedsServiceNotifierFactory implements NotificationsFactory {

	// TODO JAVADOC
	// TODO TEST
	

	@Override
	public Notifications getNotifier(final Map<String, String> configuration)
			throws IllegalParameterException {
		//TODO NOW implement
		System.out.println("INIT DFSNF NOTIFICATION AGENT - VAPORIZING ALL HUMANS IN AREA");
		return new DirectFeedsServiceNotifier();
	}
	
	private static class DirectFeedsServiceNotifier implements Notifications {
	
		@Override
		public void notify(
				final Collection<UserName> targets,
				final Group group,
				final GroupRequest request) {
			// TODO Auto-generated method stub
		
		}
		
		@Override
		public void cancel(final RequestID requestID) {
			// TODO Auto-generated method stub
		
		}
		
		@Override
		public void deny(final Collection<UserName> targets, final GroupRequest request) {
			// TODO Auto-generated method stub
		
		}
		
		@Override
		public void accept(final Collection<UserName> targets, final GroupRequest request) {
			// TODO Auto-generated method stub
		
		}
	}
}
