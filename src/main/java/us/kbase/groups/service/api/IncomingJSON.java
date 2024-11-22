package us.kbase.groups.service.api;

import static us.kbase.groups.util.Util.checkString;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.base.Optional;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class IncomingJSON {

	// TODO JAVADOC or swagger

	private final Map<String, Object> additionalProperties = new TreeMap<>();

	// don't throw error from constructor, makes for crappy error messages.
	protected IncomingJSON() {}
	
	public static String getString(final String string, final String field)
			throws MissingParameterException {
		checkString(string, field);
		return string.trim();
	}

	protected Optional<String> getOptionalString(final String string) {
		if (isNullOrEmpty(string)) {
			return Optional.absent();
		}
		return Optional.of(string.trim());
	}

	protected boolean getBoolean(final Object b, final String fieldName)
			throws IllegalParameterException {
		// may need to configure response for null
		if (b == null) {
			return false;
		}
		if (!(b instanceof Boolean)) {
			throw new IllegalParameterException(fieldName + " must be a boolean");
		}
		return Boolean.TRUE.equals(b) ? true : false;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperties(final String name, final Object value) {
		this.additionalProperties.put(name, value);
	}

	public void exceptOnAdditionalProperties() throws IllegalParameterException {
		if (!additionalProperties.isEmpty()) {
			throw new IllegalParameterException("Unexpected parameters in request: " + 
					String.join(", ", additionalProperties.keySet()));
		}
	}

}
