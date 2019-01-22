package us.kbase.groups.core.notifications;

import java.util.Map;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A factory for a {@link Notifications}.
 * @author gaprice@lbl.gov
 *
 */
public interface NotificationsFactory {

	/** Get a notifier.
	 * @param configuration the parameters for the notifier. The contents of the parameters will
	 * differ from notifier impelementation to implementation.
	 * @return the notifier.
	 * @throws IllegalParameterException if a parameter is illegal.
	 * @throws MissingParameterException if a parameter is missing.
	 */
	Notifications getNotifier(Map<String, String> configuration)
			throws IllegalParameterException, MissingParameterException; 
	
}
