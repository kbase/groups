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

public class GravatarFieldValidatorFactory implements FieldValidatorFactory {

	// TODO JAVADOC
	// TODO TEST
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		return new GravatarFieldValidator();
	}
	
	private static class GravatarFieldValidator implements FieldValidator {
		
		private static final Client CLI = ClientBuilder.newClient();
		private static final String GURL = "https://www.gravatar.com/";
		private static final String GPATH = "avatar/";
		
		@Override
		public void validate(final String fieldValue)
				throws IllegalFieldValueException, FieldValidatorException {
			
			final URI target = UriBuilder.fromUri(GURL).path(GPATH + fieldValue).build();
			
			final WebTarget wt = CLI.target(target).queryParam("d", "404");
			System.out.println(wt);
			final Builder req = wt.request();

			final Response res = req.get();
			if (res.getStatus() == 200) {
				return;
			}
			if (res.getStatus() == 404) {
				throw new IllegalFieldValueException(
						"Gravatar service does not recognize Gravatar hash " + fieldValue);
			}
			// pretty much impossible to test other than changing one of the above values
			throw new FieldValidatorException("Error contacting Gravatar service: " +
						res.getStatus());
		}
	}
	
	public static void main(String[] args) throws Exception {
		final FieldValidator v = new GravatarFieldValidatorFactory().getValidator(null);
		
		v.validate("87194228ef49d635fec5938099042b1d");
		v.validate("87194228ef49d635fec5938099042b1");
		
	}

}
