package us.kbase.groups.service.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import us.kbase.groups.core.request.GroupRequest;

public class APICommon {
	
	// TODO JAVADOC
	// TODO TEST

	public static Map<String, Object> toGroupRequestJSON(final GroupRequest request) {
		checkNotNull(request, "request");
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.REQUEST_ID, request.getID().toString());
		ret.put(Fields.REQUEST_GROUP_ID, request.getGroupID().getName());
		ret.put(Fields.REQUEST_REQUESTER, request.getRequester().getName());
		ret.put(Fields.REQUEST_TARGET, request.getTarget().isPresent() ?
				request.getTarget().get().getName() : null);
		ret.put(Fields.REQUEST_TYPE, request.getType().toString());
		ret.put(Fields.REQUEST_STATUS, request.getStatus().toString());
		ret.put(Fields.REQUEST_CREATION, request.getCreationDate().toEpochMilli());
		ret.put(Fields.REQUEST_MODIFICATION, request.getModificationDate().toEpochMilli());
		ret.put(Fields.REQUEST_EXPIRATION, request.getExpirationDate().toEpochMilli());
		return ret;
	}
	
	
	public static List<Map<String, Object>> toGroupRequestJSON(
			final Collection<GroupRequest> requests) {
		return requests.stream().map(r -> toGroupRequestJSON(r)).collect(Collectors.toList());
	}
}
