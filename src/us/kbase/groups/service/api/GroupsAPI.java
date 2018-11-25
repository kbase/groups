package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;
import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.util.Util.isNullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
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

import us.kbase.groups.core.FieldItem.StringField;
import us.kbase.groups.core.GroupCreationParams;
import us.kbase.groups.core.GroupID;
import us.kbase.groups.core.GroupName;
import us.kbase.groups.core.GroupType;
import us.kbase.groups.core.GroupUpdateParams;
import us.kbase.groups.core.GroupView;
import us.kbase.groups.core.Groups;
import us.kbase.groups.core.OptionalGroupFields;
import us.kbase.groups.core.OptionalGroupFields.Builder;
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
import us.kbase.groups.core.resource.ResourceDescriptor;
import us.kbase.groups.core.resource.ResourceID;
import us.kbase.groups.core.resource.ResourceInformationSet;
import us.kbase.groups.core.resource.ResourceType;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.GROUP)
public class GroupsAPI {

	// TODO NOW reduce request list size
	// TODO JAVADOC / swagger
	// TODO NOW add endpoint for getting group types
	
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
			throws GroupsStorageException, IllegalParameterException {
		return groups.getGroups(APICommon.getGroupsParams(excludeUpTo, order, true)).stream()
				.map(g -> toGroupJSON(g)).collect(Collectors.toList());
	}
	
	public static class CreateOrUpdateGroupJSON extends IncomingJSON {
		
		@JsonProperty(Fields.GROUP_NAME)
		private Optional<String> groupName;
		@JsonProperty(Fields.GROUP_TYPE)
		private Optional<String> type;
		@JsonProperty(Fields.GROUP_DESCRIPTION)
		private Optional<String> description;
		@JsonProperty(Fields.GROUP_CUSTOM_FIELDS)
		private Object customFields;

		@SuppressWarnings("unused")
		private CreateOrUpdateGroupJSON() {} // default constructor for Jackson
		
		// this constructor is for testing. Jackson *must* inject the fields so that the
		// optionals can be null, which distinguishes a missing field from a field with a null
		// value.
		public CreateOrUpdateGroupJSON(
				final Optional<String> groupName,
				final Optional<String> type,
				final Optional<String> description,
				final Object customFields) {
			this.groupName = groupName;
			this.type = type;
			this.description = description;
			this.customFields = customFields;
		}

		private GroupCreationParams toCreateParams(final GroupID groupID)
				throws MissingParameterException, IllegalParameterException {
			final GroupCreationParams.Builder gbuilder = GroupCreationParams.getBuilder(
					groupID, new GroupName(fromNullable(groupName)))
					.withOptionalFields(getOptionalFieldsBuilder(true)
							.withDescription(StringField.fromNullable(fromNullable(description)))
							.build());
			final String gtype = fromNullable(type);
			if (!isNullOrEmpty(gtype)) {
				gbuilder.withType(GroupType.fromRepresentation(gtype));
			}
			return gbuilder.build();
		}

		private Builder getOptionalFieldsBuilder(final boolean ignoreMissingValues)
				throws IllegalParameterException, MissingParameterException {
			final Map<String, String> customFields = getCustomFieldsAndTypeCheck();
			final Builder b = OptionalGroupFields.getBuilder();
			if (customFields != null) {
				for (final String k: customFields.keySet()) {
					if (!ignoreMissingValues || !isNullOrEmpty(customFields.get(k))) {
						b.withCustomField(new NumberedCustomField(k),
								getStringField(Optional.ofNullable(customFields.get(k))));
					}
				}
			}
			return b;
		}

		private Map<String, String> getCustomFieldsAndTypeCheck()
				throws IllegalParameterException {
			// jackson errors are too ugly, so we do it ourselves
			if (customFields == null) {
				return null;
			}
			if (!(customFields instanceof Map)) {
				throw new IllegalParameterException("'custom' field must be a mapping");
			}
			// we'll assume the map keys are strings, since it's coming in as json.
			@SuppressWarnings("unchecked")
			final Map<String, String> map = (Map<String, String>) customFields;
			for (final String s: map.keySet()) {
				if (map.get(s) != null && !(map.get(s) instanceof String)) {
					throw new IllegalParameterException(String.format(
							"Value of '%s' field in 'custom' map is not a string", s));
				}
			}
			return map;
		}

		// handles case where the optional itself is null
		private <T> T fromNullable(final Optional<T> nullable) {
			return nullable == null ? null : nullable.orElse(null);
		}
		
		private interface FuncExcept<T, R> {
			
			R apply(T item) throws IllegalParameterException, MissingParameterException;
		}
		
		private GroupUpdateParams toUpdateParams(final GroupID groupID)
				throws IllegalParameterException, MissingParameterException {
			return GroupUpdateParams.getBuilder(groupID)
					.withNullableName(fromNullable(groupName, s -> new GroupName(s)))
					.withNullableType(fromNullable(type, t -> GroupType.fromRepresentation(t)))
					.withOptionalFields(getOptionalFieldsBuilder(false)
							.withDescription(getStringField(description))
							.build())
					.build();
		}
		
		private <T> T fromNullable(
				final Optional<String> nullable,
				final FuncExcept<String, T> getValue)
				throws IllegalParameterException, MissingParameterException {
			if (nullable == null || !nullable.isPresent() || nullable.get().trim().isEmpty()) {
				return null;
			} else {
				return getValue.apply(nullable.get().trim());
			}
		}
		
