package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.core.request.GroupRequest;
import us.kbase.groups.core.request.GroupRequestWithActions;
import us.kbase.groups.core.request.RequestID;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.REQUEST)
public class RequestAPI {
	
	// TODO JAVADOC
	// TODO TEST
	// TODO NOW use getToken method for all endpoints
	
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
				IllegalParameterException {
		final GroupRequestWithActions actions = groups.getRequest(
							new Token(token), new RequestID(requestID));
		final Map<String, Object> json = APICommon.toGroupRequestJSON(actions.getRequest());
		json.put(Fields.REQUEST_USER_ACTIONS, new TreeSet<>(actions.getActions())
				.stream().map(a -> a.getRepresentation()).collect(Collectors.toList()));
		return json;
	}
	
	@GET
	@Path(ServicePaths.REQUEST_CREATED)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getCreatedRequests(
			@HeaderParam(HEADER_TOKEN) final String token)
			throws InvalidTokenException, AuthenticationException, MissingParameterException,
				GroupsStorageException {
		//TODO NOW allow getting all vs just open requests
		//TODO NOW sort by created date, up or down
		//TODO NOW allow date ranges and set limit
		return toList(groups.getRequestsForRequester(new Token(token)));
	}
	
	@GET
	@Path(ServicePaths.REQUEST_TARGETED)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getTargetedRequests(
			@HeaderParam(HEADER_TOKEN) final String token)
			throws InvalidTokenException, AuthenticationException, MissingParameterException,
				GroupsStorageException {
		//TODO NOW allow getting all vs just open requests
		//TODO NOW sort by created date, up or down
		//TODO NOW allow date ranges and set limit
		return toList(groups.getRequestsForTarget(new Token(token)));
	}
	
	@PUT
	@Path(ServicePaths.REQUEST_CANCEL)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> cancelRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
			throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
				UnauthorizedException, MissingParameterException, GroupsStorageException,
				IllegalParameterException {
		return APICommon.toGroupRequestJSON(groups.cancelRequest(
				new Token(token), new RequestID(requestID)));
	}
	
	@PUT
	@Path(ServicePaths.REQUEST_ACCEPT)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> acceptRequest(
			@HeaderParam(HEADER_TOKEN) final String token,
			@PathParam(Fields.REQUEST_ID) final String requestID)
					throws InvalidTokenException, NoSuchRequestException, AuthenticationException,
					UnauthorizedException, MissingParameterException, GroupsStorageException,
					IllegalParameterException {
		//TODO PRIVATE figure out when user that accepted / denied request should be visible. may need a requestView class
		return APICommon.toGroupRequestJSON(groups.acceptRequest(
				new Token(token), new RequestID(requestID)));
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
				IllegalParameterException {
		final String reason = denyJSON == null ? null : denyJSON.deniedReason;
		//TODO PRIVATE figure out when user that accepted / denied request should be visible. may need a requestView class
		return APICommon.toGroupRequestJSON(groups.denyRequest(
				new Token(token), new RequestID(requestID), reason));
	}

	private List<Map<String, Object>> toList(final Set<GroupRequest> reqs) {
		return reqs.stream().map(r -> APICommon.toGroupRequestJSON(r))
				.collect(Collectors.toList());
	}
	
}
