package us.kbase.groups.core.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import java.util.UUID;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

public class RequestID {
	
	private final String id;
	
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
	
	public RequestID(final UUID id) {
		checkNotNull(id, "id");
		this.id = id.toString();
	}

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