		private StringField getStringField(final Optional<String> s) {
			if (s == null) {
				return StringField.noAction();
			} else if (!s.isPresent() || s.get().trim().isEmpty()) {
				return StringField.remove();
			} else {
				return StringField.from(s.get());
			}
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
				GroupsStorageException {
		return APICommon.toGroupRequestJSON(groups.getRequestsForGroup(
				getToken(token, true), new GroupID(groupID),
				APICommon.getRequestsParams(excludeUpTo, closed, order, closed == null)));
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

	private Map<String, Object> toGroupJSON(final GroupView g) {
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.GROUP_ID, g.getGroupID().getName());
		ret.put(Fields.GROUP_NAME, g.getGroupName().getName());
		ret.put(Fields.GROUP_OWNER, g.getOwner().getName());
		ret.put(Fields.GROUP_TYPE, g.getType().getRepresentation());
		ret.put(Fields.GROUP_CUSTOM_FIELDS, getCustomFields(g));
		if (g.isStandardView()) {
			ret.put(Fields.GROUP_CREATION, g.getCreationDate().get().toEpochMilli());
			ret.put(Fields.GROUP_MODIFICATION, g.getModificationDate().get().toEpochMilli());
			ret.put(Fields.GROUP_DESCRIPTION, g.getDescription().orElse(null));
			ret.put(Fields.GROUP_MEMBERS, toSortedStringList(g.getMembers(), u -> u.getName()));
			ret.put(Fields.GROUP_ADMINS, toSortedStringList(
					g.getAdministrators(), u -> u.getName()));
			final Map<String, Object> resources = new HashMap<>();
			ret.put(Fields.GROUP_RESOURCES, resources);
			for (final ResourceType t: g.getResourceTypes()) {
				resources.put(t.getName(), getResourceList(g.getResourceInformation(t)));
			}
		}
		return ret;
	}

	private List<Map<String, Object>> getResourceList(final ResourceInformationSet resourceInfo) {
		final List<Map<String, Object>> rlist = new LinkedList<>();
		for (final ResourceDescriptor rd: sorted(resourceInfo)) {
			final Map<String, Object> resource = new HashMap<>();
			rlist.add(resource);
			resource.putAll(resourceInfo.getFields(rd));
			resource.put(Fields.GROUP_RESOURCE_ID, rd.getResourceID().getName());
		}
		return rlist;
	}
	
	private Map<String, String> getCustomFields(final GroupView g) {
		return g.getCustomFields().keySet().stream().collect(Collectors.toMap(
				k -> k.getField(), k -> g.getCustomFields().get(k)));
	}

	// may want to subclass the descriptors to allow for resource type specific sorts
	// specifically for workspaces
	// or associated a comparator with a resource type
	private List<ResourceDescriptor> sorted(final ResourceInformationSet resources) {
		final List<ResourceDescriptor> ret = new ArrayList<>(resources.getResources());
		
		Collections.sort(ret, (r1, r2) -> r1.getResourceID().compareTo(r2.getResourceID()));
		return ret;
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
	@Path(ServicePaths.GROUP_WORKSPACE_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> addWorkspace(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_WS_ID) final String workspaceID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException, RequestExistsException,
				NoSuchResourceException, IllegalResourceIDException, ResourceExistsException,
				ResourceHandlerException {
		//TODO NNOW generalize this method
		return toGroupRequestJSON(groups.addWorkspace(
				getToken(token, true), new GroupID(groupID), new ResourceID(workspaceID)));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_WORKSPACE_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public void removeWorkspace(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_WS_ID) final String workspaceID)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException {
		//TODO NNOW generalize this method
		groups.removeWorkspace(
				getToken(token, true), new GroupID(groupID), new ResourceID(workspaceID));
	}
	
	@POST
	@Path(ServicePaths.GROUP_CATALOG_METHOD_NAME)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> addCatalogMethod(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_CATALOG_METHOD_NAME) final String method)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, RequestExistsException,
				MissingParameterException, IllegalParameterException, GroupsStorageException,
				NoSuchResourceException, IllegalResourceIDException, ResourceExistsException,
				ResourceHandlerException {
		return toGroupRequestJSON(groups.addCatalogMethod(
				getToken(token, true), new GroupID(groupID), new ResourceID(method)));
	}
	
	@DELETE
	@Path(ServicePaths.GROUP_CATALOG_METHOD_NAME)
	@Produces(MediaType.APPLICATION_JSON)
	public void removeCatalogMethod(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.GROUP_ID) final String groupID,
			@PathParam(Fields.GROUP_CATALOG_METHOD_NAME) final String method)
			throws InvalidTokenException, NoSuchGroupException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, MissingParameterException,
				IllegalParameterException, GroupsStorageException, NoSuchResourceException,
				IllegalResourceIDException, ResourceHandlerException {
		groups.removeCatalogMethod(
				getToken(token, true), new GroupID(groupID), new ResourceID(method));
		
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

	private <T> List<String> toSortedStringList(
			final Collection<T> items,
			final Function<T, String> convert) {
		return new TreeSet<>(items).stream().map(n -> convert.apply(n))
				.collect(Collectors.toList());
	}
	
	private void checkIncomingJson(final IncomingJSON json)
			throws IllegalParameterException, MissingParameterException {
		if (json == null) {
			throw new MissingParameterException("Missing JSON body");
		}
		json.exceptOnAdditionalProperties();
	}
}
