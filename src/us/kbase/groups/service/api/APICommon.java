package us.kbase.groups.service.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.request.GroupRequest;

public class APICommon {
	
	// TODO JAVADOC
	// TODO TEST

	public static Map<String, Object> toGroupRequestJSON(final GroupRequest request) {
		checkNotNull(request, "request");
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.REQUEST_ID, request.getID().getID());
		ret.put(Fields.REQUEST_GROUP_ID, request.getGroupID().getName());
		ret.put(Fields.REQUEST_REQUESTER, request.getRequester().getName());
		ret.put(Fields.REQUEST_TARGET, request.getTarget().isPresent() ?
				request.getTarget().get().getName() : null);
		ret.put(Fields.REQUEST_TYPE, request.getType().getRepresentation());
		ret.put(Fields.REQUEST_STATUS, request.getStatusType().getRepresentation());
		ret.put(Fields.REQUEST_CREATION, request.getCreationDate().toEpochMilli());
		ret.put(Fields.REQUEST_MODIFICATION, request.getModificationDate().toEpochMilli());
		ret.put(Fields.REQUEST_EXPIRATION, request.getExpirationDate().toEpochMilli());
		return ret;
	}
	
	
	public static List<Map<String, Object>> toGroupRequestJSON(
			final Collection<GroupRequest> requests) {
		checkNotNull(requests, "requests");
		return requests.stream().map(r -> toGroupRequestJSON(r)).collect(Collectors.toList());
	}
	
	public static Token getToken(final String token, final boolean required)
			throws NoTokenProvidedException {
		if (isNullOrEmpty(token)) {
			if (required) {
				throw new NoTokenProvidedException("No token provided");
			} else {
				return null;
			}
		} else {
			try {
				return new Token(token);
			} catch (MissingParameterException e) {
				throw new RuntimeException("This is impossible. It didn't happen.", e);
			}
		}
	}
}
