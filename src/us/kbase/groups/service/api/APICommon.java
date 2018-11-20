package us.kbase.groups.service.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestType;
import us.kbase.groups.core.request.RequestType;

public class APICommon {
	
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
		ret.put(Fields.REQUEST_TYPE, getType(request));
		ret.put(Fields.REQUEST_RESOURCE_TYPE, getResourceType(request));
		ret.put(Fields.REQUEST_RESOURCE, getResource(request));
		ret.put(Fields.REQUEST_STATUS, request.getStatusType().getRepresentation());
		ret.put(Fields.REQUEST_CREATION, request.getCreationDate().toEpochMilli());
		ret.put(Fields.REQUEST_MODIFICATION, request.getModificationDate().toEpochMilli());
		ret.put(Fields.REQUEST_EXPIRATION, request.getExpirationDate().toEpochMilli());
		return ret;
	}
	
	private static String getResource(GroupRequest request) {
		// TODO NNOW return actual resource
		if (request.getTarget().isPresent()) {
			return request.getTarget().get().getName();
		} else if (request.getCatalogMethodTarget().isPresent()) {
			return request.getCatalogMethodTarget().get().getFullMethod();
		} else {
			return request.getWorkspaceTarget().get().getID() + "";
		}
	}

	private static String getResourceType(final GroupRequest request) {
		// TODO NNOW return actual resource types
		final GroupRequestType t = request.getType();
		if (t.equals(GroupRequestType.INVITE_TO_GROUP)
				|| t.equals(GroupRequestType.REQUEST_GROUP_MEMBERSHIP)) {
			return "user";
		} else if (t.equals(GroupRequestType.INVITE_CATALOG_METHOD) ||
				t.equals(GroupRequestType.REQUEST_ADD_CATALOG_METHOD)) {
			return "catalogmethod";
		} else {
			return "workspace";
		}
	}

	private static String getType(final GroupRequest r) {
		return r.isInvite() ? RequestType.INVITE.getRepresentation() :
			RequestType.REQUEST.getRepresentation();
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
	
	private static final String SORT_ASCENDING = "asc";
	private static final String SORT_DESCENDING = "desc";
	private static final Set<String> SORT_DIRECTION_OPTIONS = new HashSet<>(Arrays.asList(
			SORT_ASCENDING, SORT_DESCENDING));
	
	/** Get parameters for listing requests from a set of strings as may be presented in
	 * query params.
	 * @param excludeUpTo set where the list of requests starts by excluding requests modified
	 * before or after this date, exclusive, depending on the sort direction.
	 * The date must be in epoch milliseconds.
	 * Null or whitespace only values are ignored.
	 * @param includeClosed if not null or whitespace only, closed requests are included.
	 * Otherwise they are excluded from the results.
	 * @param sortDirection the direction of the sort - 'asc' for an ascending sort, and 'desc'
	 * for a descending sort.
	 * @param defaultSort if sortDirection is null or whitespace only, this value is used instead.
	 * true sets an ascending sort, false sets a descending sort.
	 * @return the get request parameters.
	 * @throws IllegalParameterException if excludeUpTo is not a valid date or sortDirection
	 * is not a valid options.
	 */
	public static GetRequestsParams getRequestsParams(
			final String excludeUpTo,
			final String includeClosed,
			final String sortDirection,
			final boolean defaultSort)
			throws IllegalParameterException {
		final GetRequestsParams.Builder b = GetRequestsParams.getBuilder();
		if (!isNullOrEmpty(excludeUpTo)) {
			final long epochms;
			try {
				epochms = Long.parseLong(excludeUpTo.trim());
			} catch (NumberFormatException e) {
				throw new IllegalParameterException("Invalid epoch ms: " + excludeUpTo.trim());
			}
			b.withNullableExcludeUpTo(Instant.ofEpochMilli(epochms));
		}
		setSortDirection(sortDirection, defaultSort, s -> b.withNullableSortAscending(s));
		
		return b.withNullableIncludeClosed(includeClosed != null).build();
	}
	
	/** Get parameters for listing groups from a set of strings as may be presented in
	 * query params.
	 * @param excludeUpTo set where the list of groups starts by excluding groups where the
	 * sort key is greater or less than this value, exclusive, depending on the sort direction.
	 * Null or whitespace only values are ignored.
	 * @param sortDirection the direction of the sort - 'asc' for an ascending sort, and 'desc'
	 * for a descending sort.
	 * @param defaultSort if sortDirection is null or whitespace only, this value is used instead.
	 * true sets an ascending sort, false sets a descending sort.
	 * @return the get groups parameters.
	 * @throws IllegalParameterException if sortDirection is not a valid options.
	 */
	public static GetGroupsParams getGroupsParams(
			final String excludeUpTo,
			final String sortDirection,
			final boolean defaultSort)
			throws IllegalParameterException {
		final GetGroupsParams.Builder b = GetGroupsParams.getBuilder()
				.withNullableExcludeUpTo(excludeUpTo);
		setSortDirection(sortDirection, defaultSort, s -> b.withNullableSortAscending(s));
		return b.build();
	}

	private static void setSortDirection(
			final String sortDirection,
			final boolean defaultSort,
			final Consumer<Boolean> sortConsumer)
			throws IllegalParameterException {
		if (isNullOrEmpty(sortDirection)) {
			sortConsumer.accept(defaultSort);
		} else {
			if (!SORT_DIRECTION_OPTIONS.contains(sortDirection.trim())) {
				throw new IllegalParameterException("Invalid sort direction: " +
						sortDirection.trim());
			}
			sortConsumer.accept(SORT_ASCENDING.equals(sortDirection.trim()));
		}
	}
}
