package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;
import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.service.api.APICommon.getGroupsParams;
import static us.kbase.groups.service.api.APICommon.getRequestsParams;
import static us.kbase.groups.service.api.APICommon.toGroupJSON;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalGroupFields.Builder;
import us.kbase.groups.core.OptionalString;
import us.kbase.groups.core.UserName;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.GroupExistsException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchCustomFieldException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchResourceTypeException;
import us.kbase.groups.core.exceptions.NoSuchUserException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.RequestExistsException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.fieldvalidation.FieldValidatorException;
import us.kbase.groups.core.fieldvalidation.NumberedCustomField;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.GROUP)
public class GroupsAPI {

	// TODO JAVADOC / swagger
	
	private final Groups groups;
	
	// normally instantiated by Jersey
	@Inject
	public GroupsAPI(final Groups groups) {
		this.groups = groups;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getGroups(
			@HeaderParam(HEADER_TOKEN) final String token,
			@QueryParam(Fields.GET_GROUPS_EXCLUDE_UP_TO) final String excludeUpTo,
			@QueryParam(Fields.GET_GROUPS_SORT_ORDER) final String order)
			throws GroupsStorageException, IllegalParameterException, NoTokenProvidedException,
				InvalidTokenException, AuthenticationException {
		return groups.getGroups(
				getToken(token, false),
				getGroupsParams(excludeUpTo, order, true))
				.stream().map(g -> toGroupJSON(g)).collect(Collectors.toList());
	}
	
	private static Map<NumberedCustomField, OptionalString> getCustomFieldsAndTypeCheck(
			final Object customFields,
			final String fieldName)
			throws IllegalParameterException, MissingParameterException {
		// jackson errors are too ugly, so we do it ourselves
		if (customFields == null) {
			return Collections.emptyMap();
		}
		if (!(customFields instanceof Map)) {
			throw new IllegalParameterException("'" + fieldName + "' field must be a mapping");
		}
		// we'll assume the map keys are strings, since it's coming in as json.
		@SuppressWarnings("unchecked")
		final Map<String, String> map = (Map<String, String>) customFields;
		final Map<NumberedCustomField, OptionalString> ret = new HashMap<>();
		for (final String s: map.keySet()) {
			if (map.get(s) != null && !(map.get(s) instanceof String)) {
				throw new IllegalParameterException(String.format(
						"Value of '%s' field in 'custom' map is not a string", s));
			}
			ret.put(new NumberedCustomField(s), OptionalString.ofEmptyable(map.get(s)));
		}
		return ret;
	}
	
	public static class CreateOrUpdateGroupJSON extends IncomingJSON {
		
		@JsonProperty(Fields.GROUP_NAME)
		private String groupName;
		@JsonProperty(Fields.GROUP_IS_PRIVATE)
		private Boolean isPrivate;
		@JsonProperty(Fields.GROUP_MEMBERS_PRIVATE)
		private Boolean isPrivateMembers;
		@JsonProperty(Fields.GROUP_CUSTOM_FIELDS)
		private Object customFields;

		@SuppressWarnings("unused")
		private CreateOrUpdateGroupJSON() {} // default constructor for Jackson
		
		// this constructor is for testing. Jackson *must* inject the fields so that the
		// optionals can be null, which distinguishes a missing field from a field with a null
		// value.
		// 18/12/10 ok, there are no optionals anymore, but we'll leave this here in case we
		// want to add them again.
		public CreateOrUpdateGroupJSON(
				final String groupName,
				final Boolean isPrivate,
				final Boolean isPrivateMembers,
				final Object customFields) {
			this.groupName = groupName;
			this.isPrivate = isPrivate;
			this.isPrivateMembers = isPrivateMembers;
			this.customFields = customFields;
		}

		private GroupCreationParams toCreateParams(final GroupID groupID)
				throws MissingParameterException, IllegalParameterException {
			return GroupCreationParams.getBuilder(groupID, new GroupName(groupName))
					.withOptionalFields(getOptionalFieldsBuilder(true)
							.build())
					.build();
		}

		private Builder getOptionalFieldsBuilder(final boolean ignoreMissingValues)
				throws IllegalParameterException, MissingParameterException {
			final Map<NumberedCustomField, OptionalString> customFields =
					getCustomFieldsAndTypeCheck(this.customFields, Fields.GROUP_CUSTOM_FIELDS);
			final Builder b = OptionalGroupFields.getBuilder()
					.withNullableIsPrivate(isPrivate)
					.withNullablePrivateMemberList(isPrivateMembers);
			for (final NumberedCustomField k: customFields.keySet()) {
				if (!ignoreMissingValues || customFields.get(k).isPresent()) {
					b.withCustomField(k, customFields.get(k));
				}
			}
			return b;
		}

		private interface FuncExcept<T, R> {
			
			R apply(T item) throws IllegalParameterException, MissingParameterException;
		}
		
		private GroupUpdateParams toUpdateParams(final GroupID groupID)
				throws IllegalParameterException, MissingParameterException {
			return GroupUpdateParams.getBuilder(groupID)
					.withNullableName(fromNullable(groupName, s -> new GroupName(s)))
					.withOptionalFields(getOptionalFieldsBuilder(false)
							.build())
					.build();
		}
		
		private <T> T fromNullable(
				final String nullable,
				final FuncExcept<String, T> getValue)
				throws IllegalParameterException, MissingParameterException {
			return isNullOrEmpty(nullable) ? null : getValue.apply(nullable.trim());
		}
		
	}
	
	@PUT
	@Path(ServicePaths.GROUP_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> createGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			final CreateOrUpdateGroupJSON create)
			throws MissingParameterException, IllegalParameterException, InvalidTokenException,
				AuthenticationException, GroupExistsException, GroupsStorageException,
				NoSuchCustomFieldException, FieldValidatorException {
		checkIncomingJson(create);
		return toGroupJSON(groups.createGroup(getToken(token, true),
				create.toCreateParams(new GroupID(groupID))));
	}
	
	@PUT
	@Path(ServicePaths.GROUP_UPDATE)
	@Produces(MediaType.APPLICATION_JSON)
	public void updateGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			final CreateOrUpdateGroupJSON update)
			throws MissingParameterException, IllegalParameterException, InvalidTokenException,
				NoSuchGroupException, NoTokenProvidedException, AuthenticationException,
				UnauthorizedException, GroupsStorageException, NoSuchCustomFieldException,
				FieldValidatorException {
		checkIncomingJson(update);
		groups.updateGroup(getToken(token, true), update.toUpdateParams(new GroupID(groupID)));
	}
	
