package us.kbase.groups.service.api;

import static us.kbase.groups.service.api.APICommon.getToken;
import static us.kbase.groups.service.api.APIConstants.HEADER_TOKEN;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;

import us.kbase.groups.core.Groups;
import us.kbase.groups.core.exceptions.AuthenticationException;
import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.InvalidTokenException;
import us.kbase.groups.core.exceptions.NoTokenProvidedException;
import us.kbase.groups.storage.exceptions.GroupsStorageException;

@Path(ServicePaths.MEMBER)
public class MemberAPI {

	// TODO JAVADOC / swagger
	
	private final Groups groups;
	
	// normally instantiated by Jersey
	@Inject
	public MemberAPI(final Groups groups) {
		this.groups = groups;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, String>> getMemberGroups(
			@HeaderParam(HEADER_TOKEN) final String token)
			throws GroupsStorageException, IllegalParameterException, NoTokenProvidedException,
				InvalidTokenException, AuthenticationException {
		return groups.getMemberGroups(getToken(token, true))
				.stream().map(g -> ImmutableMap.of(
						Fields.GROUP_ID, g.getID().getName(),
						Fields.GROUP_NAME, g.getName().getName()))
				.collect(Collectors.toList());
	}	
}
