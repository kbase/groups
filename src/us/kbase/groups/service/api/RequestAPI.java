package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import us.kbase.groups.core.Groups;
import us.kbase.groups.core.Token;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.MissingParameterException;
import us.kbase.groups.core.exceptions.NoSuchRequestException;
import us.kbase.groups.core.exceptions.UnauthorizedException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.REQUEST)
public class RequestAPI {
	
	// TODO JAVADOC
	// TODO TEST
	
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
				UnauthorizedException, MissingParameterException, GroupsStorageException {
		//TODO NOW make request id class vs. uuid.
		//TODO NOW return list of acceptable actions for request for user.
		return APICommon.toGroupRequestJSON(groups.getRequest(
				new Token(token), UUID.fromString(requestID)));
	}
	
}
