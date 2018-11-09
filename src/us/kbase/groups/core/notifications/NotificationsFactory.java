package us.kbase.groups.core.notifications;

import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public interface NotificationsFactory {

	//TODO JAVADOC
	
	Notifications getNotifier(Map<String, String> configuration)
			throws IllegalParameterException, MissingParameterException; 
	
}
