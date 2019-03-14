package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;
import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.service.api.APICommon.toGroupIDs;
import static us.kbase.groups.service.api.APICommon.toGroupJSON;
import static us.kbase.groups.service.api.APICommon.toGroupRequestJSON;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.Groups;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.ClosedRequestException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.IllegalResourceIDException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchGroupException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.NoSuchResourceException;
import us.kbase.groups.core.exceptions.NoSuchResourceTypeException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.core.exceptions.ResourceExistsException;
import us.kbase.groups.core.exceptions.ResourceHandlerException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.exceptions.UserIsMemberException;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.REQUEST)
public class RequestAPI {
	
	// TODO JAVADOC or swagger
	
	private final Groups groups;
	
	// normally instantiated by Jersey
	@Inject
	public RequestAPI(final Groups groups) {
		this.groups = groups;
	}
	
	@GET
	@Path(ServicePaths.REQUEST_ID)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
				UnauthorizedException, MissingParameterException, GroupsStorageException,
				IllegalParameterException, ResourceHandlerException {
		final GroupRequestWithActions actions = groups.getRequest(
							getToken(token, true), new RequestID(requestID));
		final Map<String, Object> json = toGroupRequestJSON(actions.getRequest());
		json.put(Fields.REQUEST_USER_ACTIONS, new TreeSet<>(actions.getActions())
				.stream().map(a -> a.getRepresentation()).collect(Collectors.toList()));
		return json;
	}
	
	@GET
	@Path(ServicePaths.REQUEST_ID_GROUP)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> getGroupForRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws InvalidTokenException, NoSuchRequestException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, ClosedRequestException,
				IllegalParameterException, MissingParameterException, GroupsStorageException,
				ResourceHandlerException {
		return toGroupJSON(groups.getGroupForRequest(
				getToken(token, true), new RequestID(requestID)));
	}
	
	@POST
	@Path(ServicePaths.REQUEST_ID_PERMS)
	public void getPerms(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws NoSuchRequestException, InvalidTokenException, NoTokenProvidedException,
				AuthenticationException, UnauthorizedException, IllegalParameterException,
				MissingParameterException, GroupsStorageException, ClosedRequestException,
				NoSuchResourceException, IllegalResourceIDException, ResourceHandlerException {
		groups.setReadPermission(getToken(token, true), new RequestID(requestID));
	}
	
	@GET
	@Path(ServicePaths.REQUEST_CREATED)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getCreatedRequests(
			@HeaderParam(HEADER_TOKEN) final String token,
			@QueryParam(Fields.GET_REQUESTS_EXCLUDE_UP_TO) final String excludeUpTo,
			@QueryParam(Fields.GET_REQUESTS_INCLUDE_CLOSED) final String closed,
			@QueryParam(Fields.GET_REQUESTS_SORT_ORDER) final String order,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_TYPE) final String resType,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_ID) final String resource)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
			IllegalParameterException, NoSuchResourceTypeException {
		return toGroupRequestJSON(groups.getRequestsForRequester(getToken(token, true),
				APICommon.getRequestsParams(
						excludeUpTo, closed, order, resType, resource, closed == null)));
	}
	
	@GET
	@Path(ServicePaths.REQUEST_TARGETED)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getTargetedRequests(
			@HeaderParam(HEADER_TOKEN) final String token,
			@QueryParam(Fields.GET_REQUESTS_EXCLUDE_UP_TO) final String excludeUpTo,
			@QueryParam(Fields.GET_REQUESTS_INCLUDE_CLOSED) final String closed,
			@QueryParam(Fields.GET_REQUESTS_SORT_ORDER) final String order,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_TYPE) final String resType,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_ID) final String resource)
			throws InvalidTokenException, AuthenticationException, GroupsStorageException,
				IllegalParameterException, ResourceHandlerException, NoSuchResourceTypeException,
				NoSuchResourceException, IllegalResourceIDException, UnauthorizedException {
		return toGroupRequestJSON(groups.getRequestsForTarget(getToken(token, true),
				APICommon.getRequestsParams(
						excludeUpTo, closed, order, resType, resource, closed == null)));
	}
	
	@GET
	@Path(ServicePaths.REQUEST_GROUPS)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getRequestsForAdministratedGroups(
			@HeaderParam(HEADER_TOKEN) final String token,
			@QueryParam(Fields.GET_REQUESTS_EXCLUDE_UP_TO) final String excludeUpTo,
			@QueryParam(Fields.GET_REQUESTS_INCLUDE_CLOSED) final String closed,
			@QueryParam(Fields.GET_REQUESTS_SORT_ORDER) final String order,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_TYPE) final String resType,
			@QueryParam(Fields.GET_REQUESTS_RESOURCE_ID) final String resource)
			throws InvalidTokenException, NoTokenProvidedException, AuthenticationException,
					IllegalParameterException, GroupsStorageException,
					NoSuchResourceTypeException {
		return toGroupRequestJSON(groups.getRequestsForGroups(getToken(token, true),
				APICommon.getRequestsParams(
						excludeUpTo, closed, order, resType, resource, closed == null)));
	}
	
	@PUT
	@Path(ServicePaths.REQUEST_CANCEL)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> cancelRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
				UnauthorizedException, MissingParameterException, GroupsStorageException,
				IllegalParameterException, ClosedRequestException {
		return toGroupRequestJSON(groups.cancelRequest(
				getToken(token, true), new RequestID(requestID)));
	}
	
	@PUT
	@Path(ServicePaths.REQUEST_ACCEPT)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> acceptRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
				UnauthorizedException, MissingParameterException, GroupsStorageException,
				IllegalParameterException, UserIsMemberException, ClosedRequestException,
				NoSuchResourceException, ResourceHandlerException, ResourceExistsException {
		//TODO PRIVATE figure out when user that accepted / denied request should be visible. may need a requestView class
		return toGroupRequestJSON(groups.acceptRequest(
				getToken(token, true), new RequestID(requestID)));
	}

	public static class DenyRequestJSON extends IncomingJSON {
		
		private final String deniedReason;
		
		@JsonCreator
		public DenyRequestJSON(
				@JsonProperty(Fields.REQUEST_DENIED_REASON) final String reason) {
			this.deniedReason = reason;
		}
	}
	
	@PUT
	@Path(ServicePaths.REQUEST_DENY)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> denyRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID,
			final DenyRequestJSON denyJSON)
			throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
				UnauthorizedException, MissingParameterException, GroupsStorageException,
				IllegalParameterException, ClosedRequestException,
				ResourceHandlerException {
		final String reason;
		if (denyJSON == null) {
			reason = null;
		} else {
			denyJSON.exceptOnAdditionalProperties();
			reason = denyJSON.deniedReason;
		}
		//TODO PRIVATE figure out when user that accepted / denied request should be visible. may need a requestView class
		return toGroupRequestJSON(groups.denyRequest(
				getToken(token, true), new RequestID(requestID), reason));
	}
	
	@GET
	@Path(ServicePaths.REQUEST_NEW)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> groupsHaveRequests(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.IDS) final String ids)
			throws IllegalParameterException, InvalidTokenException, NoSuchGroupException,
				NoTokenProvidedException, AuthenticationException, UnauthorizedException,
				GroupsStorageException {
		return groups.groupsHaveRequests(getToken(token, true), toGroupIDs(ids))
				.entrySet().stream().collect(Collectors.toMap(
						e -> e.getKey().getName(),
						e -> ImmutableMap.of(Fields.REQUEST_NEW,
								e.getValue().getRepresentation())));
	}
}
