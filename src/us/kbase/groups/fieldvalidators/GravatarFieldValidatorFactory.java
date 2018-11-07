package us.kbase.groups.fieldvalidators;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.fieldvalidation.FieldValidator;
import us.kbase.groups.core.fieldvalidation.FieldValidatorException;
import us.kbase.groups.core.fieldvalidation.FieldValidatorFactory;
import us.kbase.groups.core.fieldvalidation.IllegalFieldValueException;

/** Validates that a gravatar hash is valid and does not return a 404.
 * @author gaprice@lbl.gov
 *
 */
public class GravatarFieldValidatorFactory implements FieldValidatorFactory {

	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		return new GravatarFieldValidator(404);
	}
	
	private static class GravatarFieldValidator implements FieldValidator {
		
		private final int errorCode;
		
		// this constructor is here solely to allow testing the final exception
		private GravatarFieldValidator(final int errorCode) {
			this.errorCode = errorCode;
		}
		
		private static final Client CLI = ClientBuilder.newClient();
		private static final String GURL = "https://www.gravatar.com/";
		private static final String GPATH = "avatar/";
		
		@Override
		public void validate(final String fieldValue)
				throws IllegalFieldValueException, FieldValidatorException {
			
			final URI target = UriBuilder.fromUri(GURL).path(GPATH + fieldValue).build();
			
			final WebTarget wt = CLI.target(target).queryParam("d", "404");
			final Builder req = wt.request();

			final Response res = req.get();
			if (res.getStatus() == 200) {
				return;
			}
			if (res.getStatus() == errorCode) {
				throw new IllegalFieldValueException(
						"Gravatar service does not recognize Gravatar hash " + fieldValue);
			}
			throw new FieldValidatorException("Error contacting Gravatar service: " +
						res.getStatus());
		}
	}

}
