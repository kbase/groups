package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import java.util.UUID;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** The ID of a request. The format is a UUID.
 * @author gaprice@lbl.gov
 *
 */
public class RequestID {
	
	private final String id;
	
	/** Create a request ID.
	 * @param id the ID. Format is identical to a UUID.
	 * @throws IllegalParameterException if the input is not a UUID.
	 * @throws MissingParameterException if the input is null or whitespace only.
	 */
	public RequestID(final String id)
			throws IllegalParameterException, MissingParameterException {
		checkString(id, "request id");
		try {
			UUID.fromString(id); // check uuid
		} catch (IllegalArgumentException e) {
			throw new IllegalParameterException(id + " is not a valid request id");
		}
		this.id = id;
	}
	
	/** Create a request ID from a UUID.
	 * @param id the UUID.
	 */
	public RequestID(final UUID id) {
		checkNotNull(id, "id");
		this.id = id.toString();
	}

	/** Get the request ID.
	 * @return the request ID.
	 */
	public String getID() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RequestID other = (RequestID) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

}
