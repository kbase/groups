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
	
	// TODO TEST

	/** Transform a {@link GroupRequest} object into a Map/List structure suitable for
	 * serializing to JSON.
	 * @param request the request object.
	 * @return the JSONable structure.
	 */
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
	
	/** Transform {@link GroupRequest} objects into a list of  Map/List structures suitable for
	 * serializing to JSON.
	 * @param requests the request objects.
	 * @return the list of JSONable structures.
	 */
	public static List<Map<String, Object>> toGroupRequestJSON(
			final Collection<GroupRequest> requests) {
		checkNotNull(requests, "requests");
		return requests.stream().map(r -> toGroupRequestJSON(r)).collect(Collectors.toList());
	}
	
	/** Get a {@link Token} from a string.
	 * @param token the string containing the token.
	 * @param required true if the token is required, false if not. If true, if the token string
	 * is null or whitespace only, an exception will be thrown. If false, null will be
	 * returned in that case.
	 * @return the token or null.
	 * @throws NoTokenProvidedException if the token is required and the token string is null
	 * or whitespace only.
	 */
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
