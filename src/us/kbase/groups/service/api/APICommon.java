package us.kbase.groups.service.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import us.kbase.groups.core.GetGroupsParams;
import us.kbase.groups.core.GetRequestsParams;
import us.kbase.groups.core.Group.Role;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.GroupView.GroupUserView;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;

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
		ret.put(Fields.REQUEST_TYPE, request.getType().getRepresentation());
		ret.put(Fields.REQUEST_RESOURCE_TYPE, request.getResourceType().getName());
		ret.put(Fields.REQUEST_RESOURCE, request.getResource().getResourceID().getName());
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
	
	/** Convert a {@link GroupView} to a map based structure suitable for serializing to JSON.
	 * @param group the group view.
	 * @return JSONable data.
	 */
	public static Map<String, Object> toGroupJSON(final GroupView group) {
		requireNonNull(group, "group");
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.GROUP_ID, group.getGroupID().getName());
		ret.put(Fields.GROUP_IS_PRIVATE, group.isPrivate());
		ret.put(Fields.GROUP_ROLE, group.getRole().getRepresentation());
		if (!group.isPrivateView()) {
			ret.put(Fields.GROUP_NAME, group.getGroupName().get().getName());
			ret.put(Fields.GROUP_OWNER, group.getOwner().get().getName());
			ret.put(Fields.GROUP_MEMBER_COUNT, group.getMemberCount().get());
			ret.put(Fields.GROUP_CUSTOM_FIELDS, getCustomFields(group.getCustomFields()));
			ret.put(Fields.GROUP_CREATION, group.getCreationDate().get().toEpochMilli());
			ret.put(Fields.GROUP_MODIFICATION, group.getModificationDate().get().toEpochMilli());
			ret.put(Fields.GROUP_VISIT_DATE, toEpochMilli(group.getLastVisit()));
			final Map<String, Object> resourceCounts = new HashMap<>();
			ret.put(Fields.GROUP_RESOURCE_COUNT, resourceCounts);
			for (final ResourceType t: group.getResourceCounts().keySet()) {
				resourceCounts.put(t.getName(), group.getResourceCounts().get(t));
			}
			if (group.isStandardView()) {
				ret.put(Fields.GROUP_MEMBERS_PRIVATE, group.isPrivateMembersList().get());
				ret.put(Fields.GROUP_OWNER, toUserJson(group.getMember(group.getOwner().get())));
				ret.put(Fields.GROUP_MEMBERS, toMemberList(group.getMembers(), group));
				ret.put(Fields.GROUP_ADMINS, toMemberList(group.getAdministrators(), group));
				final Map<String, Object> resources = new HashMap<>();
				ret.put(Fields.GROUP_RESOURCES, resources);
				for (final ResourceType t: group.getResourceTypes()) {
					resources.put(t.getName(), getResourceList(group, t));
				}
			}
		}
		return ret;
	}
	
	private static Long toEpochMilli(final Optional<Instant> instant) {
		return instant.map(i -> i.toEpochMilli()).orElse(null);
	}
	
	private static Map<String, Object> toUserJson(final GroupUserView user) {
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.GROUP_MEMBER_NAME, user.getName().getName());
		ret.put(Fields.GROUP_MEMBER_JOIN_DATE, toEpochMilli(user.getJoinDate()));
		ret.put(Fields.GROUP_MEMBER_VISIT_DATE, toEpochMilli(user.getLastVisit()));
		ret.put(Fields.GROUP_MEMBER_CUSTOM_FIELDS, getCustomFields(user.getCustomFields()));
		return ret;
	}
	
	private static List<Map<String, Object>> toMemberList(
			final Collection<UserName> members,
			final GroupView group) {
		return members.stream().sorted().map(m -> toUserJson(group.getMember(m)))
				.collect(Collectors.toList());
	}

	private static List<Map<String, Object>> getResourceList(
			final GroupView g,
			final ResourceType t) {
		final ResourceInformationSet resourceInfo = g.getResourceInformation(t);
		final List<Map<String, Object>> rlist = new LinkedList<>();
		for (final ResourceID rd: sorted(resourceInfo)) {
			final Map<String, Object> resource = new HashMap<>();
			rlist.add(resource);
			resource.putAll(resourceInfo.getFields(rd));
			resource.put(Fields.GROUP_RESOURCE_ID, rd.getName());
			final Long added = Role.NONE.equals(g.getRole()) ?
					null : toEpochMilli(g.getResourceAddDate(t, rd));
			resource.put(Fields.GROUP_RESOURCE_ADDED, added);
		}
		return rlist;
	}
	
	private static Map<String, String> getCustomFields(
			final Map<NumberedCustomField, String> fields) {
		return fields.keySet().stream().collect(Collectors.toMap(
				k -> k.getField(), k -> fields.get(k)));
	}

	// may want to subclass the descriptors to allow for resource type specific sorts
	// specifically for workspaces
	// or associate a comparator with a resource type
	private static List<ResourceID> sorted(final ResourceInformationSet resources) {
		final List<ResourceID> ret = new ArrayList<>(resources.getResources());
		Collections.sort(ret);
		return ret;
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
			b.withNullableExcludeUpTo(epochMilliStringToInstant(excludeUpTo));
		}
		setSortDirection(sortDirection, defaultSort, s -> b.withNullableSortAscending(s));
		
		return b.withNullableIncludeClosed(includeClosed != null).build();
	}

	/** Parse epoch milliseconds as a string into an {@link Instant}. Surrounding whitespace
	 * is ignored.
	 * @param epochMilli the string epoch milliseconds.
	 * @return the parsed Instant.
	 * @throws IllegalParameterException if the string is not epoch milliseconds.
	 */
	public static Instant epochMilliStringToInstant(final String epochMilli)
			throws IllegalParameterException {
		requireNonNull(epochMilli, "epochMilli");
		final Instant epochms;
		try {
			epochms = Instant.ofEpochMilli(Long.parseLong(epochMilli.trim()));
		} catch (NumberFormatException e) {
			throw new IllegalParameterException("Invalid epoch ms: " + epochMilli.trim());
		}
		return epochms;
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
	
	/** Split a comma separated string into a set of group IDs. Whitespace only entries are
	 * ignored.
	 * @param commaSeparatedGroupIDs the group IDs as a comma separated string.
	 * @return the group IDs.
	 * @throws IllegalParameterException if an ID is illegal.
	 */
	public static Set<GroupID> toGroupIDs(final String commaSeparatedGroupIDs)
			throws IllegalParameterException {
		final Set<GroupID> groupIDs = new HashSet<>();
		for (final String id: requireNonNull(commaSeparatedGroupIDs, "commaSeparatedGroupIDs")
				.split(",")) {
			// can't use streams due to checked exceptions
			if (!id.trim().isEmpty()) {
				try {
					groupIDs.add(new GroupID(id));
				} catch (MissingParameterException e) {
					throw new RuntimeException("This is impossible. It didn't happen.", e);
				}
			}
		}
		return groupIDs;
	}
}
