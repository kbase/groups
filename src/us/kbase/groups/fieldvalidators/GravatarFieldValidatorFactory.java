package us.kbase.groups.fieldvalidators;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/** Validates that a gravatar hash is a valid MD5.
 * Include "strict-length": "true" in the configuration to enforce an exact 32 character MD5.
 * If omitted, any extra characters are ignored (which is what Gravatar does).
 * Include "image-exists": "true" in the configuration to enforce that an image is associated
 * with the hash (see Default Image section at https://en.gravatar.com/site/implement/images/).

 * @author gaprice@lbl.gov
 *
 */
public class GravatarFieldValidatorFactory implements FieldValidatorFactory {

	private static final String TRUE = "true";
	private static final String STRICT_LENGTH = "strict-length";
	private static final String IMAGE_EXISTS = "image-exists";
	
	@Override
	public FieldValidator getValidator(final Map<String, String> configuration)
			throws IllegalParameterException {
		requireNonNull(configuration, "configuration");
		return new GravatarFieldValidator(
				TRUE.equals(configuration.get(STRICT_LENGTH)),
				TRUE.equals(configuration.get(IMAGE_EXISTS)),
				404);
	}
	
	private static class GravatarFieldValidator implements FieldValidator {
		
		private static final Client CLI = ClientBuilder.newClient();
		private static final String GURL = "https://www.gravatar.com/";
		private static final String GPATH = "avatar/";
		private static final int MD5LEN = 32;
		private static Pattern MD5CHARS = Pattern.compile("^[a-f0-9]+$");

		private final boolean strictLength;
		private final boolean imageExists;
		private final int errorCode;
		
		private GravatarFieldValidator(
				final boolean strictLength,
				final boolean accountExists,
				final int errorCode) { // errorCode here to allow for testing the status check
			this.strictLength = strictLength;
			this.imageExists = accountExists;
			this.errorCode = errorCode;
		}
		
		@Override
		public void validate(final String fieldValue)
				throws IllegalFieldValueException, FieldValidatorException {
			requireNonNull(fieldValue, "fieldValue");
			if (fieldValue.length() < MD5LEN) {
				throw new IllegalFieldValueException(String.format(
						"Gravatar hash less than %s characters", MD5LEN));
			}
			if (strictLength && fieldValue.length() > MD5LEN) {
				throw new IllegalFieldValueException(String.format(
						"Gravatar hash must be exactly %s characters", MD5LEN));
			}
			final Matcher m = MD5CHARS.matcher(fieldValue.substring(0, MD5LEN));
			if (!m.find()) {
				throw new IllegalFieldValueException("Gravatar hash is not a valid MD5 string");
			}
			if (imageExists) {
				checkExists(fieldValue);
			}
		}
	
	private void checkExists(final String fieldValue)
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