	@GET
	@Path(ServicePaths.GROUP_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, MissingParameterException, IllegalParameterException,
				GroupsStorageException, ResourceHandlerException {
		return toGroupJSON(groups.getGroup(getToken(token, false), new GroupID(groupID)));
	}
	
	@GET
	@Path(ServicePaths.GROUP_EXISTS)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getGroupExists(
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws MissingParameterException, IllegalParameterException, GroupsStorageException {
		return ImmutableMap.of(Fields.EXISTS, (groups.getGroupExists(new GroupID(groupID))));
	}
	
	@PUT
	@Path(ServicePaths.GROUP_VISIT)
	public void visitGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				NoTokenProvidedException, AuthenticationException, MissingParameterException,
				IllegalParameterException, GroupsStorageException {
		groups.userVisited(getToken(token, true), new GroupID(groupID));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_REQUEST_MEMBERSHIP)
	public Map<String, Object> requestGroupMembership(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID)
			throws InvalidTokenException, NoSuchGroupException, AuthenticationException,
				UserIsMemberException, MissingParameterException, IllegalParameterException,
				GroupsStorageException, RequestExistsException {
		return APICommon.toGroupRequestJSON(groups.requestGroupMembership(
				getToken(token, true), new GroupID(groupID)));
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_USER_ID)
	public Map<String, Object> inviteMember(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				AuthenticationException, UnauthorizedException, UserIsMemberException,
				RequestExistsException, MissingParameterException, IllegalParameterException,
				GroupsStorageException {
		return APICommon.toGroupRequestJSON(groups.inviteUserToGroup(
				getToken(token, true), new GroupID(groupID), new UserName(member)));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.GROUP_REQUESTS)
	public List<Map<String, Object>> getRequestsForGroup(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@QueryParam(Fields.GET_REQUESTS_EXCLUDE_UP_TO) final String excludeUpTo,
			@QueryParam(Fields.GET_REQUESTS_INCLUDE_CLOSED) final String closed,
			@QueryParam(Fields.GET_REQUESTS_SORT_ORDER) final String order)
			throws InvalidTokenException, NoSuchGroupException, UnauthorizedException,
				AuthenticationException, MissingParameterException, IllegalParameterException,
				GroupsStorageException, NoSuchResourceTypeException {
		return APICommon.toGroupRequestJSON(groups.getRequestsForGroup(
				getToken(token, true), new GroupID(groupID),
				getRequestsParams(excludeUpTo, closed, order, closed == null)));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_USER_ID)
	public void removeMember(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws NoSuchGroupException, InvalidTokenException, NoSuchUserException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException {
		groups.removeMember(getToken(token, true), new GroupID(groupID), new UserName(member));
	}

	public static class UpdateUserJSON extends IncomingJSON {
		
		@JsonProperty(Fields.GROUP_MEMBER_CUSTOM_FIELDS)
		private Object customFields;

		@SuppressWarnings("unused")
		private UpdateUserJSON() {} // default constructor for Jackson
		
		// this constructor is for testing.
		public UpdateUserJSON(final Object customFields) {
			this.customFields = customFields;
		}
	}
	
	@PUT
	@Path(ServicePaths.GROUP_USER_ID_UPDATE)
	public void updateUser(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member,
			final UpdateUserJSON update)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				NoTokenProvidedException, AuthenticationException, UnauthorizedException,
				IllegalParameterException, NoSuchCustomFieldException, MissingParameterException,
				GroupsStorageException, FieldValidatorException {
		checkIncomingJson(update);
		final Map<NumberedCustomField, OptionalString> customFields = getCustomFieldsAndTypeCheck(
				update.customFields, Fields.GROUP_MEMBER_CUSTOM_FIELDS);
		if (customFields.isEmpty()) {
			throw new MissingParameterException("No fields provided to update");
		}
		groups.updateUser(getToken(token, true), new GroupID(groupID), new UserName(member),
				customFields);
	}
	
	@PUT
	@Path(ServicePaths.GROUP_USER_ID_ADMIN)
	public void promoteMember(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				NoTokenProvidedException, AuthenticationException, UnauthorizedException,
				UserIsMemberException, MissingParameterException, IllegalParameterException,
				GroupsStorageException {
		groups.promoteMember(getToken(token, true), new GroupID(groupID), new UserName(member));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_USER_ID_ADMIN)
	public void demoteAdmin(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_MEMBER) final String member)
			throws InvalidTokenException, NoSuchGroupException, NoSuchUserException,
				NoTokenProvidedException, AuthenticationException, UnauthorizedException,
				MissingParameterException, IllegalParameterException, GroupsStorageException {
		groups.demoteAdmin(getToken(token, true), new GroupID(groupID), new UserName(member));
	}
	
	@POST
	@Path(ServicePaths.GROUP_RESOURCE_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> addResource(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_RESOURCE_TYPE) final String resourceType,
			@PathParam(Fields.GROUP_RESOURCE_ID) final String resourceID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException, RequestExistsException,
				NoSuchResourceException, IllegalResourceIDException, ResourceExistsException,
				ResourceHandlerException, NoSuchResourceTypeException {
		return toGroupRequestJSON(groups.addResource(
				getToken(token, true),
				new GroupID(groupID),
				new ResourceType(resourceType),
				new ResourceID(resourceID)));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_RESOURCE_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public void removeResource(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_RESOURCE_TYPE) final String resourceType,
			@PathParam(Fields.GROUP_RESOURCE_ID) final String resourceID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException, NoSuchResourceTypeException {
		groups.removeResource(
				getToken(token, true),
				new GroupID(groupID),
				new ResourceType(resourceType),
				new ResourceID(resourceID));
	}
	
	@POST
	@Path(ServicePaths.GROUP_RESOURCE_ID_PERMS)
	@Produces(MediaType.APPLICATION_JSON)
	public void getPerms(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_RESOURCE_TYPE) final String resourceType,
			@PathParam(Fields.GROUP_RESOURCE_ID) final String resourceID)
			throws InvalidTokenException, NoSuchGroupException, NoSuchResourceException,
				NoTokenProvidedException, AuthenticationException, UnauthorizedException,
				NoSuchResourceTypeException, MissingParameterException, IllegalParameterException,
				GroupsStorageException, ResourceHandlerException {
		groups.setReadPermission(
				getToken(token, true),
				new GroupID(groupID),
				new ResourceType(resourceType),
				new ResourceID(resourceID));
	}
	
	private Map<String, Object> toGroupRequestJSON(final Optional<GroupRequest> req) {
		final Map<String, Object> ret;
		if (req.isPresent()) {
			ret = APICommon.toGroupRequestJSON(req.get());
		} else {
			ret = new HashMap<>();
		}
		ret.put(Fields.GROUP_COMPLETE, !req.isPresent());
		return ret;
	}

	private void checkIncomingJson(final IncomingJSON json)
			throws IllegalParameterException, MissingParameterException {
		if (json == null) {
			throw new MissingParameterException("Missing JSON body");
		}
		json.exceptOnAdditionalProperties();
	}
}
